package heylichen.fst.matcher;

import heylichen.fst.output.IntOutput;
import heylichen.fst.output.Output;
import heylichen.fst.output.OutputFactory;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
   * search all keys that are with maxEdits edit distance of key
   *
   * @param key
   * @param maxEdits
   * @return
   */
  public List<String> searchByEditDistance(String key, int maxEdits) {
    if (StringUtils.isBlank(key)) {
      return Collections.emptyList();
    }

    RowLevenshteinAutomata la = new RowLevenshteinAutomata(key, maxEdits);
    List<String> keys = new ArrayList<>();
    VisitContext<O> context = matcher.noPrefixVisitContext((k, o) -> {
      keys.add(k);
    }, la);
    matcher.depthFirstVisit(context);
    return keys;
  }

  public void enumerate(BiConsumer<String, Output<O>> consumer) {
    VisitContext<O> context = matcher.noPrefixVisitContext(consumer, DummyAutomaton.INSTANCE);
    matcher.depthFirstVisit(context);
  }
}
