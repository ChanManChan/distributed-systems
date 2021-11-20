package distributed.search;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Result implements Serializable {
    // mapping between every document allocated to our worker node to the DocumentData object that was calculated by the TFIDF algorithm
    private final Map<String, DocumentData> documentToDocumentData = new HashMap<>();

    public void addDocumentData(String document, DocumentData documentData) {
        this.documentToDocumentData.put(document, documentData);
    }

    public Map<String, DocumentData> getDocumentToDocumentData() {
        return Collections.unmodifiableMap(documentToDocumentData);
    }
}
