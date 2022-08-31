package heylichen.fst.serialize;

import heylichen.fst.output.OutputType;

import java.io.IOException;
import java.util.List;

public class DataRandomAccessInput<O> implements RandomAccessInput<O> {
  private List<InputEntry<O>> data;
  private int pos;

  @Override
  public void seek(long pos) throws IOException {
    pos= (int) pos;
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
  public Iterable<InputEntry<O>> getIterable() {
    return data;
  }

  @Override
  public OutputType getOutputType() {
    return OutputType.INT;
  }
}

