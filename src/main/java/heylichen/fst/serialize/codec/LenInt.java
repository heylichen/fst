package heylichen.fst.serialize.codec;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LenInt {
  private int len;
  private int value;

  public LenInt(int len, int value) {
    this.len = len;
    this.value = value;
  }
}
