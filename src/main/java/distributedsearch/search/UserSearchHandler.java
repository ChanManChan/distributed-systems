package distributedsearch.search;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.protobuf.InvalidProtocolBufferException;
import distributedsearch.management.ServiceRegistry;
import distributedsearch.model.frontend.FrontendSearchRequest;
import distributedsearch.model.frontend.FrontendSearchResponse;
import distributedsearch.model.proto.SearchModel;
import distributedsearch.networking.OnRequestCallback;
import distributedsearch.networking.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UserSearchHandler implements OnRequestCallback {
    private static final String ENDPOINT = "/documents_search";
    private static final String DOCUMENTS_LOCATION = "books";
    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final ServiceRegistry searchCoordinatorRegistry;

    public UserSearchHandler(ServiceRegistry searchCoordinatorRegistry) {
        this.searchCoordinatorRegistry = searchCoordinatorRegistry;
        this.webClient = new WebClient();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    }

    private FrontendSearchResponse createFrontendResponse(FrontendSearchRequest frontendSearchRequest) {
        SearchModel.Response searchClusterResponse = sendRequestToSearchCluster(frontendSearchRequest.getSearchQuery());
        List<FrontendSearchResponse.SearchResultInfo> filteredResults = filterResults(searchClusterResponse, frontendSearchRequest.getMaxNumberOfResults(), frontendSearchRequest.getMinScore());
        return new FrontendSearchResponse(filteredResults, DOCUMENTS_LOCATION);
    }

    private List<FrontendSearchResponse.SearchResultInfo> filterResults(SearchModel.Response searchClusterResponse, long maxResults, double minScore) {
        double maxScore = getMaxScore(searchClusterResponse);
        List<FrontendSearchResponse.SearchResultInfo> searchResultInfoList = new ArrayList<>();

        for (int i = 0; i < searchClusterResponse.getRelevantDocumentsCount() && i < maxResults; i++) {
            int normalizedDocumentScore = normalizeScore(searchClusterResponse.getRelevantDocuments(i).getScore(), maxScore);
            if (normalizedDocumentScore < minScore) {
                continue;
            }

            String documentName = searchClusterResponse.getRelevantDocuments(i).getDocumentName();
            String title = getDocumentTitle(documentName);
            String extension = getDocumentExtension(documentName);

            FrontendSearchResponse.SearchResultInfo resultInfo = new FrontendSearchResponse.SearchResultInfo(title, extension, normalizedDocumentScore);
            searchResultInfoList.add(resultInfo);
        }

        return searchResultInfoList;
    }

    private static String getDocumentExtension(String document) {
        String[] parts = document.split("\\.");
        if (parts.length == 2) {
            return parts[1];
        }
        return "";
    }

    private static String getDocumentTitle(String document) {
        return document.split("\\.")[0];
    }

    private static int normalizeScore(double inputScore, double maxScore) {
        return (int) Math.ceil(inputScore * 100.0 / maxScore);
    }

    private static double getMaxScore(SearchModel.Response searchClusterResponse) {
        if (searchClusterResponse.getRelevantDocumentsCount() == 0) {
            return 0;
        }
        return searchClusterResponse.getRelevantDocumentsList()
                .stream()
                .map(SearchModel.Response.DocumentStats::getScore)
                .max(Double::compareTo)
                .orElse(Double.MIN_VALUE);
    }

    private SearchModel.Response sendRequestToSearchCluster(String searchQuery) {
        SearchModel.Request searchRequest = SearchModel.Request.newBuilder()
                .setSearchQuery(searchQuery)
                .build();

        try {
            String coordinatorAddress = searchCoordinatorRegistry.getRandomServiceAddress();
            if (coordinatorAddress == null) {
                System.out.println("Search cluster coordinator is unavailable");
                return SearchModel.Response.getDefaultInstance();
            }

            byte[] payloadBody = webClient.sendTask(searchRequest.toByteArray(), coordinatorAddress).join();
            return SearchModel.Response.parseFrom(payloadBody);
        } catch (InvalidProtocolBufferException | InterruptedException | KeeperException e) {
            e.printStackTrace();
            return SearchModel.Response.getDefaultInstance();
        }
    }

    @Override
    public byte[] handleRequest(byte[] requestPayload) {
        try {
            FrontendSearchRequest frontendSearchRequest = objectMapper.readValue(requestPayload, FrontendSearchRequest.class);
            FrontendSearchResponse frontendSearchResponse = createFrontendResponse(frontendSearchRequest);
            return objectMapper.writeValueAsBytes(frontendSearchResponse);
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    @Override
    public String getEndpoint() {
        return ENDPOINT;
    }
}
