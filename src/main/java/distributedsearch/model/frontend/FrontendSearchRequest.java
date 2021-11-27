package distributedsearch.model.frontend;

public class FrontendSearchRequest {
    private String searchQuery;
    private static final long MAX_NUMBER_OF_RESULTS = Long.MAX_VALUE;
    private static final double MIN_SCORE = 0.0;

    public String getSearchQuery() {
        return searchQuery;
    }

    public long getMaxNumberOfResults() {
        return MAX_NUMBER_OF_RESULTS;
    }

    public double getMinScore() {
        return MIN_SCORE;
    }
}
