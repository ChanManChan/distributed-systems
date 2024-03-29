package messagebrokers.banking.user_notification_service;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class Application {
    private static final String SUSPICIOUS_TRANSACTIONS_TOPIC = "suspicious-transactions";
    private static final String BOOTSTRAP_SERVERS = "[::1]:9092, [::1]:9093, [::1]:9094";

    public static void main(String[] args) {
        String consumerGroup = args.length == 1 ? args[0] : "user-notification-service";
        System.out.println("Consumer is part of consumer group " + consumerGroup);
        Consumer<String, Transaction> kafkaConsumer = createKafkaConsumer(BOOTSTRAP_SERVERS, consumerGroup);
        consumeMessages(SUSPICIOUS_TRANSACTIONS_TOPIC, kafkaConsumer);
    }

    public static void consumeMessages(String topic, Consumer<String, Transaction> kafkaConsumer) {
        kafkaConsumer.subscribe(Collections.singletonList(topic));
        while (true) {
            ConsumerRecords<String, Transaction> consumerRecords = kafkaConsumer.poll(Duration.ofSeconds(1));
            if (consumerRecords.isEmpty()) {
                // do something else
            } else {
                for (ConsumerRecord<String, Transaction> consumerRecord : consumerRecords) {
                    sendUserNotification(consumerRecord.value());
                }
                kafkaConsumer.commitAsync();
            }
        }
    }

    public static Consumer<String, Transaction> createKafkaConsumer(String bootstrapServers, String consumerGroup) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, Transaction.TransactionDeserializer.class.getName());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new KafkaConsumer<>(properties);
    }

    private static void sendUserNotification(Transaction transaction) {
        System.out.printf("Sending user %s notification about a suspicious transaction of $%.2f in their account originating in %s%n", transaction.getUser(), transaction.getAmount(), transaction.getTransactionLocation());
    }
}
