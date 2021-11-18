package documentTFIDF.search;

import documentTFIDF.model.DocumentData;

import java.util.*;

public class TFIDF {
    public static double calculateTermFrequency(List<String> words, String term) { // calculate term frequency for one search term in a document
        // search term count in the document / total words in the document
        long count = 0;
        for (String word : words) {
            if (term.equalsIgnoreCase(word)) {
                count++;
            }
        }
        double termFrequency = (double) count / words.size();
        return termFrequency;
    }

    public static DocumentData createDocumentData(List<String> words, List<String> terms) { // calculate all the search term frequencies for one document
        // each search term frequency for a document
        DocumentData documentData = new DocumentData();

        for (String term : terms) {
            double termFreq = calculateTermFrequency(words, term); // number of occurrences / total words
            documentData.putTermFrequency(term, termFreq);
        }

        return documentData;
    }

    private static double getInverseDocumentFrequency(String term, Map<String, DocumentData> documentResults) { // calculating IDF for a single search term
        // nt - number of documents that contain the given term
        double nt = 0;
        for (String document : documentResults.keySet()) { // go through all the documents and count how many of them contains at least one occurrence of this search term.
            DocumentData documentData = documentResults.get(document);
            double termFrequency = documentData.getFrequency(term);
            if (termFrequency > 0.0) {
                // if the document contains the search term, increment the number of documents that contain the given search term by one
                nt++;
            }
        }
        return nt == 0 ? 0 : Math.log10(documentResults.size() / nt);
    }

    private static Map<String, Double> getTermToInverseDocumentFrequencyMap(List<String> terms, Map<String, DocumentData> documentResults) { // calculate IDF for all search terms
        Map<String, Double> termToIDF = new HashMap<>();
        for (String term : terms) {
            // for each search term, calculate the weight to be applied for each search-term term frequency for a particular document
            double idf = getInverseDocumentFrequency(term, documentResults);
            termToIDF.put(term, idf);
        }
        return termToIDF;
    }

    private static double calculateDocumentScore(List<String> terms, DocumentData documentData, Map<String, Double> termToInverseDocumentFrequency) { // calculate score = TF * IDF for each search term and add it all up
        double score = 0;
        for (String term : terms) {
            double termFrequency = documentData.getFrequency(term);
            double inverseTermFrequency = termToInverseDocumentFrequency.get(term); // common for all the documents
            score += termFrequency * inverseTermFrequency;
        }
        return score;
    }

    public static Map<Double, List<String>> getDocumentsSortedByScore(List<String> terms, Map<String, DocumentData> documentResults) {
        // The map is sorted according to the natural ordering of its keys, or by a Comparator provided at map creation time, depending on which constructor is used.
        // This proves to be an efficient way of sorting and storing the key-value pairs.
        TreeMap<Double, List<String>> scoreToDocuments = new TreeMap<>();
        Map<String, Double> termToInverseDocumentFrequency = getTermToInverseDocumentFrequencyMap(terms, documentResults);

        for (String document : documentResults.keySet()) {
            DocumentData documentData = documentResults.get(document);
            double score = calculateDocumentScore(terms, documentData, termToInverseDocumentFrequency);
            addDocumentScoreToTreeMap(scoreToDocuments, score, document);
        }

        return scoreToDocuments.descendingMap();
    }

    private static void addDocumentScoreToTreeMap(TreeMap<Double, List<String>> scoreToDoc, double score, String document) {
        List<String> documentsWithCurrentScore = scoreToDoc.get(score);

        if (documentsWithCurrentScore == null) {
            documentsWithCurrentScore = new ArrayList<>();
        }

        documentsWithCurrentScore.add(document); // multiple documents with the same score
        scoreToDoc.put(score, documentsWithCurrentScore);
    }

    public static List<String> getWordsFromLine(String lines) {
        return Arrays.asList(lines.split("(\\.)+|(,)+|( )+|(-)+|(\\?)+|(!)+|(;)+|(:)+|(/d)+|(/n)+"));
    }

    public static List<String> getWordsFromLines(List<String> lines) {
        List<String> words = new ArrayList<>();
        for (String line : lines) {
            words.addAll(getWordsFromLine(line));
        }
        return words;
    }
}
