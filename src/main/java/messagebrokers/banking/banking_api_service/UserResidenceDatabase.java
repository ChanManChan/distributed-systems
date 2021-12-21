package messagebrokers.banking.banking_api_service;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class UserResidenceDatabase {
    private static final String USER_RESIDENCE_FILE = "banking/user-residence.txt";
    private final Map<String, String> userToResidenceMap;

    public UserResidenceDatabase() {
        this.userToResidenceMap = loadUsersResidenceFromFile();
    }

    public String getUserResidence(String user) {
        if (!userToResidenceMap.containsKey(user)) {
            throw new RuntimeException("user " + user + " doesn't exist");
        }
        return userToResidenceMap.get(user);
    }

    private Map<String, String> loadUsersResidenceFromFile() {
        Map<String, String> userToResidence = new HashMap<>();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(USER_RESIDENCE_FILE);
        Scanner scanner = new Scanner(inputStream);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] userResidencePair = line.split(" ");
            userToResidence.put(userResidencePair[0], userResidencePair[1]);
        }
        return Collections.unmodifiableMap(userToResidence);
    }
}
