package heylichen.fst.matcher;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author lichen
 * @date 2022-9-6 20:12
 */
@Getter
@Setter
@NoArgsConstructor
public class Suggestion<O> {
    private String key;
    private O value;
    private double score;

    public Suggestion(String key, O value) {
        this.key = key;
        this.value = value;
    }
}
