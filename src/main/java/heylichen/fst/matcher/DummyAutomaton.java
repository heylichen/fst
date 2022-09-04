package heylichen.fst.matcher;

public class DummyAutomaton implements Automaton {
  public static final DummyAutomaton INSTANCE = new DummyAutomaton();

  @Override
  public void step(char ch) {

  }

  @Override
  public boolean isMatch() {
    return false;
  }

  @Override
  public boolean canMatch() {
    return false;
  }

  @Override
  public Automaton copy() {
    return this;
  }
}
