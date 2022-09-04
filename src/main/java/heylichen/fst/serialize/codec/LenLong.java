package heylichen.fst.serialize.codec;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LenLong {
  private int len;
  private long value;

  public LenLong(int len, long value) {
    this.len = len;
    this.value = value;
  }
}
