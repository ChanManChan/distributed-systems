package distributedsearch.search;

import com.google.protobuf.InvalidProtocolBufferException;
import distributedsearch.management.ServiceRegistry;
import distributedsearch.model.DocumentData;
import distributedsearch.model.Result;
import distributedsearch.model.SerializationUtils;
import distributedsearch.model.Task;
import distributedsearch.model.proto.SearchModel;
import distributedsearch.networking.OnRequestCallback;
import distributedsearch.networking.WebClient;
import org.apache.zookeeper.KeeperException;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static distributedsearch.search.TFIDF.getDocumentScores;
import static distributedsearch.search.TFIDF.getWordsFromLine;

public class SearchCoordinator implements OnRequestCallback {
    private static final String ENDPOINT = "/search";
    private static final String BOOKS_DIRECTORY = "D:\\Spring\\distributed-systems\\src\\main\\resources\\books";
    private final ServiceRegistry workersServiceRegistry;
    private final WebClient client;
    private final List<String> documents;

    public SearchCoordinator(ServiceRegistry workersServiceRegistry, WebClient client) {
        this.workersServiceRegistry = workersServiceRegistry;
        this.client = client;
        this.documents = readDocumentsList();
    }

    /**
     * The tasks are asynchronously sent to all the currently available workers
     */
    private List<Result> sendTasksToWorkers(List<String> workers, List<Task> tasks) {
        CompletableFuture<Result>[] futures = new CompletableFuture[workers.size()];
        for (int i = 0; i < workers.size(); i++) {
            String worker = workers.get(i);
            Task task = tasks.get(i);
            byte[] payload = SerializationUtils.serialize(task);
            futures[i] = client.sendTask(worker, payload);
        }

        List<Result> results = new ArrayList<>();
        for (CompletableFuture<Result> future : futures) {
            try {
                Result result = future.get();
                results.add(result);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        System.out.println(String.format("Received %d/%d results", results.size(), tasks.size()));
        return results;
    }

    /**
     * Creates a list of tasks where each task will be sent to a different search worker
     */
    public List<Task> createTasks(int numberOfWorkers, List<String> searchTerms) {
        List<List<String>> workerDocuments = splitDocumentList(numberOfWorkers, documents);
        List<Task> tasks = new ArrayList<>();
        for (List<String> documentsForWorker : workerDocuments) {
            Task task = new Task(searchTerms, documentsForWorker);
            tasks.add(task);
        }
        return tasks;
    }

    /**
     * This method divides all the documents in a fair way among all the workers, so we get as many list of documents as we have workers
     * Each such list of documents can later be sent to a different worker node
     *
     * @param numberOfWorkers number of search workers we currently have in the cluster
     * @param documents       list of documents we currently have in the repository
     */
    private static List<List<String>> splitDocumentList(int numberOfWorkers, List<String> documents) {
        int numberOfDocumentsPerWorker = (documents.size() + numberOfWorkers - 1) / numberOfWorkers;
        List<List<String>> workersDocuments = new ArrayList<>();

        for (int i = 0; i < numberOfWorkers; i++) {
            int firstDocumentIndex = i * numberOfDocumentsPerWorker;
            int lastDocumentIndexExclusive = Math.min(firstDocumentIndex + numberOfDocumentsPerWorker, documents.size());

            if (firstDocumentIndex >= lastDocumentIndexExclusive) {
                break;
            }

            List<String> currentWorkerDocuments = new ArrayList<>(documents.subList(firstDocumentIndex, lastDocumentIndexExclusive));
            workersDocuments.add(currentWorkerDocuments);
        }
        return workersDocuments;
    }

    private static List<String> readDocumentsList() {
        File documentsDirectory = new File(BOOKS_DIRECTORY);
        String[] booksList = documentsDirectory.list();
        return booksList == null ? Collections.emptyList() : Arrays.stream(booksList)
                .map(documentName -> BOOKS_DIRECTORY + "/" + documentName)
                .collect(Collectors.toList());
    }

    private SearchModel.Response createResponse(SearchModel.Request searchRequest) throws InterruptedException, KeeperException {
        SearchModel.Response.Builder searchResponse = SearchModel.Response.newBuilder();
        String searchQuery = searchRequest.getSearchQuery();
        System.out.println("Received search query: " + searchQuery);
        List<String> searchTerms = getWordsFromLine(searchQuery);
        List<String> workers = workersServiceRegistry.getAllServiceAddresses();

        if (workers.isEmpty()) {
            System.out.println("No search workers currently available");
            return searchResponse.build();
        }

        List<Task> tasks = createTasks(workers.size(), searchTerms);
        List<Result> results = sendTasksToWorkers(workers, tasks);
        List<SearchModel.Response.DocumentStats> sortedDocuments = aggregateResults(results, searchTerms);
        searchResponse.addAllRelevantDocuments(sortedDocuments);
        return searchResponse.build();
    }

    private List<SearchModel.Response.DocumentStats> aggregateResults(List<Result> results, List<String> terms) {
        Map<String, DocumentData> allDocumentsResults = new HashMap<>();

        for (Result result : results) {
            allDocumentsResults.putAll(result.getDocumentToDocumentData());
        }

        System.out.println("Calculating score for all the documents");
        Map<Double, List<String>> scoreToDocuments = getDocumentScores(terms, allDocumentsResults);
        return sortDocumentsByScore(scoreToDocuments);
    }

    private List<SearchModel.Response.DocumentStats> sortDocumentsByScore(Map<Double, List<String>> scoreToDocuments) {
        List<SearchModel.Response.DocumentStats> sortedDocumentStatsList = new ArrayList<>();

        for (Map.Entry<Double, List<String>> docScorePair : scoreToDocuments.entrySet()) {
            double score = docScorePair.getKey();

            for (String document : docScorePair.getValue()) {
                File documentPath = new File(document);

                SearchModel.Response.DocumentStats documentStats = SearchModel.Response.DocumentStats.newBuilder()
                        .setScore(score)
                        .setDocumentName(documentPath.getName())
                        .setDocumentSize(documentPath.length())
                        .build();
                sortedDocumentStatsList.add(documentStats);
            }
        }
        return sortedDocumentStatsList;
    }

    @Override
    public byte[] handleRequest(byte[] requestPayload) {
        try {
            SearchModel.Request request = SearchModel.Request.parseFrom(requestPayload);
            SearchModel.Response response = createResponse(request);
            return response.toByteArray();
        } catch (InvalidProtocolBufferException | InterruptedException | KeeperException e) {
            e.printStackTrace();
            return SearchModel.Response.getDefaultInstance().toByteArray();
        }
    }

    @Override
    public String getEndpoint() {
        return ENDPOINT;
    }
}
