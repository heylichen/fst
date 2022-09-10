package heylichen.fst;

import heylichen.fst.input.InputEntry;
import heylichen.fst.input.InputIterable;
import heylichen.fst.output.Output;
import heylichen.fst.serialize.FstDotWriter;
import heylichen.fst.serialize.FstSerializeWriter;
import heylichen.fst.serialize.FstWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * for build fst
 *
 * @param <O>
 * @author heylichen@qq.com
 */
public class FstBuilder<O> {
  private FstWriter<O> fstWriter;
  private List<State<O>> tempStates;

  private String currentWord;
  private Output<O> currentOutput;
  private String previousWord;

  private boolean needOutput;

  public void dump(InputIterable<O> input,
                      OutputStream os,
                      boolean output,
                      boolean needStateOutput) throws IOException {
    this.needOutput = output;
    fstWriter = new FstSerializeWriter<>(os, output, needStateOutput, input, true);
    buildFst(input);
  }

  public void compile(InputIterable<O> input,
                      OutputStream os,
                      boolean output,
                      boolean needStateOutput) throws IOException {
    this.needOutput = output;
    fstWriter = new FstSerializeWriter<>(os, output, needStateOutput, input);
    buildFst(input);
  }

  public void compileDot(InputIterable<O> input, OutputStream os, boolean output) throws IOException {
    this.needOutput = output;
    fstWriter = new FstDotWriter<>(os);
    buildFst(input);
  }

  private void buildFst(InputIterable<O> input) throws IOException {
    Dictionary<O> dictionary = new Dictionary<>();
    int nextStateId = 0;
    int errorInputIndex = 0;
    int entryIndex = 0;

    tempStates = new ArrayList<>();
    previousWord = null;
    tempStates.add(new State<>(nextStateId++));

    for (InputEntry<O> inputEntry : input.getIterable()) {
      currentWord = inputEntry.getKey();
      currentOutput = inputEntry.getValue();
      if (StringUtils.isBlank(currentWord)) {
        throw new IllegalArgumentException("empty key is not allowed!");
      }

      int prefixLength = getCommonPrefixLength(previousWord, currentWord);
      boolean inOrder = inOrder(previousWord, currentWord);
      if (!inOrder) {
        throw new IllegalArgumentException("keys must be sorted! prev=" + previousWord + ", cur=" + currentWord);
      }

      if (StringUtils.equals(previousWord, currentWord)) {
        throw new IllegalArgumentException("duplicate keys not allowed! duplicate key=" + currentWord);
      }

      // We minimize the states from the suffix of the previous word
      for (int i = StringUtils.length(previousWord); i > prefixLength; i--) {
        Pair<Boolean, State<O>> findPair = findMinimized(tempStates.get(i), dictionary);
        char arc = previousWord.charAt(i - 1);
        State<O> state = findPair.getRight();
        if (findPair.getLeft()) {
          // 找到了，state为找到的，i-1->state
          nextStateId--;
        } else {
          //没找到，i-1->i 不变
          // Ownership of the object in temp_states[i] has been moved to the
          // dictionary...
          fstWriter.write(state, arc);
          tempStates.set(i, new State<>(-1));
        }
        tempStates.get(i - 1).setTransition(arc, state);
      }

      // This loop initializes the tail states for the current word
      for (int i = prefixLength + 1; i <= currentWord.length(); i++) {
        assert i <= tempStates.size();
        //当前字符要用的state id = next_state_id, temp_states[i]是最新的一个state
        if (i == tempStates.size()) {
          tempStates.add(new State<>(nextStateId++));
        } else {
          tempStates.get(i).reuse(nextStateId++);
        }

        char arc = currentWord.charAt(i - 1);
        tempStates.get(i - 1).setTransition(arc, tempStates.get(i));
      }

      if (!StringUtils.equals(currentWord, previousWord)) {
        State<O> state = tempStates.get(currentWord.length());
        state.setFinalState(true);
      }

      writeOutput(prefixLength);
      previousWord = currentWord;
    }

    // Here we are minimizing the states of the last word
    for (int i = previousWord.length(); i >= 0; i--) {
      Pair<Boolean, State<O>> findPair = findMinimized(tempStates.get(i), dictionary);
      State<O> gotState = findPair.getRight();
      char arc = i > 0 ? previousWord.charAt(i - 1) : 0;
      if (findPair.getLeft()) {
        nextStateId--;
      } else {
        fstWriter.write(gotState, arc);
      }

      if (i > 0) {
        tempStates.get(i - 1).setTransition(arc, gotState);
      }
    }

    fstWriter.close();
  }

  private void writeOutput(int prefixLength) {
    if (!needOutput) {
      return;
    }
    for (int i = 1; i <= prefixLength; i++) {
      State<O> previousState = tempStates.get(i - 1);
      char arc = currentWord.charAt(i - 1);

      Output<O> prevOut = previousState.getOutput(arc);
      Output<O> commonPrefix = currentOutput.getCommonPrefix(prevOut);
      Output<O> wordSuffix = prevOut == null ? null : prevOut.getSuffix(commonPrefix);

      previousState.setOutput(arc, commonPrefix);

      if (wordSuffix != null && !wordSuffix.empty()) {
        State<O> state = tempStates.get(i);
        state.getTransitions().foreach((Character ch, Transition<O> t) -> {
          state.prependSuffixToOutput(ch, wordSuffix);
        });

        if (state.isFinalState()) {
          state.prependSuffixToStateOutput(wordSuffix);
        }
      }

      currentOutput = currentOutput.getSuffix(commonPrefix);
    }

    if (StringUtils.equals(currentWord, previousWord)) {
      State<O> stateTmp = tempStates.get(currentWord.length());
      stateTmp.setStateOutput(currentOutput);
    } else {
      State<O> stateTmp = tempStates.get(prefixLength);
      char arc = currentWord.charAt(prefixLength);
      stateTmp.setOutput(arc, currentOutput);
    }
  }


  private Pair<Boolean, State<O>> findMinimized(State<O> state, Dictionary<O> dictionary) {
    BigInteger key = state.hash();
    State<O> got = dictionary.get(key, state);
    if (got != null) {
      return Pair.of(true, got);
    }
    dictionary.put(key, state);
    return Pair.of(false, state);
  }

  private static int getCommonPrefixLength(String a, String b) {
    if (StringUtils.isEmpty(a) || StringUtils.isEmpty(b)) {
      return 0;
    }
    int l = 0;
    int aLen = a.length();
    int bLen = b.length();
    while (l < aLen && l < bLen) {
      if (a.charAt(l) != b.charAt(l)) {
        break;
      }
      l++;
    }
    return l;
  }

  /**
   * required keys are in non-descending order
   *
   * @param a
   * @param b
   * @return
   */
  private static boolean inOrder(String a, String b) {
    if (StringUtils.isEmpty(a)) {
      return true;
    }
    if (StringUtils.isEmpty(b)) {
      return false;
    }
    int l = 0;
    while (l < a.length() && l < b.length()) {
      char cha = a.charAt(l);
      char chb = b.charAt(l);
      if (cha > chb) {
        return false;
      } else if (cha < chb) {
        break;
      } else {
        l++;
      }
    }
    return true;
  }
}
