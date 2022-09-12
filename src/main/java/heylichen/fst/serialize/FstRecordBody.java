package heylichen.fst.serialize;

import heylichen.fst.output.Output;
import lombok.Getter;
import lombok.Setter;

/**
 * a transition record of one state.
 * a state may have many transition records.
 * @param <T>
 */
@Getter
@Setter
public class FstRecordBody<T> {
  private char label;
  // delta is position diff between current transition and next state
  // if current state is last state, delta is 0
  private int delta;
  private Output<T> output;
  private Output<T> stateOutput;

  //for writer
  public FstRecordBody() {
  }

  // for read in Matcher
  public FstRecordBody(char label, int delta, Output<T> output, Output<T> stateOutput) {
    this.label = label;
    this.delta = delta;
    this.output = output;
    this.stateOutput = stateOutput;
  }

  public boolean isStateOutputEmpty() {
    return stateOutput == null || stateOutput.empty();
  }
}
