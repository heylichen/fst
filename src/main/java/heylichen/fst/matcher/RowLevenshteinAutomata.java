package heylichen.fst.matcher;

import java.util.ArrayList;
import java.util.List;

public class RowLevenshteinAutomata implements Automaton {
  private List<Integer> state;
  private final String string;
  private final int maxEdits;

  public RowLevenshteinAutomata(String string, int maxEdits) {
    this.string = string;
    List<Integer> localState = new ArrayList<>(string.length() + 1);
    localState.add(0);
    for (int i = 0; i < string.length(); i++) {
      localState.add(i + 1);
    }
    this.state = localState;
    this.maxEdits = maxEdits;
  }

  public void step(char ch) {
    List<Integer> newState = new ArrayList<>(string.length() + 1);
    newState.add(state.get(0) + 1);

    for (int i = 0; i < string.length(); i++) {
      int cost = ch == string.charAt(i) ? 0 : 1;
      // dist = min(state.get(i) + cost, state.get(i + 1) + 1, newState.get(i) + 1)
      int dist = Math.min(
          state.get(i) + cost,
          state.get(i + 1) + 1
      );
      dist = Math.min(dist, newState.get(i) + 1);
      newState.add(dist);
    }
    this.state = newState;
  }

  public boolean isMatch() {
    return state.get(state.size() - 1) <= maxEdits;
  }

  public boolean canMatch() {
    for (Integer integer : state) {
      if (integer <= maxEdits) {
        return true;
      }
    }
    return false;
  }

  public List<Character> transitions() {
    List<Character> result = new ArrayList<>(string.length());
    for (int i = 0; i < string.length(); i++) {
      if (state.get(i) <= maxEdits) {
        result.add(string.charAt(i));
      }
    }
    return result;
  }

  @Override
  public Automaton copy() {
    RowLevenshteinAutomata l = new RowLevenshteinAutomata(string, maxEdits);
    l.state = new ArrayList<>(state);
    return l;
  }
}
