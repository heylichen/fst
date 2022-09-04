package heylichen.fst.matcher;

import heylichen.fst.FstBuildResult;
import heylichen.fst.FstBuilder;
import heylichen.fst.input.InputIterable;
import heylichen.fst.output.IntOutput;
import heylichen.fst.serialize.FstTestInputFactory;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class FstMapTest {

  @Test
  public void testFstWrite() throws IOException {
    InputIterable<Integer> input = FstTestInputFactory.newSingleKey();
    compile(input);
  }

  @Test
  public void testMapGetSingle() throws IOException {
    InputIterable<Integer> input = FstTestInputFactory.newSingleKey();
    byte[] bytes = compile(input);

    RandomAccessInput di = new ByteArrayInput(bytes);
    FstMap<Integer> map = new FstMap<>(new IntOutput(0), di);
    Integer out = map.get("say");
    System.out.println(out);
  }

  @Test
  public void testMapGet() throws IOException {
    InputIterable<Integer> input = FstTestInputFactory.newMultipleKeyWithJumpTable();
    byte[] bytes = compile(input);

    RandomAccessInput di = new ByteArrayInput(bytes);
    FstMap<Integer> map = new FstMap<>(new IntOutput(0), di);
    Integer out = map.get("say");
    System.out.println(out);

    System.out.println(map.get("sb"));
    System.out.println(map.get("sc"));
  }

  @Test
  public void testMapGet2() throws IOException {
    InputIterable<Integer> input = FstTestInputFactory.newInputForEditDistance();
    byte[] bytes = compile(input);

    RandomAccessInput di = new ByteArrayInput(bytes);
    FstMap<Integer> map = new FstMap<>(new IntOutput(0), di);

    System.out.println(map.get("thir"));
  }

  private byte[] compile(InputIterable<Integer> input) throws IOException {
    FstBuilder<Integer> fstBuilder = new FstBuilder<>();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    FstBuildResult result = fstBuilder.compile(input, os, true);
    if (!result.isSuccess()) {
      System.err.println(result.getCode());
    }
    return os.toByteArray();
  }

  @Test
  public void searchByEditDistance() throws IOException {
    InputIterable<Integer> input = FstTestInputFactory.newInputForEditDistance();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    FstBuilder<Integer> fstBuilder = new FstBuilder<>();

    fstBuilder.compile(input, os, true);

    RandomAccessInput di = new ByteArrayInput(os.toByteArray());

    FstMap<Integer> map = new FstMap<>(new IntOutput(0), di);
    List<String> found = map.searchByEditDistance("thier", 2);
    System.out.println(found);
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