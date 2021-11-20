package distributed.search;

import java.util.Collections;
import java.util.List;

public class Task {
    private final List<String> searchTerms;
    private final List<String> documents;

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
