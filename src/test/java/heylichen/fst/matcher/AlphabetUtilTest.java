package heylichen.fst.matcher;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class AlphabetUtilTest {

  @Test
  public void name() {
    char a = '\u0430';
    System.out.println(AlphabetUtil.calculateUTF8ByteCount(String.valueOf(a).getBytes(StandardCharsets.UTF_8)[0]));
    a = 'a';
    System.out.println(AlphabetUtil.calculateUTF8ByteCount(String.valueOf(a).getBytes(StandardCharsets.UTF_8)[0]));

    a = '\u4e8c';
    System.out.println(AlphabetUtil.calculateUTF8ByteCount(String.valueOf(a).getBytes(StandardCharsets.UTF_8)[0]));
  }
}