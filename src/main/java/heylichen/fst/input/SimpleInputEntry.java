package heylichen.fst.input;

import heylichen.fst.output.Output;

public class SimpleInputEntry<O> implements InputEntry<O> {
  private String key;
  private Output<O> data;

  public SimpleInputEntry(String key, Output<O> data) {
    this.key = key;
    this.data = data;
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public Output<O> getValue() {
    return data;
  }
}

