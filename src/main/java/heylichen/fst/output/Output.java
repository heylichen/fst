package heylichen.fst.output;

import java.io.IOException;
import java.io.OutputStream;

public interface Output<T> {
  OutputType type();

  boolean empty();

  T getInitValue();

  void prepend(Output<T> preValue);

  Output<T> getSuffix(Output <T> prefix);

  Output<T> getCommonPrefix(Output<T> other);

  int getByteValueSize();

  void writeByteValue(OutputStream os) throws IOException;

  void writeByteValue(OutputStream os, T value) throws IOException;

  T getData();
}
