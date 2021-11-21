package distributedsearch.model.frontend;

import java.util.List;

public class FrontendSearchResponse {
    private final List<SearchResultInfo> searchResults;
    private String documentsLocation = "";

    public FrontendSearchResponse(List<SearchResultInfo> searchResults, String documentsLocation) {
        this.searchResults = searchResults;
        this.documentsLocation = documentsLocation;
    }

    public List<SearchResultInfo> getSearchResults() {
        return searchResults;
    }

    public String getDocumentsLocation() {
        return documentsLocation;
    }

    public static class SearchResultInfo {
        private final String title;
        private final String extension;
        private final int score;

        public SearchResultInfo(String title, String extension, int score) {
            this.title = title;
            this.extension = extension;
            this.score = score;
        }

        public String getTitle() {
            return title;
        }

        public String getExtension() {
            return extension;
        }

        public int getScore() {
            return score;
        }
    }
}