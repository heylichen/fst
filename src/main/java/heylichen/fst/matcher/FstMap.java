package heylichen.fst.matcher;

import heylichen.fst.output.IntOutput;
import heylichen.fst.output.Output;
import heylichen.fst.output.OutputFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;

public class FstMap<O> {
  private Matcher<O> matcher;

  public FstMap(OutputFactory<O> outputFactory, RandomAccessInput randomAccessInput) throws IOException {
    matcher = new Matcher<>(outputFactory, randomAccessInput);
  }

  /**
   * search  output mapping to the key
   *
   * @param key
   * @return
   * @throws IOException
   */
  public O get(String key) throws IOException {
    Output<O> output = (Output<O>) new IntOutput(0);
    boolean found = matcher.match(key, k -> output.setData(k.getData()), null);
    return found ? output.getData() : null;
  }

  /**
   * search all keys that has a common prefix with key
   * @param key
   * @param consumer
   * @return
   * @throws IOException
   */
  public boolean commonPrefixSearch(String key, BiConsumer<Integer, Output<O>> consumer) throws IOException {
    return matcher.match(key, null, consumer);
  }

  public boolean predictiveSearch(String key, BiConsumer<String, Output<O>> consumer) {
    MutableBoolean mb = new MutableBoolean(false);
    VisitContext<O> context = matcher.prefixVisitContext(key, (String word, Output<O> output) -> {
      mb.setTrue();
      consumer.accept(word, output);
    }, DummyAutomaton.INSTANCE);

    matcher.depthFirstVisit(context);
    return mb.getValue();
  }

  public List<Pair<String,Output<O>>> predictiveSearch(String key){
    List<Pair<String, Output<O>>> list = new ArrayList<>();
    predictiveSearch(key,(String word, Output<O> output) -> {
      list.add(Pair.of(word, output));
    });
    return list;
  }

  /**
   * search all keys that are with maxEdits edit distance of key
   *
   * @param key
   * @param maxEdits
   * @return
   */
  public Set<String> searchByEditDistance(String key, int maxEdits) {
    Set<String> keys = new HashSet<>();
    matcher.searchByEditDistance(key,maxEdits,(k, o) -> {
      keys.add(k);
    });
    return keys;
  }

  public List<Suggestion<O>> suggestSearch(String key) {
    return matcher.suggestSearch(key);
  }

  public void enumerate(BiConsumer<String, Output<O>> consumer) {
    VisitContext<O> context = matcher.noPrefixVisitContext(consumer, DummyAutomaton.INSTANCE);
    matcher.depthFirstVisit(context);
  }
}
