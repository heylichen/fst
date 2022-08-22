package heylichen.fst;

import heylichen.fst.output.Output;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

public class Transitions<O> {
  //keep insertion order
  private LinkedHashMap<Character, Transition<O>> arcToStateAndOutputMap;

  public int size() {
    return arcToStateAndOutputMap.size();
  }

  public boolean empty() {
    return size() == 0;
  }

  public Output<O> getOutput(char arc) {
    return arcToStateAndOutputMap.get(arc).getOutput();
  }

  public void foreach(BiConsumer<Character, Transition<O>> consumer) {
    for (Map.Entry<Character, Transition<O>> entry : arcToStateAndOutputMap.entrySet()) {
      consumer.accept(entry.getKey(), entry.getValue());
    }
  }

  public void clear() {
    arcToStateAndOutputMap.clear();
  }

  public void setTransition(char arc, State<O> state) {
    Transition<O> t = arcToStateAndOutputMap.computeIfAbsent(arc, k -> new Transition<O>());
    t.setId(state.getId());
    t.setToFinal(state.isFinalState());
    t.setStateOutput(state.getStateOutput());
  }

  public void setOutput(char arc, Output<O> output) {
    arcToStateAndOutputMap.get(arc).setOutput(output);
  }

  public void prependOutput(char arc, Output<O> output) {
    Output<O> base = arcToStateAndOutputMap.get(arc).getOutput();
    base.prepend(output);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Transitions<O> that = (Transitions<O>) o;
    if (arcToStateAndOutputMap.size() != that.arcToStateAndOutputMap.size()) {
      return false;
    }

    LinkedHashMap<Character, Transition<O>> thatMap = that.arcToStateAndOutputMap;
    for (Map.Entry<Character, Transition<O>> entry : arcToStateAndOutputMap.entrySet()) {
      Transition thatTran = thatMap.get(entry.getKey());
      if (thatTran == null) {
        return false;
      }
      if (!thatTran.equals(entry.getValue())) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(arcToStateAndOutputMap);
  }
}
