package storage.vod;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MoviesGenerator {
    private static final String MONGO_DB_URL = "mongodb://127.0.0.1:27023";
    private static final String DB_NAME = "videodb";
    private static final String COLLECTION_NAME = "movies";
    private static final Random random = new Random();

    public static void main(String[] args) {
        MongoDatabase database = connectToMongoDB(MONGO_DB_URL, DB_NAME);
        generateMovies(10000, database, COLLECTION_NAME);
    }

    private static MongoDatabase connectToMongoDB(String url, String dbName) {
        // connects to the sharded MongoDB cluster through mongos
        MongoClient mongoClient = new MongoClient(new MongoClientURI(url));
        return mongoClient.getDatabase(dbName);
    }

    private static void generateMovies(int numberOfMovies, MongoDatabase database, String collectionName) {
        MongoCollection<Document> collection = database.getCollection(collectionName);

        List<Document> documents = new ArrayList<>();
        System.out.println("Generating " + numberOfMovies + " movies");

        for (int movieIndex = 0; movieIndex < numberOfMovies; movieIndex++) {
            Document document = new Document();
            document.append("name", generateName())
                    .append("directors", generateDirectorNames())
                    .append("rating", generateRating())
                    .append("year", generateYear())
                    .append("cast", generateCast());
            documents.add(document);
        }

        collection.insertMany(documents);
        System.out.println("Finished generating movies");
    }

    private static String generateName() {
        StringBuilder name = new StringBuilder();
        name.append(RandomStringUtils.randomAlphabetic(1).toUpperCase());
        name.append(RandomStringUtils.randomAlphabetic(5, 10).toLowerCase());
        return name.toString();
    }

    private static List<String> generateCast() {
        int numberOfActors = random.nextInt(20) + 10;
        List<String> actors = new ArrayList<>(numberOfActors);

        for (int i = 0; i < numberOfActors; i++) {
            String firstName = generateName();
            String lastName = generateName();
            actors.add(firstName + " " + lastName);
        }

        return actors;
    }

    private static float generateRating() {
        return random.nextFloat() * 10.0f;
    }

    private static int generateYear() {
        return random.nextInt(119) + 1900;
    }

    private static List<String> generateDirectorNames() {
        int numberOfDirectors = random.nextInt(3) + 1;
        List<String> directors = new ArrayList<>(numberOfDirectors);

        for (int i = 0; i < numberOfDirectors; i++) {
            String firstName = generateName();
            String lastName = generateName();
            directors.add(firstName + " " + lastName);
        }

        return directors;
    }
}
