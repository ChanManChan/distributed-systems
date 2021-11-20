package distributed.search;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

// Represents the search task sent to the worker node from the search cluster coordinator
public class Task implements Serializable {
    private final List<String> searchTerms;
    private final List<String> documents; // subset of documents allocated to this particular worker node

    public Task(List<String> searchTerms, List<String> documents) {
        this.searchTerms = searchTerms;
        this.documents = documents;
    }

    public List<String> getSearchTerms() {
        return Collections.unmodifiableList(searchTerms);
    }

    public List<String> getDocuments() {
        return Collections.unmodifiableList(documents);
    }
}
