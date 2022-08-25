package heylichen.fst.serialize;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

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
  public void foreach(Consumer<InputEntry<O>> consumer) {
    for (InputEntry<O> datum : data) {
      consumer.accept(datum);
    }
  }
}

