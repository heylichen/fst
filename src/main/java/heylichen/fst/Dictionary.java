package heylichen.fst;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class Dictionary<O> {
  public static final int BUCKET_COUNT = 10000;
  private List<CacheEntry<O>> entryList = new ArrayList<>(BUCKET_COUNT);

  public int getBucketId(long key) {
    return (int) (key % BUCKET_COUNT);
  }

  public State<O> get(long key, State<O> state) {
    int bucketId = getBucketId(key);
    CacheEntry<O> entry = entryList.get(bucketId);
    if (state.equals(entry.first)) {
      return entry.first;
    }
    if (state.equals(entry.second)) {
      State<O> tmp = entry.second;
      entry.second = entry.first;
      entry.first = tmp;
      return tmp;
    }
    if (state.equals(entry.third)) {
      State<O> tmp = entry.third;
      entry.third = entry.first;
      entry.first = tmp;
      return tmp;
    }
    return null;
  }

  public void put(long key, State<O> state) {
    int bucketId = getBucketId(key);
    CacheEntry<O> entry = entryList.get(bucketId);
    if (entry.getThird() != null) {
      //evict cache item
      entry.setThird(null);
    }
    entry.setThird(entry.getSecond());
    entry.setSecond(entry.getFirst());
    entry.setFirst(state);
  }

  public Pair<Boolean,State<O>> findMinimized(State<O> state) {
    long key = state.hash();

    State<O> got = get(key, state);
    if (got != null) {
      return Pair.of(true, got);
    }
    put(key, state);
    return Pair.of(true, state);
  }

  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  private static class CacheEntry<O> {
    //latest accessed stored here
    private State<O> first;
    private State<O> second;
    private State<O> third;
  }
}
