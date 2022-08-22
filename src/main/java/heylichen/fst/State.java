package heylichen.fst;

import heylichen.fst.output.Output;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
@Getter
@Setter
public class State<O> {
  private long id;
  private boolean finalState;
  private Transitions<O> transitions;
  Output<O> stateOutput;

  public Output<O> getOutput(char arc) {
    return transitions.getOutput(arc);
  }

  public void setTransition(char arc, State<O> state) {
    transitions.setTransition(arc, state);
  }

  public void setOutput(char arc, Output<O> output) {
    transitions.setOutput(arc, output);
  }

  public void prependSuffixToOutput(char arc, Output<O> suffix) {
    transitions.prependOutput(arc, suffix);
  }

  public void prependSuffixToStateOutput(Output<O> suffix) {
    stateOutput.prepend(suffix);
  }

  public boolean equals(State<O> s) {
    if (this == s) {
      return true;
    }
    return finalState == s.finalState && Objects.equals(stateOutput, s.stateOutput) &&
        Objects.equals(transitions, s.transitions);
  }

  public void reuse(long stateId) {
    id = stateId;
    finalState = false;
    transitions.clear();
    try {
      stateOutput = stateOutput.getClass().newInstance();
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  //TODO hash
}
