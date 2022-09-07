package heylichen.fst.serialize;

import heylichen.fst.CharTransition;
import heylichen.fst.State;
import heylichen.fst.Transition;
import heylichen.fst.Transitions;
import heylichen.fst.input.InputEntry;
import heylichen.fst.input.InputIterable;
import heylichen.fst.output.OutputType;
import heylichen.fst.serialize.codec.VBCodec;
import lombok.Getter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

@Getter
public class FstSerializeWriter<O> implements FstWriter<O> {
  private final OutputStream os;
  private final boolean needOutput;
  private final boolean needStateOutput;
  private final OutputType outputType;
  private Map<Character, Integer> charIndexMap;

  private Map<Character, Counter> charCountMap;
  private Map<Integer, Counter> biGramCountMap;

  private long address = 0;
  //one state record has one entry. key is state id, value is index in arcAddressTable
  private Map<Long, Integer> stateRecordIndexMap;
  //the arcs of all transitions of all states
  private List<Long> arcAddressTable;

  //if transition count>=8, use jump table for better search performance
  public static final int NEED_JUMP_TABLE_TRANS_COUNT = 8;

  public FstSerializeWriter(OutputStream os,
                            boolean needOutput,
                            boolean needStateOutput,
                            InputIterable<O> input) {
    this.os = os;
    this.needOutput = needOutput;
    this.needStateOutput = needStateOutput;
    initCharIndexTable(input);
    stateRecordIndexMap = new HashMap<>();
    arcAddressTable = new ArrayList<>();
    this.outputType = needOutput ? input.getOutputType() : OutputType.NONE;
  }


  private void initCharIndexTable(InputIterable<O> input) {
    charCountMap = new HashMap<>();
    biGramCountMap = new HashMap<>();

    for (InputEntry<O> en : input.getIterable()) {
      String keyWord = en.getKey();
      char prev = 0;
      for (char ch : keyWord.toCharArray()) {
        charCountMap.computeIfAbsent(ch, k -> new Counter(k, 0)).increment();
        biGramCountMap.computeIfAbsent(genBiGramKey(prev, ch), k -> new Counter(0)).increment();
        prev = ch;
      }
    }
    List<Counter> counters = new ArrayList<>(charCountMap.values());
    counters.sort(Comparator.comparing(Counter::getCount).reversed());

    //reserve 0 for no index
    charIndexMap = new HashMap<>();
    int charIndex = 1;
    for (Counter counter : counters) {
      charIndexMap.put(counter.getCh(), charIndex);
      charIndex++;
    }
  }

  public void close() throws IOException {
    if (arcAddressTable.isEmpty()) {
      return;
    }
    long startByteAddress = arcAddressTable.get(arcAddressTable.size() - 1);
    FstHeader fstHeader = new FstHeader(outputType, needStateOutput, startByteAddress, charIndexMap);
    fstHeader.write(os);
  }

  public void write(State<O> state, char previousArc) throws IOException {
    Transitions<O> transitions = state.getTransitions();
    int transitionCount = transitions == null ? 0 : transitions.size();
    List<Integer> arcIndexes = getArcAccessIndexes(previousArc, transitions);

    StateWriteContext<O> context = new StateWriteContext<>(state, arcIndexes);
    // written transitions are in reversed order
    // first written transition is the last transition
    for (int i = arcIndexes.size() - 1; i >= 0; i--) {
      writeTransitionRecord(i, context);
    }

    if (transitionCount > 0) {
      stateRecordIndexMap.put(state.getId(), arcAddressTable.size() - 1);
    }
  }

  private void writeTransitionRecord(int i, StateWriteContext<O> context) throws IOException {
    CharTransition<O> charTransition = context.getArcTransition(i);
    Transition<O> transition = charTransition.getTransition();

    Integer addressIndex = stateRecordIndexMap.get(transition.getId());
    boolean hasAddress = addressIndex != null;
    boolean lastTransition = i == context.getTransitionCount() - 1;
    //noAddress means this state is adjacent to previous state, no need to write address to output stream
    boolean noAddress = lastTransition && hasAddress && addressIndex == arcAddressTable.size() - 1;
    boolean generateJumpTable = i == 0 && context.needJumpTable();

    FstRecord<O> record = new FstRecord<>(needOutput, needStateOutput);
    RecordHeader recHeader = record.getHeader();
    recHeader.setNoAddress(noAddress);
    recHeader.setLastTransition(lastTransition);
    recHeader.setFinal(transition.isToFinal());

    record.setDelta(0);
    long nextAddress = 0;//for dump
    if (!noAddress && hasAddress) {
      long delta = address - arcAddressTable.get(addressIndex);
      record.setDelta((int) delta);
      nextAddress = address - delta;
    }

    if (needOutput) {
      boolean hasOutput = transition.getOutput() != null && !transition.getOutput().empty();
      recHeader.setHasOutput(hasOutput);
      if (hasOutput) {
        record.setOutput(transition.getOutput());
      }

      if (needStateOutput) {
        boolean hasStateOutput = transition.getStateOutput() != null && !transition.getStateOutput().empty();
        recHeader.setHasStateOutput(hasStateOutput);
        if (hasStateOutput) {
          record.setStateOutput(transition.getStateOutput());
        }
      }
    }

    int labelIndex = 0;
    char arc = charTransition.getCh();
    int index = charIndexMap.get(arc);
    if (index < recHeader.getCharIndexSize()) {
      labelIndex = index;
    } else {
      record.setLabel(arc);
    }
    recHeader.setLabelIndex(labelIndex);

    // During reading the serialized state, we read from high address to low address, backward.
    // If record header flag tag happens to be the same as jump table tag (0xFF or 0xFE),
    // when we see a tag as 0xFF or 0xFE, it can be a record header flag OR jump table tag.
    // We can't distinguish them. So change the header byte to make a difference.
    if (recHeader.hasJumpTable()) {
      record.setLabel(arc);
      recHeader.setLabelIndex(0);
    }

    int byteSize = record.getByteSize();
    long accessibleAddress = address + byteSize - 1;
    arcAddressTable.add(accessibleAddress);
    address += byteSize;

    record.write(os);
    context.setJumpTableAt(i, accessibleAddress);

    if (generateJumpTable) {
      int jumpTableByteSize = writeJumpTable(context.jumpTable, accessibleAddress, recHeader);
      address += jumpTableByteSize;
      addAddressTable(arcAddressTable.size() - 1, jumpTableByteSize);
    }
  }

  /**
   * absorb info in write method, make writeTransitionRecord parameters less
   *
   * @param <O>
   */
  private static class StateWriteContext<O> {
    private final Transitions<O> transitions;
    @Getter
    private final int transitionCount;
    private final long[] jumpTable;
    // element is index in transitions
    private final List<Integer> arcIndexes;

    public StateWriteContext(State<O> state, List<Integer> arcIndexes) {
      transitions = state.getTransitions();
      transitionCount = transitions == null ? 0 : transitions.size();
      jumpTable = needJumpTable() ? new long[transitionCount] : null;
      this.arcIndexes = arcIndexes;
    }

    public CharTransition<O> getArcTransition(int i) {
      int arcIndex = arcIndexes.get(i);
      return transitions.get(arcIndex);
    }

    public void setJumpTableAt(int i, long v) {
      if (jumpTable != null) {
        jumpTable[i] = v;
      }
    }

    public boolean needJumpTable() {
      return transitionCount >= NEED_JUMP_TABLE_TRANS_COUNT;
    }
  }

  /**
   * write jump table, has 3 parts:
   * 1) one byte jumpTableTag
   * 2) length
   * 3) elements
   *
   * @param jumpTable
   * @param accessibleAddress
   * @param recHeader
   * @return
   * @throws IOException
   */
  private int writeJumpTable(long[] jumpTable, long accessibleAddress, RecordHeader recHeader) throws IOException {
    int jumpTableElementSize = calculateJumpTableElementSize(accessibleAddress, jumpTable);

    int jumpTableByteSize = 1 + VBCodec.getEncodedBytes(jumpTable.length) + jumpTable.length * jumpTableElementSize;
    boolean needTwoBytes = jumpTableByteSize == 2;
    byte jumpTableTag = recHeader.getJumpTableTag(needTwoBytes);

    writeJumpTableElements(jumpTableElementSize, jumpTable);
    VBCodec.encodeReverse(jumpTable.length, os);
    os.write(jumpTableTag);

    return jumpTableByteSize;
  }

  private void writeJumpTableElements(int jumpTableElementSize, long[] jumpTable) throws IOException {
    int byteCount = jumpTableElementSize * jumpTable.length;
    boolean twoBytes = jumpTableElementSize == 2;
    byte[] bytes = new byte[byteCount];
    int byteOffset = 0;
    for (long l : jumpTable) {
      bytes[byteOffset++] = (byte) (l & 0xFF);
      if (twoBytes) {
        bytes[byteOffset++] = (byte) (l >>> 8 & 0xFF);
      }
    }
    os.write(bytes);
  }

  private void addAddressTable(int i, long delta) {
    long old = arcAddressTable.get(i);
    arcAddressTable.set(i, old + delta);
  }

  private int calculateJumpTableElementSize(long accessibleAddress, long[] jumpTable) {
    int eleSize = 1;
    for (int i = 0; i < jumpTable.length; i++) {
      jumpTable[i] = accessibleAddress - jumpTable[i];
      if (jumpTable[i] > 0xFF) {
        eleSize = 2;
      }
    }
    return eleSize;
  }

  private List<Integer> getArcAccessIndexes(char prevChar, Transitions<O> transitions) {
    int transitionCount = transitions == null ? 0 : transitions.size();
    boolean needJumpTable = transitionCount >= 8;
    //values in arcIndexes are indexes in transitions
    List<Integer> arcIndexes = genArcIndexes(transitionCount);
    if (needJumpTable) {
      return arcIndexes;
    }

    List<Long> biGramKeyCount = new ArrayList<>(transitionCount);
    transitions.foreach((Character ch, Transition<O> t) -> {
      long count = biGramCountMap.get(genBiGramKey(prevChar, ch)).getCount();
      biGramKeyCount.add(count);
    });
    arcIndexes.sort(Comparator.comparing((Integer k) -> biGramKeyCount.get(k)).reversed());
    return arcIndexes;
  }

  private void reverse(List<Integer> indexes) {
    if (indexes == null || indexes.isEmpty()) {
      return;
    }
    int len = indexes.size() / 2;
    int last = indexes.size() - 1;
    for (int i = 0; i < len; i++) {
      swap(indexes, i, last - i);
    }
  }

  private <T> void swap(List<T> list, int a, int b) {
    T tmp = list.get(a);
    list.set(a, list.get(b));
    list.set(b, tmp);
  }

  private List<Integer> genArcIndexes(int transitionCount) {
    List<Integer> arcIndexes = new ArrayList<>(transitionCount);
    for (int i = 0; i < transitionCount; i++) {
      arcIndexes.add(i);
    }
    return arcIndexes;
  }

  private Integer genBiGramKey(char prev, char cur) {
    return (prev << 16) | cur;
  }

  @Getter
  private static class Counter {
    private char ch;
    private long count;

    public Counter(char ch, long count) {
      this.ch = ch;
      this.count = count;
    }

    public Counter(long count) {
      this.count = count;
    }

    public void increment() {
      count++;
    }
  }
}
