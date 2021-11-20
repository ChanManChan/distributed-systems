package distributed.search;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Result implements Serializable {
    private final Map<String, DocumentData> documentToDocumentData = new HashMap<>();

    public void addDocumentData(String document, DocumentData documentData) {
        this.documentToDocumentData.put(document, documentData);
    }

    public Map<String, DocumentData> getDocumentToDocumentData() {
        return Collections.unmodifiableMap(documentToDocumentData);
    }
}
