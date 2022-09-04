package heylichen.fst;

import heylichen.fst.output.Output;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class Transition<O> {
  private long id;
  private boolean toFinal;
  private Output<O> stateOutput;
  private Output<O> output;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Transition<?> that = (Transition<?>) o;
    return id == that.id && toFinal == that.toFinal &&
        Objects.equals(stateOutput, that.stateOutput)
        && Objects.equals(output, that.output);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, toFinal, stateOutput, output);
  }
}
