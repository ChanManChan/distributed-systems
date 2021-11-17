package documentTFIDF.search;

import documentTFIDF.model.DocumentData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TFIDF {
    public static double calculateTermFrequency(List<String> words, String term) {
        long count = 0;
        for (String word : words) {
            if (term.equalsIgnoreCase(word)) {
                count++;
            }
        }
        double termFrequency = (double) count / words.size();
        return termFrequency;
    }

    public static DocumentData createDocumentData(List<String> words, List<String> terms) {
        DocumentData documentData = new DocumentData();

        for (String term : terms) {
            double termFreq = calculateTermFrequency(words, term);
            documentData.putTermFrequency(term, termFreq);
        }

        return documentData;
    }

    private static double getInverseDocumentFrequency(String term, Map<String, DocumentData> documentResults) {
        // nt - number of documents that contain the given term
        double nt = 0;
        for (String document : documentResults.keySet()) {
            DocumentData documentData = documentResults.get(document);
            double termFrequency = documentData.getFrequency(term);
            if (termFrequency > 0.0) {
                nt++;
            }
        }
        return nt == 0 ? 0 : Math.log10(documentResults.size() / nt);
    }

    private static Map<String, Double> getTermToInverseDocumentFrequencyMap(List<String> terms, Map<String, DocumentData> documentResults) {
        Map<String, Double> termToIDF = new HashMap<>();
        for (String term : terms) {
            double idf = getInverseDocumentFrequency(term, documentResults);
            termToIDF.put(term, idf);
        }
        return termToIDF;
    }

    private static double calculateDocumentScore(List<String> terms, DocumentData documentData, Map<String, Double> termToInverseDocumentFrequency) {
        double score = 0;
        for (String term : terms) {
            double termFrequency = documentData.getFrequency(term);
            double inverseTermFrequency = termToInverseDocumentFrequency.get(term);
            score += termFrequency * inverseTermFrequency;
        }
        return score;
    }

    
}
