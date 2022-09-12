package heylichen.fst.serialize;

import heylichen.fst.FstBuilder;
import heylichen.fst.input.InputIterable;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class FstDumpTest {
  @Test
  public void name() {
    System.out.println(String.format("%10d",2));
  }

  @Test
  public void testDump() throws IOException {
    InputIterable<Integer> input = FstTestInputFactory.newMultipleKeyWithJumpTable();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    FstBuilder<Integer> fstBuilder = new FstBuilder<>();
    fstBuilder.dump(input, os, true, true);

    String dump = new String(os.toByteArray(), StandardCharsets.UTF_8);
    System.out.println(dump);
  }
}
