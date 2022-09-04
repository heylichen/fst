package heylichen.fst.input;

import heylichen.fst.output.OutputType;

import java.util.List;

public class DataInputIterable<O> implements InputIterable<O> {
  private List<InputEntry<O>> data;

  public DataInputIterable(List<InputEntry<O>> data) {
    this.data = data;
  }

  @Override
  public Iterable<InputEntry<O>> getIterable() {
    return data;
  }

  @Override
  public OutputType getOutputType() {
    return OutputType.INT;
  }
}

