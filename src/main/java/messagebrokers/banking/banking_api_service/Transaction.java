package messagebrokers.banking.banking_api_service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;

public class Transaction {
    private String user;
    private double amount;
    private String transactionLocation;

    public Transaction(String user, double amount, String transactionLocation) {
        this.user = user;
        this.amount = amount;
        this.transactionLocation = transactionLocation;
    }

    public String getUser() {
        return user;
    }

    public double getAmount() {
        return amount;
    }

    public String getTransactionLocation() {
        return transactionLocation;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "user='" + user + '\'' +
                ", amount=" + amount +
                ", transactionLocation='" + transactionLocation + '\'' +
                '}';
    }

    public static class TransactionSerializer implements Serializer<Transaction> {
        @Override
        public byte[] serialize(String topic, Transaction data) {
            byte[] serializedData = null;
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                serializedData = objectMapper.writeValueAsString(data).getBytes();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return serializedData;
        }
    }
}
