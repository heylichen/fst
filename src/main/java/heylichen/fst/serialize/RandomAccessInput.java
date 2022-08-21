package heylichen.fst.serialize;

import java.io.IOException;

public interface RandomAccessInput {

  void seek(long pos) throws IOException;

  byte[] readBackward(int bytes);

  byte readBackward();
}
