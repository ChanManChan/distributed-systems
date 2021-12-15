package messagebrokers.banking.account_manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;

public class Transaction {
    private String user;
    private double amount;
    private String transactionLocation;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getTransactionLocation() {
        return transactionLocation;
    }

    public void setTransactionLocation(String transactionLocation) {
        this.transactionLocation = transactionLocation;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "user='" + user + '\'' +
                ", amount=" + amount +
                ", transactionLocation='" + transactionLocation + '\'' +
                '}';
    }

    /**
     * Kafka Deserializer implementation.
     * Deserializes a Transaction from JSON to a {@link Transaction} object
     */
    public static class TransactionDeserializer implements Deserializer<Transaction> {
        @Override
        public Transaction deserialize(String topic, byte[] data) {
            ObjectMapper mapper = new ObjectMapper();
            Transaction transaction = null;
            try {
                transaction = mapper.readValue(data, Transaction.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return transaction;
        }
    }
}
