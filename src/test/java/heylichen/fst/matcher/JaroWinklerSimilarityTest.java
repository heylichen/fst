package heylichen.fst.matcher;

import org.junit.Test;

public class JaroWinklerSimilarityTest {

  @Test
  public void name() {
    printSim("thier", "thir");
  }

  private void printSim(String a, String b) {
    JaroWinklerSimilarity js = new JaroWinklerSimilarity();
    System.out.println(js.getJaroSim(a, b));
    System.out.println(js.getJaroWinklerSim(a, b));
  }
}