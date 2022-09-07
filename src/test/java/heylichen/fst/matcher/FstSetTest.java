package heylichen.fst.matcher;

import heylichen.fst.FstBuilder;
import heylichen.fst.input.InputIterable;
import heylichen.fst.serialize.FstTestInputFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FstSetTest {

  @Test
  public void searchByEditDistance() throws IOException {
    FstSet map = compileSet(FstTestInputFactory.newInputForEditDistance());
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
    FstSet map = compileSet(FstTestInputFactory.newInputForEditDistance());
    Set<String> found = new HashSet<>();
    map.enumerate((k ) -> found.add(k));

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
    FstSet map = compileSet(FstTestInputFactory.newInputForPredictive());
    Set<String> found = new HashSet<>();
    String string = "predictiv";
    map.commonPrefixSearch(string, (k ) -> found.add(string.substring(0, k)));

    Set<String> expected = new HashSet<>();
    expected.add("predictiv");
    expected.add("predicti");
    expected.add("predic");
    Assert.assertEquals(expected, found);
  }

  @Test
  public void testPredictive() throws IOException {
    FstSet map = compileSet(FstTestInputFactory.newInputForPredictive());
    List<String> list = map.predictiveSearch("predictiv");
    Set<String> foundKeys = new HashSet<>(list);

    Set<String> expected = new HashSet<>();
    expected.add("predictiv");
    expected.add("predictive");
    expected.add("predictively");
    expected.add("predictiveness");
    Assert.assertEquals(expected, foundKeys);
  }

  @Test
  public void testSuggest() throws IOException {
    FstSet map = compileSet(FstTestInputFactory.newInputForSuggest());
    List<Suggestion<Object>> list = map.suggestSearch("thier");
    Assert.assertEquals(5, list.size());
  }

  private <T extends Object> FstSet compileSet(InputIterable<T> input) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    FstBuilder<T> fstBuilder = new FstBuilder<>();
    fstBuilder.compile(input, os, false,false);

    RandomAccessInput di = new ByteArrayInput(os.toByteArray());
    return new FstSet(di);
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