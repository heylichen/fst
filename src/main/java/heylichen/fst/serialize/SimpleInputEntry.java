package heylichen.fst.serialize;

public class SimpleInputEntry<O> implements InputEntry<O> {
  private String key;
  private O data;

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public O getValue() {
    return data;
  }
}

