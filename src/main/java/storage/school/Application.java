package storage.school;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import static com.mongodb.client.model.Filters.eq;

public class Application {
    private static final String MONGO_DB_URL = "mongodb://127.0.0.1:27018,127.0.0.1:27019,127.0.0.1:27020/?replicaSet=rs0";
    private static final String DB_NAME = "online-school";
    private static final double MIN_GPA = 90.0;

    public static void main(String[] args) {
        // java -jar .\online_school-jar-with-dependencies.jar physics Nandu 26 90.5
        String courseName = args[0];
        String studentName = args[1];
        int age = Integer.parseInt(args[2]);
        double gpa = Double.parseDouble(args[3]);

        MongoDatabase onlineSchoolDB = connectToMongoDB(MONGO_DB_URL, DB_NAME);
        enroll(onlineSchoolDB, courseName, studentName, age, gpa);
    }

    private static void enroll(MongoDatabase onlineSchoolDB, String courseName, String studentName, int age, double gpa) {
        if (!isValidCourse(onlineSchoolDB, courseName)) {
            System.out.println("Invalid course " + courseName);
            return;
        }

        MongoCollection<Document> courseCollection = onlineSchoolDB.getCollection(courseName)
                .withWriteConcern(WriteConcern.MAJORITY) // since we want to make sure any update to our course collection is fully replicated to a majority of our cluster. To make sure we don't lose any enrolment data.
                .withReadPreference(ReadPreference.primaryPreferred()); // since we are going to read from the course collection right after we add a new student, and we want to guarantee strict consistency in our reads.

        if (courseCollection.find(eq("name", studentName)).first() != null) {
            System.out.println("Student " + studentName + " already enrolled");
            return;
        }

        if (gpa < MIN_GPA) {
            System.out.println("Please improve your grades");
            return;
        }

        courseCollection.insertOne(new Document("name", studentName).append("age", age).append("gpa", gpa));
        System.out.println("Student " + studentName + " was successfully enrolled in " + courseName);

        for (Document document : courseCollection.find()) {
            System.out.println(document);
        }
    }

    private static boolean isValidCourse(MongoDatabase database, String course) {
        for (String collectionName : database.listCollectionNames()) {
            if (collectionName.equals(course)) {
                return true;
            }
        }
        return false;
    }

    public static MongoDatabase connectToMongoDB(String url, String dbName) {
        MongoClient mongoClient = new MongoClient(new MongoClientURI(url));
        return mongoClient.getDatabase(dbName);
    }
}
