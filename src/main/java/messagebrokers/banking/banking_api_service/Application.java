package messagebrokers.banking.banking_api_service;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class Application {
    private static final String SUSPICIOUS_TRANSACTIONS_TOPIC = "suspicious-transactions";
    private static final String VALID_TRANSACTIONS_TOPIC = "valid-transactions";
    private static final String BOOTSTRAP_SERVERS = "[::1]:9092, [::1]:9093, [::1]:9094";

    public static void main(String[] args) {
        Producer<String, Transaction> kafkaProducer = createKafkaProducer(BOOTSTRAP_SERVERS);
        try {
            processTransactions(new IncomingTransactionsReader(), new UserResidenceDatabase(), kafkaProducer);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            kafkaProducer.flush();
            kafkaProducer.close();
        }
    }

    public static void processTransactions(IncomingTransactionsReader incomingTransactionsReader, UserResidenceDatabase userResidenceDatabase, Producer<String, Transaction> kafkaProducer) throws ExecutionException, InterruptedException {
        while (incomingTransactionsReader.hasNext()) {
            Transaction transaction = incomingTransactionsReader.next();
            String userResidence = userResidenceDatabase.getUserResidence(transaction.getUser());
            if (transaction.getTransactionLocation().equals(userResidence)) {
                // valid
                ProducerRecord<String, Transaction> validTransaction = new ProducerRecord<>(VALID_TRANSACTIONS_TOPIC, transaction);
                RecordMetadata recordMetadata = kafkaProducer.send(validTransaction).get();
                System.out.printf("Record with (key: %s, value: %s), was sent to (partition: %d, offset: %d)%n", validTransaction.key(), validTransaction.value(), recordMetadata.partition(), recordMetadata.offset());
            } else {
                // suspicious
                ProducerRecord<String, Transaction> suspiciousTransaction = new ProducerRecord<>(SUSPICIOUS_TRANSACTIONS_TOPIC, transaction);
                RecordMetadata recordMetadata = kafkaProducer.send(suspiciousTransaction).get();
                System.out.printf("Record with (key: %s, value: %s), was sent to (partition: %d, offset: %d)%n", suspiciousTransaction.key(), suspiciousTransaction.value(), recordMetadata.partition(), recordMetadata.offset());
            }
        }
    }

    public static Producer<String, Transaction> createKafkaProducer(String bootstrapServers) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, "banking-api-service");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, Transaction.TransactionSerializer.class.getName());
        return new KafkaProducer<>(properties);
    }
}
