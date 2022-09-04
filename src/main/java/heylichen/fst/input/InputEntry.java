package heylichen.fst.input;

import heylichen.fst.output.Output;

public interface InputEntry<O> {
  String getKey();
  Output<O> getValue();
}
