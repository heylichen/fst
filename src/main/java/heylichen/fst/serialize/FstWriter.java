package heylichen.fst.serialize;

import lombok.Getter;

import java.io.OutputStream;
import java.util.*;

public class FstWriter<O, I extends InputEntry<O>> {
  private OutputStream os;
  private boolean needOutput;
  private boolean needStateOutput;
  private Map<Character, Integer> charIndexMap;

  public FstWriter(OutputStream os, boolean needOutput, boolean needStateOutput,
                   RandomAccessInput input) {
    this.os = os;
    this.needOutput = needOutput;
    this.needStateOutput = needStateOutput;
    initCharIndexTable(input);
  }

  private Map<Character, Counter> charCountMap;
  private Map<Integer, Counter> biGramCountMap;

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

  public void write() {

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
