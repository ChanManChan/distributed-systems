package messagebrokers.kafka;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class ProducerApplication {
    private static final String TOPIC = "events";
    // If you're running your Apache Kafka brokers using WSL2 and want to connect Java service to your brokers, you may run into some errors due to misconfigurations.
    // For instance, trying to connect to "localhost:9092" or "127.0.0.1:9092" will not work.
    // Solution - IPv6
    // Use IPv6 loopback address in your broker (server.properties)
    // listeners=PLAINTEXT://[::1]:9092
    // if it still doesn't work try restarting wsl
    // wsl -l -v
    // wsl --shutdown
    private static final String BOOTSTRAP_SERVERS = "[::1]:9092,[::1]:9093,[::1]:9094";

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
        // int partition = 0; <- if we do not specify the partition explicitly, a hash function is going to be applied on the key which will determine which partition the event will go to
        for (int i = 0; i < numberOfMessages; i++) {
            // long key = i;
            String value = String.format("event %d", i);
            // long timeStamp = System.currentTimeMillis();

            // Generally using the key to determine the target partition is a better approach rather than choosing a partition explicitly because when using
            // the key we don't need to know any specifics about the topic structure and how many partitions it has. So we can run a producer that is less coupled to the topic configuration.
            // ProducerRecord<Long, String> producerRecord = new ProducerRecord<>(TOPIC, partition, timeStamp, key, value);

            // In some cases having a key and a value just doesn't make any logical sense, in this case we can publish the event with no key and the kafka producer will simply use round-robin
            // to decide to which partition it is going to send the messages to.
            // ProducerRecord<Long, String> producerRecord = new ProducerRecord<>(TOPIC, key, value);

            ProducerRecord<Long, String> producerRecord = new ProducerRecord<>(TOPIC, value);
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
