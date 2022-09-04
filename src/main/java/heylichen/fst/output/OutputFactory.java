package heylichen.fst.output;

import heylichen.fst.matcher.RandomAccessInput;
import org.apache.commons.lang3.tuple.Pair;

public interface OutputFactory<O> {
  Output<O> newInstance();

  OutputType getOutputType();

  Pair<Output<O>,Integer> readByteValue(RandomAccessInput input, long offset);
}
