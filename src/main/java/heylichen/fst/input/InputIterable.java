package heylichen.fst.input;

import heylichen.fst.output.OutputType;

public interface InputIterable<O> {
  Iterable<InputEntry<O>> getIterable();

  OutputType getOutputType();
}
