package heylichen.fst.serialize;

import heylichen.fst.FstBuilder;
import heylichen.fst.output.IntOutput;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

public class FstWriterTest {

  @Test
  public void testFstWrite() throws IOException {
    List<InputEntry<Integer>> list = Arrays.asList(
        new SimpleInputEntry<>("say", new IntOutput(31))
    );

    FstBuilder<Integer> fstBuilder = new FstBuilder();
    OutputStream os = new ByteArrayOutputStream();
    fstBuilder.compile(newInput(list), os, true);
    System.out.println("ok");
  }

  private RandomAccessInput<Integer> newInput(List<InputEntry<Integer>> list) {
    return new RandomAccessInput<Integer>() {
      @Override
      public void seek(long pos) throws IOException {

      }

      @Override
      public byte[] readBackward(int bytes) {
        return new byte[0];
      }

      @Override
      public byte readBackward() {
        return 0;
      }

      @Override
      public Iterable<InputEntry<Integer>> getIterable() {
        return list;
      }
    };
  }
}