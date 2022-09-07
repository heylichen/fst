package heylichen.fst.matcher;

import heylichen.fst.output.Output;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class FstSet {
  private final Matcher<Object> matcher;

  public FstSet(RandomAccessInput randomAccessInput) throws IOException {
    matcher = new Matcher<>(null, randomAccessInput);
  }

  public boolean contains(String key) throws IOException {
    return matcher.match(key, null, null);
  }

  /**
   * search all keys that has a common prefix with key
   *
   * @param key
   * @param consumer
   * @return
   * @throws IOException
   */
  public boolean commonPrefixSearch(String key, Consumer<Integer> consumer) throws IOException {
    return matcher.match(key, null, (len, o) -> consumer.accept(len));
  }

  public boolean predictiveSearch(String key, Consumer<String> consumer) {
    MutableBoolean mb = new MutableBoolean(false);
    VisitContext context = matcher.prefixVisitContext(key, (String word, Output<Object> output) -> {
      mb.setTrue();
      consumer.accept(word);
    }, DummyAutomaton.INSTANCE);

    matcher.depthFirstVisit(context);
    return mb.getValue();
  }

  public List<String> predictiveSearch(String key) {
    List<String> list = new ArrayList<>();
    predictiveSearch(key, (String word) -> {
      list.add(word);
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
    matcher.searchByEditDistance(key, maxEdits, (String k,  Output<Object> o) -> {
      keys.add(k);
    });
    return keys;
  }

  public List<Suggestion<Object>> suggestSearch(String key) {
    return matcher.suggestSearch(key);
  }

  public void enumerate(Consumer<String> consumer) {
    VisitContext context = matcher.noPrefixVisitContext((k, o) -> consumer.accept(k), DummyAutomaton.INSTANCE);
    matcher.depthFirstVisit(context);
  }
}
