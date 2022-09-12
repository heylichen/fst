package heylichen.fst.corpus;

import com.google.common.base.Splitter;
import heylichen.fst.FstBuilder;
import heylichen.fst.input.InputEntry;
import heylichen.fst.input.InputIterable;
import heylichen.fst.input.SimpleInputEntry;
import heylichen.fst.matcher.ByteArrayInput;
import heylichen.fst.matcher.FstSet;
import heylichen.fst.matcher.RandomAccessInput;
import heylichen.fst.output.OutputType;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

public class CorpusTest {
  public static final Pattern p = Pattern.compile("[—\\s\\.\\,;:\\?!\"\'\\)\\(\\[\\]\\#\\-\\*\\_\\$&%]");
  String a = "\\s\\-\\.";
  public static final Splitter SPLITTER = Splitter.on(p).omitEmptyStrings().trimResults();
  public static final char[] punctuation = {'.', ',', ';', ':', '?', '!', '"', '\'', ')', '('};
  public static final String OUT_FILE = "C:\\Users\\lc\\Desktop\\fstSet.out";


  @Test
  public void testContainsForDebug() throws IOException {
    List<String> expected = new ArrayList<>();
    for (InputEntry<Object> entry : newWordsInput().getIterable()) {
      String word = entry.getKey();
      expected.add(word);
    }

    FstSet map = compileSet(newWordsInput());
    Assert.assertTrue(map.contains("19"));
  }

  @Test
  public void testEnumWords() throws IOException {
    List<String> expected = new ArrayList<>();
    for (InputEntry<Object> entry : newWordsInput().getIterable()) {
      String word = entry.getKey();
      expected.add(word);
    }

    FstSet map = compileSet(newWordsInput());
    List<String> found = new ArrayList<>();

    map.enumerate((k) -> {
      found.add(k);
    });
    assertList(expected, found);
  }

  private void assertList(List<String> expected, List<String> found) {
    expected.sort(Comparator.naturalOrder());
    found.sort(Comparator.naturalOrder());

    for (int i = 0; i < expected.size(); i++) {
      if (!expected.get(i).equals(found.get(i))) {
        System.out.println(expected.get(i) + " not equals " + found.get(i));
      }
    }
    Assert.assertEquals(expected, found);
  }


  @Test
  public void testWriteToFile() throws IOException {
    try (FileOutputStream fi = new FileOutputStream(new File(OUT_FILE));
         BufferedOutputStream bos = new BufferedOutputStream(fi, 1024 * 1024 * 10)) {
      FstBuilder<Object> fstBuilder = new FstBuilder<>();
      fstBuilder.compile(newWordsInput(), bos, false, false);
    }
  }

  @Test
  public void testReadFromFile() throws IOException {
    byte[] data = FileUtils.readFileToByteArray(new File(OUT_FILE));
    RandomAccessInput ri = new ByteArrayInput(data);
    FstSet fstSet = new FstSet(ri);

    List<String> expected = new ArrayList<>();
    for (InputEntry<Object> entry : newWordsInput().getIterable()) {
      String word = entry.getKey();
      expected.add(word);
    }

    List<String> found = new ArrayList<>();
    fstSet.enumerate((k) -> {
      found.add(k);
    });

    assertList(expected, found);
  }


  @Test
  public void testDump() throws IOException {
    InputIterable<Object> input = newWordsInput();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    FstBuilder<Object> fstBuilder = new FstBuilder<>();
    fstBuilder.dump(input, os, false, false);

    String dump = new String(os.toByteArray(), StandardCharsets.UTF_8);
    System.out.println(dump);
  }

  @Test
  public void testPrintDot() throws IOException {
    printDot(newWordsInput());
  }

  private void printDot(InputIterable<Object> input) throws IOException {
    FstBuilder<Object> fstBuilder = new FstBuilder<>();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    fstBuilder.compileDot(input, os, false);
    /**
     * after install graphviz, save output string in dfa.txt, run
     * dot -Tpdf dfa.txt -o dfa.pdf
     */
    System.out.println(new String(os.toByteArray(), StandardCharsets.UTF_8));
  }

  private <T extends Object> FstSet compileSet(InputIterable<T> input) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    FstBuilder<T> fstBuilder = new FstBuilder<>();
    fstBuilder.compile(input, os, false, false);

    RandomAccessInput di = new ByteArrayInput(os.toByteArray());
    return new FstSet(di);
  }


  private InputIterable<Object> newWordsInput() throws IOException {
    File f = new File("C:\\Users\\lc\\Desktop\\1342-1.txt");
    String s = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
    List<String> words = SPLITTER.splitToList(s);
    Set<String> set = new HashSet<>();

    for (String word : words) {
      word = convertWord(word);
      if (word != null) {
        set.add(word);
      }
    }

    words = new ArrayList<>(set);
    words.sort(Comparator.naturalOrder());

    for (String word : words) {
      for (char c : word.toCharArray()) {
        if ((c >= '\uDC00' && c <= '\uDFFF') || (c >= '\uD800' && c <= '\uDBFF')) {
          System.out.println("FOUND----->");
        }
      }
    }
    return newInputWords(words);
  }

  private String convertWord(String word) {
    word = removePunctuation(word);
    if (word.endsWith("'s")) {
      word = word.substring(0, word.length() - 2);
    }
    word = processUpperCase(word);
    return word;
  }

  private String processUpperCase(String word) {
    if (word.length() < 2) {
      return word;
    }
    if (Character.isUpperCase(word.charAt(0)) && Character.isLowerCase(word.charAt(1))) {
      return Character.toLowerCase(word.charAt(0)) + word.substring(1);
    }
    return word;
  }

  private String removePunctuation(String word) {
    for (char c : punctuation) {
      if (word == null || word.length() == 0) {
        continue;
      }
      if (word.charAt(0) == c) {
        word = word.substring(1);
      }
      if (word.charAt(word.length() - 1) == c) {
        word = word.substring(0, word.length() - 1);
      }
    }
    return word;
  }

  @Test
  public void name2() throws IOException {
    File f = new File(OUT_FILE);
    RandomAccessInput in = new ByteArrayInput(FileUtils.readFileToByteArray(f));
    FstSet fstSet = new FstSet(in);
    fstSet.enumerate((s) -> {
      System.out.println(s);
    });
  }

  private InputIterable<Object> newInputWords(List<String> words) {
    return new InputIterable<Object>() {
      @Override
      public Iterable<InputEntry<Object>> getIterable() {
        return new Iterable<InputEntry<Object>>() {
          @Override
          public Iterator<InputEntry<Object>> iterator() {
            Iterator<String> it = words.iterator();
            return new Iterator<InputEntry<Object>>() {
              @Override
              public boolean hasNext() {
                return it.hasNext();
              }

              @Override
              public InputEntry<Object> next() {
                return new SimpleInputEntry<>(it.next(), null);
              }
            };
          }
        };
      }

      @Override
      public OutputType getOutputType() {
        return OutputType.NONE;
      }
    };
  }


}
