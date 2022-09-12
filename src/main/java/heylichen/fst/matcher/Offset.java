package heylichen.fst.matcher;

/**
 * a Mutable offset
 */
public class Offset {
  private long offset;

  public Offset() {
    offset = 0;
  }

  public Offset(long offset) {
    this.offset = offset;
  }

  public void add(long delta) {
    offset += delta;
  }

  public void subtract(long delta) {
    offset -= delta;
  }

  public long get() {
    return offset;
  }

  public long getAndAdd(long delta) {
    long v = offset;
    offset += delta;
    return v;
  }

  public void setOffset(long offset) {
    this.offset = offset;
  }
}
