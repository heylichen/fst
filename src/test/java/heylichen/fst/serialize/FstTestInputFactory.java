package heylichen.fst.serialize;

import heylichen.fst.input.DataInputIterable;
import heylichen.fst.input.InputEntry;
import heylichen.fst.input.InputIterable;
import heylichen.fst.input.SimpleInputEntry;
import heylichen.fst.output.IntOutput;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FstTestInputFactory {

  public static InputIterable<Integer> newMultipleKeyWithJumpTable() {
    List<InputEntry<Integer>> list = Arrays.asList(
        new SimpleInputEntry<>("say", new IntOutput(31)),
        new SimpleInputEntry<>("sb", new IntOutput(28)),
        new SimpleInputEntry<>("sc", new IntOutput(29)),
        new SimpleInputEntry<>("sd", new IntOutput(29)),
        new SimpleInputEntry<>("se", new IntOutput(29)),
        new SimpleInputEntry<>("sf", new IntOutput(29)),
        new SimpleInputEntry<>("sg", new IntOutput(29)),
        new SimpleInputEntry<>("sh", new IntOutput(29))
    );
    return newInput(list);
  }

  public static InputIterable<Integer> newInputForEditDistance() {
    List<InputEntry<Integer>> list = Arrays.asList(
        newEntry("their", 120),
        newEntry("thir", 130),
        newEntry("tier", 140),
        newEntry("thief", 150),
        newEntry("trier", 160)
    );
    list.sort(Comparator.comparing(InputEntry::getKey));

    return newInput(list);
  }

  private static InputEntry<Integer> newEntry(String key, Integer v) {
    return new SimpleInputEntry<>(key, new IntOutput(v));
  }

  public static InputIterable<Integer> newSingleKey() {
    List<InputEntry<Integer>> list = Collections.singletonList(
        new SimpleInputEntry<>("say", new IntOutput(31))
    );
    return newInput(list);
  }

  private static InputIterable<Integer> newInput(List<InputEntry<Integer>> list) {
    return new DataInputIterable<>(list);
  }
}
