## Apache Kafka

* Apache Kafka is open source and provides:
    * Distributed Queuing
    * Publish/Subscribe capabilities
* Beautifully designed Distributed System for high scalability and fault tolerance
* Distributed Streaming Platform, for exchanging messages in realtime between different servers
* Can be described as a Message Broker on a high level
* Internally Kafka is a distributed system that may use multiple message brokers to handle the messages

### Apache Kafka architecture, topics and partitions

**Apache Kafka's Producer Record** <br />
![kafka record](assets/kafka-record.jpg) <br />
The message we want to publish to Kafka comes inside a record that consists of a Key, Value which is our message and a
Timestamp

**Kafka Abstraction - Topics** <br />
![kafka topic](assets/kafka-topic.jpg) <br />
The center of the abstraction that Kafka provides is the topic, we can think of topic as a category of messages or
events. Publishers can publish records to any topic and subscribers can consume records from any topic.

**Topics and Partitioned Logs** <br />
![kafka topic partition](assets/topic-partition.jpg) <br />
Instead of having a monolithic log or queue for each topic, Kafka allows us to partition each topic into multiple logs
that really look like arrays of records. Whenever a new record is published to a topic, it is appended to one of those
partitions and is assigned a sequence number in an increasing order. Each such sequence number is called an offset. We
can think of a topic as a collection of ordered queues where each partition is a separate independent queue of records.
Each partition maintains the messages in the order they were published however there is no global ordering between the
messages inside the topic.

**Kafka Keys and Partitions** <br />
![kafka keys and partition](assets/keys-and-partition.jpg) <br />
The partition to which a record is appended depends on the key the publisher sets in the record. More precisely a hash
function is applied on the records key and that determines where that record would go.

**Kafka Scalability through Partitioning** <br />
Kafka Topic partitioning allows us to scale a topic horizontally. More partitions in a topic -> higher parallelism.

**Kafka Partitioning - Online Store** <br />
![kafka partition 1](assets/partitioning-1.jpg) <br />
For example, The Purchases Topic in an online store gets a lot of messages. So it can spread those messages across a
larger number of partitions. <br />
![kafka partition 2](assets/partitioning-2.jpg) <br />
Purchases from different users are not related to each other so maintaining the order of may not be very important. So
we can use the userId as the key for the record. This way we can spread all the purchasing events across all the
partitions. On the other hand order of events from the same user is important. For example, a user may purchase an item
and then immediately ask for a refund for the same item. And because they key which is the userId for both messages is
the same, the records for both those events will go to the same partition and will be consumed in the correct
order. <br />
![kafka partition 3](assets/partitioning-3.jpg) <br />
In the same time, Topics that receive fewer messages can have fewer partitions. Or if the order is always important in
all cases we can even have only one partition per topic.

### Consumer Groups, Distributed Queue and Pub/Sub

**Kafka Consumer Groups** <br />
![kafka consumer groups](assets/consumer-groups.jpg) <br />
In order for an application instance to consume messages from a Kafka topic, a consumer has to belong to a consumer
group. When consumers subscribe to a topic each message is delivered to a single instance in a consumer group.

**Kafka as a Distributed Queue** <br />
![kafka distributed queue](assets/distributed-queue.jpg) <br />
So if the processing of the published messages is complex, we can place multiple consumer instances in a group and the
messages to our topic will be load balanced among all the consumers within that group.

**Kafka as a Publish/Subscribe system** <br />
![kafka pubsub](assets/pubsub.jpg) <br />
We can place each consumer in a different consumer group which means every message published to a topic will be
delivered to all the consumers.