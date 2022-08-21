package heylichen.fst.output;

import lombok.Getter;

public enum OutputType {
  INVALID(-1),
  NONE(0),
  INT(1),
  LONG(2),
  STRING(3);
  @Getter
  private final int value;

  OutputType(int value) {
    this.value = value;
  }

  public static OutputType parse(int v) {
    OutputType result = INVALID;
    switch (v) {
      case 0:
        result = NONE;
        break;
      case 1:
        result = INT ;
        break;
      case 2:
        result = LONG ;
        break;case 3:
        result = STRING ;
        break;
      default:
    }
    return result;
  }
}
