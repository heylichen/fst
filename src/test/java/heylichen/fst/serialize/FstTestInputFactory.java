package heylichen.fst.serialize;

import heylichen.fst.input.InputEntry;
import heylichen.fst.input.InputIterable;
import heylichen.fst.input.IntDataInputIterable;
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
        newEntry("xoof", 120),
        newEntry("1xoof", 130),
        newEntry("11xoof", 140),
        newEntry("oof", 150),
        newEntry("of", 160),
        newEntry("f", 89)
    );
    list.sort(Comparator.comparing(InputEntry::getKey));
    return newInput(list);
  }

  public static InputIterable<Integer> newInputForEnum() {
    List<InputEntry<Integer>> list = Arrays.asList(
        newEntry("The", null),
        newEntry("Project", null),
        newEntry("Gutenberg", null)
    );
    list.sort(Comparator.comparing(InputEntry::getKey));
    return newInput(list);
  }

  public static InputIterable<Integer> newInputForPredictive() {
    List<InputEntry<Integer>> list = Arrays.asList(
        newEntry("predic", 153472),
        newEntry("predicti", 153473),
        newEntry("predictiv", 153474),
        newEntry("predictive", 153474),
        newEntry("predictively", 153475),
        newEntry("predictiveness", 153476)
    );
    list.sort(Comparator.comparing(InputEntry::getKey));
    return newInput(list);
  }

  public static InputIterable<Integer> newInputForSuggest() {
    List<InputEntry<Integer>> list = Arrays.asList(
        newEntry("their", 153472),
        newEntry("thir", 153473),
        newEntry("tier", 153474),
        newEntry("thief", 153474),
        newEntry("trier", 153475),
        newEntry("predic", 153472),
        newEntry("predictiveness", 153476)
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
    return new IntDataInputIterable(list);
  }
}
