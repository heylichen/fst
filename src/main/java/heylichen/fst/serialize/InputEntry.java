package heylichen.fst.serialize;

public interface InputEntry<O> {
  String getKey();
  O getValue();
}
