package heylichen.fst.serialize;

import heylichen.fst.State;

import java.io.IOException;

public interface FstWriter<O> {

  void write(State<O> state, char previousArc) throws IOException;

  void close() throws IOException;
}
