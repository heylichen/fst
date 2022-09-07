package heylichen.fst.input;

import heylichen.fst.output.OutputType;

import java.util.List;

public class IntDataInputIterable implements InputIterable<Integer> {
  private List<InputEntry<Integer>> data;

  public IntDataInputIterable(List<InputEntry<Integer>> data) {
    this.data = data;
  }

  @Override
  public Iterable<InputEntry<Integer>> getIterable() {
    return data;
  }

  @Override
  public OutputType getOutputType() {
    return OutputType.INT;
  }
}

