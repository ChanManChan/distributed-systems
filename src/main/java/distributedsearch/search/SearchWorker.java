package distributedsearch.search;

import distributedsearch.model.DocumentData;
import distributedsearch.model.Result;
import distributedsearch.model.SerializationUtils;
import distributedsearch.model.Task;
import distributedsearch.networking.OnRequestCallback;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static distributedsearch.search.TFIDF.getWordsFromDocument;
import static distributedsearch.search.TFIDF.populateTermToTermFrequencyMap;

// Worker node just goes through the given subset of documents that was given to it and calculates the term frequency for each search term within a particular document
// The calculated Term to Term Frequency map for each document in the subset is then transferred to the search coordinator where further calculations happen.
public class SearchWorker implements OnRequestCallback {
    private static final String ENDPOINT = "/task";

    private Result createResult(Task task) {
        List<String> documents = task.getDocuments();
        System.out.println(String.format("Received %d documents to process", documents.size()));
        Result result = new Result();

        for (String document : documents) {
            List<String> words = parseWordsFromDocument(document);
            DocumentData documentData = populateTermToTermFrequencyMap(words, task.getSearchTerms());
            result.addDocumentData(document, documentData);
        }
        return result;
    }

    private List<String> parseWordsFromDocument(String document) {
        try (FileReader fileReader = new FileReader(document);
             BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            List<String> lines = bufferedReader.lines().collect(Collectors.toList());
            return getWordsFromDocument(lines);
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public byte[] handleRequest(byte[] requestPayload) {
        // will always get request from the search coordinator
        Task task = (Task) SerializationUtils.deserialize(requestPayload);
        if (task != null) {
            Result result = createResult(task);
            return SerializationUtils.serialize(result);
        }
        return new byte[]{};
    }

    @Override
    public String getEndpoint() {
        return ENDPOINT;
    }
}
