package heylichen.fst.serialize;

import heylichen.fst.output.OutputType;

import java.io.IOException;

public interface RandomAccessInput<O> {

  void seek(long pos) throws IOException;

  byte[] readBackward(int bytes);

  byte readBackward();

  Iterable<InputEntry<O>> getIterable();

  OutputType getOutputType();
}
