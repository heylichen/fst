package heylichen.fst.matcher;

import heylichen.fst.output.Output;
import heylichen.fst.output.OutputFactory;
import heylichen.fst.serialize.FstHeader;
import heylichen.fst.serialize.FstRecord;
import heylichen.fst.serialize.RecordHeader;
import heylichen.fst.serialize.codec.LenInt;
import heylichen.fst.serialize.codec.VBCodec;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author lichen
 * @date 2022-9-1 8:42
 */
public class Matcher<O> {
  @Getter
  private final OutputFactory<O> outputFactory;
  private final RandomAccessInput input;
  @Getter
  private final FstHeader fstHeader;
  private final boolean needOutput;
  private final boolean needStateOutput;
  @Getter
  private final boolean valid;

  public Matcher(OutputFactory<O> outputFactory, RandomAccessInput input) throws IOException {
    this.outputFactory = outputFactory;
    this.input = input;

    fstHeader = new FstHeader();
    boolean readHeadSuccess = fstHeader.read(input, input.getSize());
    needOutput = fstHeader.isNeedOutput();
    needStateOutput = fstHeader.isNeedStateOutput();

    if (!readHeadSuccess) {
      valid = false;
      return;
    }
    if (fstHeader.getFlagOutputType() != outputFactory.getOutputType()) {
      valid = false;
      return;
    }

    valid = true;
  }

  /**
   * search for key, do process as needed
   *
   * @param string         the searching target
   * @param outputs        is a consumer logic for found output
   * @param prefixConsumer a BiConsumer logic for processing keys that have a common prefix with string
   * @return
   * @throws IOException
   */
  public boolean match(String string,
                       Consumer<Output<O>> outputs,
                       BiConsumer<Integer, Output<O>> prefixConsumer
  ) throws IOException {
    boolean matched = false;
    // end address of current transition

    Offset transitionAddress = new Offset(fstHeader.getStartAddress());
    Output<O> output = outputFactory.newInstance();
    int i = 0;
    //basically each loop correspond to a transition in a state
    while (i < string.length()) {
      char ch = string.charAt(i);
      Output<O> stateOutput;

      long transitionEnd = transitionAddress.get();
      Offset p = new Offset(transitionEnd);

      byte recordHeaderByte = input.readByte(p.getAndAdd(-1));
      RecordHeader recordHeader = RecordHeader.newInstance(needOutput, needStateOutput, recordHeaderByte);

      if (recordHeader.hasJumpTable()) {
        boolean found = lookupInJumpTable(transitionAddress, p, recordHeader.getJumpTableElementSize(), ch);
        if (found) {
          //read arc in the next loop
          continue;
        } else {
          //no arc found in transitions of current state, stuck
          break;
        }
      }

      FstRecord<O> tranRecord = readRecord(recordHeader, p);
      // delta is position diff between current transition and next state
      // if current state is last state, delta is 0
      int deltaToNextState = tranRecord.getDelta();
      stateOutput = tranRecord.getStateOutput();

      long currentTransitionBytes = transitionEnd - p.get();
      long nextAddress = 0;
      if (!recordHeader.isNoAddress()) {
        // if current state is last state, delta is 0, nextAddress is 0
        if (deltaToNextState > 0) {
          nextAddress = transitionAddress.get() - currentTransitionBytes - deltaToNextState + 1;
        }
      } else {
        nextAddress = transitionAddress.get() - currentTransitionBytes;
      }

      if (ch == tranRecord.getLabel()) {
        // if found char in transitions of current state, will go to next state
        output.append(tranRecord.getOutput());
        i++;
        if (recordHeader.isFinal()) {
          //we have a prefix
          if (prefixConsumer != null) {
            if (stateOutput == null || stateOutput.empty()) {
              prefixConsumer.accept(i, output);
            } else {
              prefixConsumer.accept(i, output.appendCopy(stateOutput));
            }
            matched = true;
          }
          if (i == string.length()) {
            //we have a match string
            if (outputs != null) {
              if (stateOutput == null || stateOutput.empty()) {
                outputs.accept(output);
              } else {
                outputs.accept(output.appendCopy(stateOutput));
              }
              matched = true;
              break;
            }
          }
        }
        if (nextAddress <= 0) {
          break;
        }
        transitionAddress.setOffset(nextAddress);
      } else {
        // still in current state, current transition arc not match. try next if possible
        if (recordHeader.isLastTransition()) {
          break;
        }
        transitionAddress.subtract(currentTransitionBytes);
      }
    }
    return matched;
  }

  public void depthFirstVisit(VisitContext<O> context) {
    depthFirstVisit(fstHeader.getStartAddress(), context);
  }


  private void depthFirstVisit(long address,
                               VisitContext<O> context) {
    while (true) {
      long end = address;
      Offset p = new Offset(end);

      byte recordHeaderByte = input.readByte(p.getAndAdd(-1));
      RecordHeader recordHeader = RecordHeader.newInstance(needOutput, needStateOutput, recordHeaderByte);

      if (recordHeader.hasJumpTable()) {
        // just move to transition record address, pass jumpTable
        int jumpTableEleSize = recordHeader.getJumpTableElementSize();
        LenInt lenInt = VBCodec.decodeReverse(input, p.get());

        int eleCount = lenInt.getValue();
        p.subtract(lenInt.getLen());
        p.subtract((long) eleCount * jumpTableEleSize);

        address -= end - p.get();
        continue;
      }

      FstRecord<O> transRecord = readRecord(recordHeader, p);
      long byteSize = end - p.get();
      long nextAddress = 0;
      if (!recordHeader.isNoAddress()) {
        if (transRecord.getDelta() > 0) {
          nextAddress = address - byteSize - transRecord.getDelta() + 1;
        }
      } else {
        nextAddress = address - byteSize;
      }
      //copy to try this arc. may try others arcs in this state, so keep original context untouched.
      Automaton atm = context.copyAutomaton();
      char arc = transRecord.getLabel();
      atm.step(arc);

      String word = context.getPartialKey() + arc;
      Output<O> output = context.getPartialOutput().appendCopy(transRecord.getOutput());

      String prefix = context.getPrefix();
      if (recordHeader.isFinal()) {
        //got a key
        if (!transRecord.isStateOutputEmpty()) {
          output.appendCopy(transRecord.getStateOutput());
        }
        if (atm.isMatch()) {
          //for predictive search, if prefix is empty, found key longer than prefix
          if (StringUtils.isEmpty(prefix) ||
              // in this case, found key equals prefix
              (prefix.length() == 1 && prefix.charAt(0) == arc)) {
            context.accept(word, output);
          }
        }
      }

      if (!atm.canMatch()) {
        break;
      }

      if (nextAddress > 0) {
        if (StringUtils.isEmpty(prefix) || prefix.charAt(0) == arc) {
          VisitContext<O> subContext = context.copy();
          subContext.setPartialKey(word);
          subContext.setPartialOutput(output);
          subContext.setPrefix(StringUtils.isEmpty(prefix) ? prefix : prefix.substring(1));
          // need to remember the state stepped so far in Automaton
          subContext.setAutomaton(atm);
          depthFirstVisit(nextAddress, subContext);
        }
      }

      if (recordHeader.isLastTransition()) {
        break;
      } else {
        //try next transition
        address -= byteSize;
      }
    }
  }


  /**
   * read current transition record format. left to right, from low address to high address.
   * field in [] is optional.
   * [stateOutput] | [output] | [delta] | [arc label] | recordHeaderByte
   *
   * @param recordHeader
   * @param p
   * @return
   */
  private FstRecord<O> readRecord(RecordHeader recordHeader, Offset p) {
    // read current transition record format. left to right, from low address to high address.
    // field in [] is optional.
    // [stateOutput] | [output] | [delta] | [arc label] | recordHeaderByte
    char arc = readArc(recordHeader, p);
    // delta is position diff between current transition and next state
    // if current state is last state, delta is 0
    int deltaToNextState = 0;
    if (!recordHeader.isNoAddress()) {
      LenInt lenInt = VBCodec.decodeReverse(input, p.get());
      deltaToNextState = lenInt.getValue();
      p.subtract(lenInt.getLen());
    }

    Output<O> outputSuffix = outputFactory.newInstance();
    if (recordHeader.hasOutput()) {
      Pair<Output<O>, Integer> pair = outputFactory.readByteValue(input, p.get());
      outputSuffix = pair.getKey();
      p.subtract(pair.getValue());
    }

    Output<O> stateOutput = null;
    if (recordHeader.hasStateOutput()) {
      Pair<Output<O>, Integer> pair = outputFactory.readByteValue(input, p.get());
      stateOutput = pair.getKey();
      p.subtract(pair.getValue());
    }

    return new FstRecord<>(arc, deltaToNextState, outputSuffix, stateOutput);
  }

  private boolean lookupInJumpTable(Offset transitionAddress, Offset p, int jumpTableEleSize, char ch) {
    LenInt lenInt = VBCodec.decodeReverse(input, p.get());

    int eleCount = lenInt.getValue();
    p.subtract(lenInt.getLen());
    p.subtract((long) eleCount * jumpTableEleSize);

    long jumpTableStart = p.get() + 1;
    int jumpTableByteSize = 1 + lenInt.getLen() + eleCount * jumpTableEleSize;
    long arcsBaseAddress = transitionAddress.get() - jumpTableByteSize;
    Function<Long, Character> getArcFunction = (Long index) -> {
      long off = arcsBaseAddress - lookupJumpTable(jumpTableStart, index.intValue(), jumpTableEleSize);
      Offset offP = new Offset(off);
      byte flag = input.readByte(offP.getAndAdd(-1));
      RecordHeader rh = RecordHeader.newInstance(needOutput, needStateOutput, flag);
      return readArc(rh, offP);
    };

    long foundIndex = binarySearchIndexForChar(0, eleCount,
        (Long index) -> getArcFunction.apply(index) < ch);

    if (foundIndex < eleCount && getArcFunction.apply(foundIndex) == ch) {
      long offset = lookupJumpTable(jumpTableStart, (int) foundIndex, jumpTableEleSize);
      transitionAddress.subtract((offset + jumpTableByteSize));
    } else {
      //no arc found in transitions of current state
      return false;
    }
    return true;
  }

  private int lookupJumpTable(long jumpTableStart, int i, int elementSize) {
    int value = input.readByte(jumpTableStart + i * elementSize) & 0xFF;
    if (elementSize == 2) {
      value += (input.readByte(jumpTableStart + i * elementSize + 1) & 0xFF) << 8;
    }
    return value;
  }

  private char readArc(RecordHeader recordHeader, Offset p) {
    int index = recordHeader.getLabelIndex();
    if (index > 0) {
      return fstHeader.getChar(index);
    } else {
      byte[] charBytes = AlphabetUtil.readUTF8Bytes(input, p.get());
      p.subtract(charBytes.length);
      // only consider BMP in Unicode.
      // supplementary characters are represented as a pair of char values in Java.
      return (new String(charBytes, StandardCharsets.UTF_8)).charAt(0);
    }
  }

  /**
   * char is in less function
   *
   * @param first
   * @param last
   * @param less
   * @return
   */
  private long binarySearchIndexForChar(long first, long last, Function<Long, Boolean> less) {
    long len = last - first;

    while (len > 0) {
      long half = len >> 1;
      long middle = first + half;
      if (less.apply(middle)) {
        first = middle;
        first++;
        len = len - half - 1;
      } else {
        len = half;
      }
    }
    return first;
  }

  public void searchByEditDistance(String key, int maxEdits, BiConsumer<String, Output<O>> biConsumer) {
    if (StringUtils.isEmpty(key)) {
      return;
    }

    RowLevenshteinAutomata la = new RowLevenshteinAutomata(key, maxEdits);
    Set<String> keys = new HashSet<>();
    VisitContext<O> context = noPrefixVisitContext(biConsumer, la);
    depthFirstVisit(context);
  }

  public static final int MIN_EDITS = 2;
  public static final int MAX_EDITS = 6;
  public static final JaroWinklerSimilarity JW = new JaroWinklerSimilarity();

  public List<Suggestion<O>> suggestSearch(String key) {
    List<Suggestion<O>> list = Collections.emptyList();
    for (int i = MIN_EDITS; i < MAX_EDITS; i++) {
      List<Suggestion<O>> results = searchByEditDistance(key, i);
      if (results.size() == 1) {
        list = results;
        break;
      } else if (results.size() > 1) {
        score(key, results);
        list = results;
        break;
      }
    }
    return list;
  }

  private void score(String key, List<Suggestion<O>> results) {
    for (Suggestion<O> result : results) {
      String word = result.getKey();
      if (word.equals(key)) {
        result.setScore(0);
        continue;
      }
      double simL = getLevenshteinDistanceSimilarity(key, word);
      double simJ = JW.getJaroWinklerSim(key, word);
      double similarity = simL * simJ;
      result.setScore(similarity);
    }
    results.sort(Comparator.comparing(Suggestion<O>::getScore).reversed());
  }

  private double getLevenshteinDistanceSimilarity(String a, String b) {
    double ld = RowLevenshteinAutomata.calculateEditDistance(a, b);
    return 1.0 - (ld / Math.max(StringUtils.length(a), StringUtils.length(b)));
  }

  private List<Suggestion<O>> searchByEditDistance(String key, int maxEdits) {
    List<Suggestion<O>> results = new ArrayList<>();
    searchByEditDistance(key, maxEdits, (k, o) -> {
      results.add(new Suggestion<>(k, o.getData()));
    });
    return results;
  }

  public VisitContext<O> prefixVisitContext(String prefix, BiConsumer<String, Output<O>> accept, Automaton automaton) {
    return contextBuilder().withPrefix("").withAutomaton(automaton).withConsumer(accept)
        .withPrefix(prefix).build();
  }

  public VisitContext<O> noPrefixVisitContext(BiConsumer<String, Output<O>> accept, Automaton automaton) {
    return contextBuilder().withPrefix("").withAutomaton(automaton).withConsumer(accept).build();
  }

  private VisitContext.VisitContextBuilder<O> contextBuilder() {
    VisitContext.VisitContextBuilder<O> builder = VisitContext.builder();
    builder.withPartialOutput(outputFactory.newInstance())
        .withPartialKey("");
    return builder;
  }
}
