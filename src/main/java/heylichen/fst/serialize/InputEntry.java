package heylichen.fst.serialize;

import heylichen.fst.output.Output;

public interface InputEntry<O> {
  String getKey();
  Output<O> getValue();
}
