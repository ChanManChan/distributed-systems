package messagebrokers.kafka;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class ConsumerApplication {
    private static final String TOPIC = "events";
    private static final String BOOTSTRAP_SERVERS = "[::1]:9092,[::1]:9093,[::1]:9094";

    public static void main(String[] args) {
        String consumerGroup = args.length == 1 ? args[0] : "defaultConsumerGroup";
        System.out.println("Consumer is part of consumer group " + consumerGroup);
        Consumer<Long, String> kafkaConsumer = createKafkaConsumer(BOOTSTRAP_SERVERS, consumerGroup);
        consumeMessages(TOPIC, kafkaConsumer);
    }

    public static void consumeMessages(String topic, Consumer<Long, String> kafkaConsumer) {
        kafkaConsumer.subscribe(Collections.singletonList(topic));

        while (true) {
            ConsumerRecords<Long, String> consumerRecords = kafkaConsumer.poll(Duration.ofSeconds(1));
            if (consumerRecords.isEmpty()) {
                // do something else
            }
            for (ConsumerRecord<Long, String> consumerRecord : consumerRecords) {
                System.out.printf("Received record (key: %d, value: %s, partition: %d, offset: %d)%n", consumerRecord.key(), consumerRecord.value(), consumerRecord.partition(), consumerRecord.offset());
            }

            // do something with the records
            // only after the processing of the messages is complete, we call the commitAsync() method which tells kafka that our consumer successfully consumed those messages
            kafkaConsumer.commitAsync();
        }
    }

    public static Consumer<Long, String> createKafkaConsumer(String bootstrapServers, String consumerGroup) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);
        // Disable auto commit of the message because the consumer may consume the message, but before it does anything with it the consumer may crash.
        // In that case we would like that message to be reread again.
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new KafkaConsumer<>(properties);
    }
}
