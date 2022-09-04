package heylichen.fst.output;

import java.io.IOException;
import java.io.OutputStream;

public interface Output<T> {

  T getInitValue();
  boolean empty();

  T getData();

  void setData(T o);

  Output<T> getSuffix(Output <T> prefix);

  Output<T> getCommonPrefix(Output<T> other);

  int getByteValueSize();

  void writeByteValue(OutputStream os) throws IOException;

  void prepend(Output<T> preValue);

  void append(Output<T> v);

  /**
   * append this with v, not modify this
   * @param v
   * @return
   */
  Output<T> appendCopy(Output<T> v);

  Output<T> copy();
}
