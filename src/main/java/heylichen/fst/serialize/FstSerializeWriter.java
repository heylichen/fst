package heylichen.fst.serialize;

import heylichen.fst.CharTransition;
import heylichen.fst.State;
import heylichen.fst.Transition;
import heylichen.fst.Transitions;
import heylichen.fst.input.InputEntry;
import heylichen.fst.input.InputIterable;
import heylichen.fst.output.OutputType;
import lombok.Getter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Getter
public class FstSerializeWriter<O> implements FstWriter<O> {
  private final OutputStream os;
  private final boolean needOutput;
  private final boolean needStateOutput;
  private final OutputType outputType;
  //dump serialized layout
  private final boolean dump;
  private final DumpTableStringBuilder dumpBuilder;
  private final List<String> dumpJumpTables;
  private Map<Character, Integer> charIndexMap;

  private Map<Character, Counter> charCountMap;
  private Map<Integer, Counter> biGramCountMap;

  private long address = 0;
  //one state record has one entry. key is state id, value is index in arcAddressTable
  private final Map<Long, Integer> stateRecordIndexMap;
  //the arcs of all transitions of all states
  private final List<Long> arcAddressTable;

  //if transition count>=8, use jump table for better search performance
  public static final int NEED_JUMP_TABLE_TRANS_COUNT = 8;
  public static final char NEW_LINE = '\n';
  public static final char TAB = '\t';
  public static final char SPACE = ' ';

  public FstSerializeWriter(OutputStream os,
                            boolean needOutput,
                            boolean needStateOutput,
                            InputIterable<O> input) throws IOException {
    this(os, needOutput, needStateOutput, input, false);
  }

  public FstSerializeWriter(OutputStream os,
                            boolean needOutput,
                            boolean needStateOutput,
                            InputIterable<O> input,
                            boolean dump) throws IOException {
    this.os = os;
    this.needOutput = needOutput;
    this.needStateOutput = needStateOutput;
    initCharIndexTable(input);
    stateRecordIndexMap = new HashMap<>();
    arcAddressTable = new ArrayList<>();
    this.outputType = needOutput ? input.getOutputType() : OutputType.NONE;
    this.dump = dump;
    if (dump) {
      dumpBuilder = new DumpTableStringBuilder(needOutput);
      os.write(dumpBuilder.getTableHeader().getBytes(StandardCharsets.UTF_8));
      dumpJumpTables = new ArrayList<>();
    } else {
      dumpBuilder = null;
      dumpJumpTables = null;
    }
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
    if (dump) {
      dumpJumpTable();
      return;
    }
    long startByteAddress = arcAddressTable.get(arcAddressTable.size() - 1);
    FstHeader fstHeader = new FstHeader(outputType, needStateOutput, startByteAddress, charIndexMap);
    fstHeader.write(os);
  }

  private void dumpJumpTable() throws IOException {
    if (dumpJumpTables == null || dumpJumpTables.isEmpty()) {
      return;
    }
    os.write("\n".getBytes(StandardCharsets.UTF_8));
    for (String dumpJumpTable : dumpJumpTables) {
      os.write(dumpJumpTable.getBytes(StandardCharsets.UTF_8));
    }
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

    FstRecordWriter<O> recordWriter = new FstRecordWriter<>(needOutput, needStateOutput);
    recordWriter.setNFL(noAddress, transition.isToFinal(), lastTransition);

    int delta = 0;
    long nextAddress = 0; //for dump
    if (!noAddress && hasAddress) {
      delta = (int) (address - arcAddressTable.get(addressIndex));
      nextAddress = address - delta;
    }
    recordWriter.setDelta(delta);

    if (needOutput) {
      boolean hasOutput = transition.getOutput() != null && !transition.getOutput().empty();
      if (hasOutput) {
        recordWriter.setOutput(transition.getOutput());
      }

      if (needStateOutput) {
        boolean hasStateOutput = transition.getStateOutput() != null && !transition.getStateOutput().empty();
        if (hasStateOutput) {
          recordWriter.setStateOutput(transition.getStateOutput());
        }
      }
    }

    char arc = charTransition.getCh();
    int labelIndex = charIndexMap.get(arc);
    labelIndex = recordWriter.setLabel(arc, labelIndex);

    int byteSize = recordWriter.getByteSize();
    long accessibleAddress = address + byteSize - 1;
    arcAddressTable.add(accessibleAddress);
    address += byteSize;

    if (!dump) {
      recordWriter.write(os);
    }
    context.setJumpTableAt(i, accessibleAddress);
    int jumpTableByteSize = 0;
    if (generateJumpTable) {
      JumpTableWriter jumpTableWriter = new JumpTableWriter(context.jumpTable, accessibleAddress);
      jumpTableByteSize = jumpTableWriter.calculateTotalSize();
      if (!dump) {
        jumpTableWriter.write(os);
      } else {
        dumpJumpTables.add(jumpTableWriter.dump(context.stateId));
      }
      address += jumpTableByteSize;
      addAddressTable(arcAddressTable.size() - 1, jumpTableByteSize);
    }

    if (dump) {
      Long addr = arcAddressTable.get(arcAddressTable.size() - 1);
      dumpBuilder.appendStateId(context.stateId, transition.getId());
      dumpBuilder.appendAddress(addr);
      dumpBuilder.appendArc(arc, labelIndex);
      dumpBuilder.appendNFL(noAddress, transition.isToFinal(), lastTransition);
      dumpBuilder.appendNextAddr(!noAddress, nextAddress);
      dumpBuilder.appendOut(transition.getOutput(), transition.getStateOutput());
      dumpBuilder.appendByteSize(byteSize + jumpTableByteSize, jumpTableByteSize);

      os.write(dumpBuilder.buildTransitionRecord().getBytes(StandardCharsets.UTF_8));
    }
  }

  /**
   * absorb info in write method, make writeTransitionRecord parameters less
   *
   * @param <O>
   */
  private static class StateWriteContext<O> {
    private final long stateId;
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
      this.stateId = state.getId();
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

  private void addAddressTable(int i, long delta) {
    long old = arcAddressTable.get(i);
    arcAddressTable.set(i, old + delta);
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
