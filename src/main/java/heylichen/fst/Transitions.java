package heylichen.fst;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class Transitions<O> {
  //keep insertion order
  private LinkedHashMap<Character, Transition<O>> arcToStateAndOutputMap;

  public int size() {
    return arcToStateAndOutputMap.size();
  }

  public boolean empty() {
    return size() == 0;
  }

  public O getOutput(char arc) {
    return arcToStateAndOutputMap.get(arc).getOutput();
  }

  public void foreach(BiConsumer<Character, Transition<O>> consumer) {
    for (Map.Entry<Character, Transition<O>> entry : arcToStateAndOutputMap.entrySet()) {
      consumer.accept(entry.getKey(), entry.getValue());
    }
  }

  private void clear() {
    arcToStateAndOutputMap.clear();
  }

  public void setTransition(char arc, State<O> state) {
    Transition<O> t = arcToStateAndOutputMap.computeIfAbsent(arc,
        k -> new Transition<O>());
    t.setId(state.getId());
    t.setToFinal(state.isFinalState());
    t.setStateOutput(state.getStateOutput());
  }

  public void setOutput(char arc, O output) {
    arcToStateAndOutputMap.get(arc).setOutput(output);
  }


}
