package heylichen.fst;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class State<O> {
  private long id;
  private boolean finalState;
  private Transitions<O> transitions;
  O stateOutput;
}
