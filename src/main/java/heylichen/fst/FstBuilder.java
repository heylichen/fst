package heylichen.fst;

import heylichen.fst.output.Output;
import heylichen.fst.serialize.FstWriter;
import heylichen.fst.serialize.InputEntry;
import heylichen.fst.serialize.RandomAccessInput;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class FstBuilder<O> {
  private FstWriter<O> fstWriter;
  private List<State<O>> tempStates;

  private String currentWord;
  private Output<O> currentOutput;
  private String previousWord;

  public FstBuildResult compile(RandomAccessInput<O> input, OutputStream os, boolean output) throws IOException {
    fstWriter = new FstWriter<>(os, output, true, input);
    return buildFst(input);
  }

  private FstBuildResult buildFst(RandomAccessInput<O> input) throws IOException {
    Dictionary<O> dictionary = new Dictionary<>();
    int nextStateId = 0;
    int errorInputIndex = 0;
    int entryIndex = 0;

    tempStates = new ArrayList<>();
    previousWord = null;
    tempStates.add(new State<>(nextStateId++));
    FstBuildResult result = new FstBuildResult(ResultCode.SUCCESS);

    for (InputEntry<O> inputEntry : input.getIterable()) {
      currentWord = inputEntry.getKey();
      currentOutput = inputEntry.getValue();
      if (StringUtils.isBlank(currentWord)) {
        result.setCode(ResultCode.EMPTY_KEY);
        break;
      }

      int prefixLength = getCommonPrefixLength(previousWord, currentWord);
      boolean inOrder = inOrder(previousWord, currentWord);
      if (!inOrder) {
        result.setCode(ResultCode.UNSORTED_KEY);
        break;
      }

      if (StringUtils.equals(previousWord, currentWord)) {
        result.setCode(ResultCode.DUPLICATE_KEY);
        break;
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

    if (!result.isSuccess()) {
      return result;
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
    result.setCode(ResultCode.SUCCESS);
    return result;
  }

  private void writeOutput(int prefixLength) {
    if (!fstWriter.isNeedOutput()) {
      return;
    }
    for (int i = 1; i <= prefixLength; i++) {
      State<O> previousState = tempStates.get(i - 1);
      char arc = currentWord.charAt(i - 1);

      Output<O> prevOut = previousState.getOutput(arc);
      Output<O> commonPrefix = currentOutput.getCommonPrefix(prevOut);
      Output<O> wordSuffix = prevOut.getSuffix(commonPrefix);

      previousState.setOutput(arc, commonPrefix);

      if (!wordSuffix.empty()) {
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
      stateTmp.setOutput(arc,currentOutput);
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
    int aLen = a.length();
    int bLen = b.length();
    while (l < aLen && l < bLen) {
      if (a.charAt(l) > b.charAt(l)) {
        return false;
      }
      l++;
    }
    return true;
  }
}
