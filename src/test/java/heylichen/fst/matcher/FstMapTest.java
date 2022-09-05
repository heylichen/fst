package heylichen.fst.matcher;

import heylichen.fst.FstBuilder;
import heylichen.fst.input.InputEntry;
import heylichen.fst.input.InputIterable;
import heylichen.fst.output.IntOutput;
import heylichen.fst.output.Output;
import heylichen.fst.output.OutputFactory;
import heylichen.fst.serialize.FstTestInputFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FstMapTest {

  @Test
  public void testFstWrite() throws IOException {
    InputIterable<Integer> input = FstTestInputFactory.newSingleKey();
    compileIntMap(input);
  }

  @Test
  public void testMapGetSingle() throws IOException {
    InputIterable<Integer> input = FstTestInputFactory.newSingleKey();
    FstMap<Integer> map = compileIntMap(input);
    Integer out = map.get("say");
    Assert.assertEquals(31, out.intValue());
  }

  @Test
  public void testMapGet() throws IOException {
    InputIterable<Integer> input = FstTestInputFactory.newMultipleKeyWithJumpTable();
    FstMap<Integer> map = compileIntMap(input);

    for (InputEntry<Integer> entry : input.getIterable()) {
      Integer out = map.get(entry.getKey());
      Assert.assertEquals("key=" + entry.getKey() + " get error", entry.getValue().getData(), out);
    }
  }

  @Test
  public void searchByEditDistance() throws IOException {
    FstMap<Integer> map = compileIntMap(FstTestInputFactory.newInputForEditDistance());
    Set<String> found = map.searchByEditDistance("woof", 2);
    Set<String> expected = new HashSet<>();
    expected.add("1xoof");
    expected.add("xoof");
    expected.add("of");
    expected.add("oof");
    Assert.assertEquals(expected, found);
  }

  @Test
  public void testEnumerate() throws IOException {
    FstMap<Integer> map = compileIntMap(FstTestInputFactory.newInputForEditDistance());
    Set<String> found = new HashSet<>();
    map.enumerate((k, o) -> found.add(k));

    Set<String> expected = new HashSet<>();
    expected.add("1xoof");
    expected.add("xoof");
    expected.add("of");
    expected.add("oof");
    expected.add("11xoof");
    expected.add("f");
    Assert.assertEquals(expected, found);
  }

  @Test
  public void testCommonPrefixSearch() throws IOException {
    FstMap<Integer> map = compileIntMap(FstTestInputFactory.newInputForPredictive());
    Set<String> found = new HashSet<>();
    String string = "predictiv";
    map.commonPrefixSearch(string, (k, o) -> found.add(string.substring(0, k)));

    Set<String> expected = new HashSet<>();
    expected.add("predictiv");
    expected.add("predicti");
    expected.add("predic");
    Assert.assertEquals(expected, found);
  }

  @Test
  public void testPredictive() throws IOException {
    FstMap<Integer> map = compileIntMap(FstTestInputFactory.newInputForPredictive());
    List<Pair<String, Output<Integer>>> list = map.predictiveSearch("predictiv");
    Set<String> foundKeys = list.stream().map(Pair::getKey).collect(Collectors.toSet());

    Set<String> expected = new HashSet<>();
    expected.add("predictiv");
    expected.add("predictive");
    expected.add("predictively");
    expected.add("predictiveness");
    Assert.assertEquals(expected, foundKeys);
  }

  private FstMap<Integer> compileIntMap(InputIterable<Integer> input) throws IOException {
    return compileMap(input, new IntOutput(0));
  }

  private <O> FstMap<O> compileMap(InputIterable<O> input, OutputFactory<O> factory) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    FstBuilder<O> fstBuilder = new FstBuilder<>();
    fstBuilder.compile(input, os, true);

    RandomAccessInput di = new ByteArrayInput(os.toByteArray());
    return new FstMap<>(factory, di);
  }

  @Test
  public void testLm() {
    RowLevenshteinAutomata la = new RowLevenshteinAutomata("thier", 2);
    String key = "thief";
    System.out.println(canMatch(la, key));
  }

  private boolean canMatch(Automaton lm, String input) {
    int i = 0;
    for (; i < input.length() && lm.canMatch(); i++) {
      lm.step(input.charAt(i));
      if (i == input.length() - 1 && lm.isMatch()) {
        return true;
      }
    }
    return false;
  }
}