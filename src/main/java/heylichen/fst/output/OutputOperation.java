package heylichen.fst.output;

import java.io.IOException;
import java.io.OutputStream;

public interface OutputOperation<T> {
  OutputType type();

  boolean empty(T value);

  T getInitValue();

  void prepend(T base, T value);

  T getSuffix(T base, T prefix);

  T getCommonPrefix(T a, T b);

  int getByteValueSize(T value);

  void writeByteValue(OutputStream os, T value) throws IOException;

//  int readByteValue()

}
