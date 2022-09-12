package heylichen.fst.matcher;

import heylichen.fst.output.Output;
import heylichen.fst.output.OutputFactory;
import heylichen.fst.serialize.FstRecordBody;
import heylichen.fst.serialize.FstRecordHeader;
import heylichen.fst.serialize.JumpTableFstReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author lichen
 * @date 2022-9-1 8:42
 */
public class Matcher<O> extends JumpTableFstReader<O> {
  // for suggestion search
  private static final int MIN_EDITS = 2;
  private static final int MAX_EDITS = 6;
  private static final JaroWinklerSimilarity JW = new JaroWinklerSimilarity();

  public Matcher(OutputFactory<O> outputFactory, RandomAccessInput input) throws IOException {
    super(input, outputFactory);
    if (needOutput() && (outputFactory == null || fstHeader.getFlagOutputType() != outputFactory.getOutputType())) {
      throw new IllegalArgumentException("invalid input: outputFactory output type does not match fst header!");
    }
  }

  /**
   * search for key, do process as needed
   *
   * @param string         the searching target
   * @param outputConsumer is a consumer logic for found output
   * @param prefixConsumer a BiConsumer logic for processing keys that have a common prefix with string
   * @return
   * @throws IOException
   */
  public boolean match(String string,
                       Consumer<Output<O>> outputConsumer,
                       BiConsumer<Integer, Output<O>> prefixConsumer
  ) throws IOException {
    boolean matched = false;
    // end address of current transition

    RecordMetaBytes recordMeta = new RecordMetaBytes();
    Offset transitionAddress = new Offset(fstHeader.getStartAddress());
    Output<O> output = newOutput();
    int i = 0;
    //basically each loop correspond to a transition in a state
    while (i < string.length()) {
      char ch = string.charAt(i);
      Output<O> stateOutput;

      long transitionEnd = transitionAddress.get();
      Offset p = new Offset(transitionEnd);

      readRecordHead(p, recordMeta);
      if (recordMeta.isHasJumpTable()) {
        boolean found = lookupInJumpTable(transitionAddress, p, recordMeta, ch);
        if (found) {
          //read arc in the next loop
          continue;
        } else {
          //no arc found in transitions of current state, stuck
          break;
        }
      }
      FstRecordHeader recordHeader = recordMeta.getRecordHeader();
      FstRecordBody<O> tranRecord = readRecord(recordHeader, p);
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
        if (needOutput() && output != null) {
          output.append(tranRecord.getOutput());
        }
        i++;
        if (recordHeader.isFinal()) {
          //we have a prefix
          if (prefixConsumer != null) {
            matched = true;
            if (stateOutput == null || stateOutput.empty()) {
              prefixConsumer.accept(i, output);
            } else {
              prefixConsumer.accept(i, output.appendCopy(stateOutput));
            }
          }
          if (i == string.length()) {
            //we have a match string
            matched = true;
            if (outputConsumer != null) {
              if (stateOutput == null || stateOutput.empty()) {
                outputConsumer.accept(output);
              } else {
                outputConsumer.accept(output.appendCopy(stateOutput));
              }
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
    RecordMetaBytes recordMetaBytes = new RecordMetaBytes();
    while (true) {
      long end = address;
      Offset p = new Offset(end);

      readRecordHead(p, recordMetaBytes);
      if (recordMetaBytes.isHasJumpTable()) {
        // just move to transition record address, pass jumpTable
        readPassJumpTable(p, recordMetaBytes);

        address -= end - p.get();
        continue;
      }

      FstRecordHeader recordHeader = recordMetaBytes.getRecordHeader();
      FstRecordBody<O> transRecord = readRecord(recordHeader, p);
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
      Output<O> output = needOutput() ? context.getPartialOutput().appendCopy(transRecord.getOutput()) : null;

      String prefix = context.getPrefix();
      if (recordHeader.isFinal()) {
        //got a key
        if (!transRecord.isStateOutputEmpty() && output != null) {
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
        //prune search path
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

  public void searchByEditDistance(String key, int maxEdits, BiConsumer<String, Output<O>> biConsumer) {
    if (StringUtils.isEmpty(key)) {
      return;
    }

    RowLevenshteinAutomata la = new RowLevenshteinAutomata(key, maxEdits);
    VisitContext<O> context = noPrefixVisitContext(biConsumer, la);
    depthFirstVisit(context);
  }

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
      O out = o == null ? null : o.getData();
      results.add(new Suggestion<>(k, out));
    });
    return results;
  }
}
