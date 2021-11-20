package distributed.search;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

// Represents the mapping between each given search term to its term frequency inside a given document
// We will have to send instances of this class for each document allocated to the worker, so we need to make it serializable
public class DocumentData implements Serializable {
    private final Map<String, Double> termToFrequency = new HashMap<>();

    public void putTermFrequency(String term, double frequency) {
        termToFrequency.put(term, frequency);
    }

    public double getFrequency(String term) {
        return termToFrequency.get(term);
    }
}
