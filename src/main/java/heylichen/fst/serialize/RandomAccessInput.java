package heylichen.fst.serialize;

import java.io.IOException;
import java.util.function.Consumer;

public interface RandomAccessInput<T extends InputEntry> {

  void seek(long pos) throws IOException;

  byte[] readBackward(int bytes);

  byte readBackward();

  void foreach(Consumer<T> consumer);
}
