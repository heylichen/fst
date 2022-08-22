package heylichen.fst.output;

import java.io.IOException;
import java.io.OutputStream;

public interface Output<T> {
  OutputType type();

  boolean empty(T value);

  T getInitValue();

  void prepend(Output<T> preValue);

  T getSuffix(T base, T prefix);

  T getCommonPrefix(T a, T b);

  int getByteValueSize(T value);

  void writeByteValue(OutputStream os, T value) throws IOException;

  T getData();
}
