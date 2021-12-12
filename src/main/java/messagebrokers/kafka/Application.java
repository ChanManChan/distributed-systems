package messagebrokers.kafka;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class Application {
    private static final String TOPIC = "events";
    private static final String BOOTSTRAP_SERVERS = "localhost:9092, localhost:9093, localhost:9094";

    public static void main(String[] args) {
        Producer<Long, String> kafkaProducer = createKafkaProducer(BOOTSTRAP_SERVERS);
        try {
            produceMessages(10, kafkaProducer);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } finally {
            kafkaProducer.flush();
            kafkaProducer.close();
        }
    }

    public static void produceMessages(int numberOfMessages, Producer<Long, String> kafkaProducer) throws ExecutionException, InterruptedException {
        int partition = 0;
        for (int i = 0; i < numberOfMessages; i++) {
            long key = i;
            String value = String.format("event %d", i);
            long timeStamp = System.currentTimeMillis();
            ProducerRecord<Long, String> producerRecord = new ProducerRecord<>(TOPIC, partition, timeStamp, key, value);
            RecordMetadata recordMetadata = kafkaProducer.send(producerRecord).get();
            System.out.printf("Record with (key: %s, value: %s), was sent to (partition: %d, offset: %d)%n", producerRecord.key(), producerRecord.value(), recordMetadata.partition(), recordMetadata.offset());
        }
    }

    public static Producer<Long, String> createKafkaProducer(String bootstrapServers) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, "events-producer");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(properties);
    }
}
