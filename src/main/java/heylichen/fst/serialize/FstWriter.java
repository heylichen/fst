package heylichen.fst.serialize;

import heylichen.fst.CharTransition;
import heylichen.fst.State;
import heylichen.fst.Transition;
import heylichen.fst.Transitions;
import lombok.Getter;

import java.io.OutputStream;
import java.util.*;

public class FstWriter<O, I extends InputEntry<O>> {
  private OutputStream os;
  private boolean needOutput;
  private boolean needStateOutput;
  private Map<Character, Integer> charIndexMap;

  private Map<Character, Counter> charCountMap;
  private Map<Integer, Counter> biGramCountMap;

  private long address = 0;
  //one state record has one entry. key is state id, value is index in arcAddressTable
  private Map<Long, Integer> stateRecordIndexMap;
  //the arcs of all transitions of all states
  private List<Long> arcAddressTable;

  public FstWriter(OutputStream os, boolean needOutput, boolean needStateOutput,
                   RandomAccessInput input) {
    this.os = os;
    this.needOutput = needOutput;
    this.needStateOutput = needStateOutput;
    initCharIndexTable(input);
  }


  private void initCharIndexTable(RandomAccessInput<I> input) {
    charCountMap = new HashMap<>();
    biGramCountMap = new HashMap<>();

    input.foreach((I en) -> {
      String keyWord = en.getKey();
      char prev = 0;
      for (char ch : keyWord.toCharArray()) {
        charCountMap.computeIfAbsent(ch, k -> new Counter(k, 0)).increment();
        biGramCountMap.computeIfAbsent(genBiGramKey(prev, ch), k -> new Counter(0)).increment();
      }
    });

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

  public void write(State<O> state, char previousArc) {
    Transitions<O> transitions = state.getTransitions();
    int transitionCount = transitions == null ? 0 : transitions.size();

    int charIndexSize = RecordHeader.getCharIndexSize(needOutput, needStateOutput);
    boolean needJumpTable = transitionCount >= 8;
    List<Integer> arcIndexes = getArcAccessIndexes(previousArc, transitions);
    reverse(arcIndexes);
    for (Integer arcIndex : arcIndexes) {
      processTransition(arcIndex, state);
    }
  }

  private void processTransition(Integer arcIndex, State<O> state) {
    Transitions<O> transitions = state.getTransitions();
    CharTransition<O> charTransition = transitions.get(arcIndex);
    char arc = charTransition.getCh();
    Transition<O> transition = charTransition.getTransition();
    //TODO
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
    int len = indexes.size() / 2;
    int last = indexes.size() - 1;
    for (int i = 0; i <= len; i++) {
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
    for (int i = 0; i < transitionCount; i--) {
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
