package heylichen.fst.matcher;

public interface Automaton {

  void step(char ch);

  boolean isMatch();

  boolean canMatch();

  Automaton copy();
}
