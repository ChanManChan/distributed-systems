package documentTFIDF.search;

import documentTFIDF.model.DocumentData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SequentialSearch {
    public static final String BOOKS_DIRECTORY = "src/main/resources/books";
    public static final String SEARCH_QUERY_1 = "The best detective that catches many criminals using his deductive methods";
    public static final String SEARCH_QUERY_2 = "The girl that falls through a rabbit hole into a fantasy wonderland";
    public static final String SEARCH_QUERY_3 = "A war between Russia and France in the cold winter";

    public static void main(String[] args) throws FileNotFoundException {
        File documentsDirectory = new File(BOOKS_DIRECTORY);

        List<String> documents = Arrays.asList(documentsDirectory.list()) // list all files or directories within this path
                .stream()
                .map(documentName -> BOOKS_DIRECTORY + "/" + documentName)
                .collect(Collectors.toList()); // list of book paths from content root

        List<String> terms = TFIDF.getWordsFromLine(SEARCH_QUERY_3);
        findMostRelevantDocuments(documents, terms);
    }

    private static void findMostRelevantDocuments(List<String> documents, List<String> terms) throws FileNotFoundException {
        Map<String, DocumentData> documentsResults = new HashMap<>(); // search term frequencies for each document

        for (String document : documents) {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(document));
            List<String> lines = bufferedReader.lines().collect(Collectors.toList());
            List<String> words = TFIDF.getWordsFromLines(lines); // all the words in one document
            DocumentData documentData = TFIDF.createDocumentData(words, terms); // search term frequency
            documentsResults.put(document, documentData);
        }

        Map<Double, List<String>> documentsSortedByScore = TFIDF.getDocumentsSortedByScore(terms, documentsResults);
        printResults(documentsSortedByScore);
    }

    private static void printResults(Map<Double, List<String>> documentsSortedByScore) {
        for (Map.Entry<Double, List<String>> docScorePair : documentsSortedByScore.entrySet()) {
            double score = docScorePair.getKey();
            for (String document : docScorePair.getValue()) {
                System.out.println(String.format("Book: %s - score: %f", document.split("/")[4], score));
            }
        }
    }
}
