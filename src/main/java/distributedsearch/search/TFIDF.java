package distributedsearch.search;

import distributedsearch.model.DocumentData;

import java.util.*;

public class TFIDF {
    // term frequency calculation for a search term in a document
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

    // inverse term frequency calculation for a search term
    private static double getInverseDocumentFrequency(String term, Map<String, DocumentData> documentResults) {
        double n = 0;
        for (String document : documentResults.keySet()) {
            // go through all the documents to find how many of them contains this particular search term at least once
            DocumentData documentData = documentResults.get(document);
            double termFrequency = documentData.getFrequency(term);
            if (termFrequency > 0.0) {
                n++;
            }
        }
        // calculating the weight for a search term
        return n == 0 ? 0 : Math.log10(documentResults.size() / n); // log10(Number of documents / Number of documents that contains the term)
    }

    // term to IDF mapping
    private static Map<String, Double> getTermToInverseDocumentFrequencyMap(List<String> terms, Map<String, DocumentData> documentResults) {
        Map<String, Double> termToIDF = new HashMap<>();
        for (String term : terms) {
            double idf = getInverseDocumentFrequency(term, documentResults);
            termToIDF.put(term, idf);
        }
        return termToIDF;
    }

    // term to TF mapping for a document
    public static DocumentData populateTermToTermFrequencyMap(List<String> words, List<String> terms) {
        DocumentData documentData = new DocumentData();
        for (String term : terms) {
            double termFrequency = calculateTermFrequency(words, term.toLowerCase());
            documentData.putTermFrequency(term, termFrequency);
        }
        return documentData;
    }

    // documents ordered based on scores ------------------------
    public static Map<Double, List<String>> getDocumentScores(List<String> terms, Map<String, DocumentData> documentResults) {
        TreeMap<Double, List<String>> scoreToDoc = new TreeMap<>();
        Map<String, Double> termToInverseDocumentFrequency = getTermToInverseDocumentFrequencyMap(terms, documentResults);

        for (String document : documentResults.keySet()) {
            DocumentData documentData = documentResults.get(document);
            double score = calculateDocumentScore(terms, documentData, termToInverseDocumentFrequency);
            addDocumentScoreToTreeMap(scoreToDoc, score, document);
        }
        return scoreToDoc.descendingMap();
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

    private static void addDocumentScoreToTreeMap(TreeMap<Double, List<String>> scoreToDoc, double score, String document) {
        List<String> booksWithCurrentScore = scoreToDoc.get(score); // if already other documents are mapped under the same score

        if (booksWithCurrentScore == null) {
            booksWithCurrentScore = new ArrayList<>();
        }

        booksWithCurrentScore.add(document);
        scoreToDoc.put(score, booksWithCurrentScore); // multiple documents with the same score is a possibility
    }

    // util methods ------------------------
    public static List<String> getWordsFromDocument(List<String> lines) {
        List<String> words = new ArrayList<>();
        for (String line : lines) {
            words.addAll(getWordsFromLine(line));
        }
        return words;
    }

    public static List<String> getWordsFromLine(String line) {
        return Arrays.asList(line.split("(\\.)+|(,)+|( )+|(-)+|(\\?)+|(!)+|(;)+|(:)+|(/d)+|(/n)"));
    }
}
