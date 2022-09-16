package heylichen.fst.serialize;

import heylichen.fst.FstBuilder;
import heylichen.fst.input.InputIterable;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class FstDotWriterTest {

  @Test
  public void testDotSample() throws IOException {
    InputIterable<Integer> input = FstTestInputFactory.newSimpleSample();
    printDot(input);
  }

  @Test
  public void testDot1() throws IOException {
    InputIterable<Integer> input = FstTestInputFactory.newMultipleKeyWithJumpTable();
    printDot(input);
  }

  @Test
  public void testDot2() throws IOException {
    InputIterable<Integer> input = FstTestInputFactory.newInputForEditDistance();
    printDot(input);
  }

  @Test
  public void testDot3() throws IOException {
    InputIterable<Integer> input = FstTestInputFactory.newInputForPredictive();
    printDot(input);
  }

  private void printDot(InputIterable<Integer> input) throws IOException {
    FstBuilder<Integer> fstBuilder = new FstBuilder<>();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    fstBuilder.compileDot(input, os, true);
    /**
     * after install graphviz, save output string in dfa.txt, run
     * dot -Tpdf dfa.txt -o dfa.pdf
     */
    System.out.println(new String(os.toByteArray(), StandardCharsets.UTF_8));
  }

}