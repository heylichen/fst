package heylichen.fst;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CharTransition<O> {
  private char ch;
  private Transition<O> transition;

  public CharTransition(char ch) {
    this.ch = ch;
    transition = new Transition<>();
  }

  public boolean equals(CharTransition<O> that) {
    return ch == that.ch && transition.equals(that.transition);
  }
}