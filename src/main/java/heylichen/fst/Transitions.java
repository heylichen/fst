package heylichen.fst;

import heylichen.fst.output.Output;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class Transitions<O> {
  private List<CharTransition<O>> charTransitions;

  public int size() {
    return charTransitions.size();
  }

  public boolean empty() {
    return size() == 0;
  }

  public Output<O> getOutput(char arc) {
    CharTransition<O> charTransition = get(arc);
    assert charTransition != null;
    return charTransition.getTransition().getOutput();
  }

  private CharTransition<O> get(char arc) {
    for (CharTransition<O> charTransition : charTransitions) {
      if (charTransition.getCh() == arc) {
        return charTransition;
      }
    }
    return null;
  }

  public CharTransition<O> get(int i) {
    return charTransitions.get(i);
  }

  public void foreach(BiConsumer<Character, Transition<O>> consumer) {
    for (CharTransition<O> charTransition : charTransitions) {
      consumer.accept(charTransition.getCh(), charTransition.getTransition());
    }
  }

  public void clear() {
    charTransitions.clear();
  }

  public void setTransition(char arc, State<O> state) {
    CharTransition<O> charTransition = get(arc);
    if (charTransition == null) {
      charTransition = new CharTransition<>();
      charTransitions.add(charTransition);
    }
    Transition<O> t = charTransition.getTransition();
    t.setId(state.getId());
    t.setToFinal(state.isFinalState());
    t.setStateOutput(state.getStateOutput());
  }

  public void setOutput(char arc, Output<O> output) {
    CharTransition<O> charTransition = get(arc);
    assert charTransition != null;
    charTransition.getTransition().setOutput(output);
  }

  public void prependOutput(char arc, Output<O> output) {
    CharTransition<O> charTransition = get(arc);
    assert charTransition != null;
    Output<O> base = charTransition.getTransition().getOutput();
    base.prepend(output);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Transitions<O> that = (Transitions<O>) o;
    if (charTransitions.size() != that.charTransitions.size()) {
      return false;
    }

    List<CharTransition<O>> thatMap = that.charTransitions;
    for (int i = 0; i < charTransitions.size(); i++) {
      CharTransition<O> thisTran = charTransitions.get(i);
      CharTransition<O> thatTran = thatMap.get(i);
      if (!thisTran.equals(thatTran)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(charTransitions);
  }
}
