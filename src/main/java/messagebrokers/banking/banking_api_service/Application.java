package messagebrokers.banking.banking_api_service;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Properties;

public class Application {
    private static final String SUSPICIOUS_TRANSACTIONS_TOPIC = "suspicious-transactions";
    private static final String VALID_TRANSACTIONS_TOPIC = "valid-transactions";
    private static final String BOOTSTRAP_SERVERS = "[::1]:9092, [::1]:9093, [::1]:9094";

    public static void processTransactions(IncomingTransactionsReader incomingTransactionsReader, UserResidenceDatabase userResidenceDatabase, Producer<String, Transaction> kafkaProducer) {
        while (incomingTransactionsReader.hasNext()) {
            Transaction transaction = incomingTransactionsReader.next();
        }
    }

    public static Producer<String, Transaction> createKafkaProducer(String bootstrapServers) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, "banking-api-service");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, Transaction.TransactionSerializer.class.getName());
        return new KafkaProducer<>(properties);
    }
}
