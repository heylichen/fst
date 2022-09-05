package heylichen.fst.matcher;

public class DummyAutomaton implements Automaton {
  public static final DummyAutomaton INSTANCE = new DummyAutomaton();

  @Override
  public void step(char ch) {
    //no op
  }

  @Override
  public boolean isMatch() {
    return true;
  }

  @Override
  public boolean canMatch() {
    return true;
  }

  @Override
  public Automaton copy() {
    return this;
  }
}
