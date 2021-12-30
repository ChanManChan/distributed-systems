## MongoDB

![mongodb terminology](assets/mongodb_terminology.jpg)

![mongodb operations](assets/mongodb_operations.jpg)

### MongoDB Replication Sets

![mongodb replication sets](assets/replica_set_mongodb.jpg)

By default, we launch a single mongodb instance which contained all of our data. And our application client (mongo
shell) connected, wrote and read all the data from that single node. To provide redundancy for data availability and
resilience to failures of individual nodes, we are going to replicate our data by launching multiple mongod instances. A
group of mongod instances that contain the same data is called a replication set. <br />
In a replication set there is one node that is the primary and rest of the nodes are considered secondary which is a
variation of the master-slave architecture. In mongodb by default all reads and writes from the application client go to
the primary node and the secondaries constantly sync with the primary to stay up-to-date. If the primary node fails the
secondary nodes detect that and hold an election to elect a new primary. Until that election completes no write
operations can be acknowledged. Once the old primary recovers, it can join back either as a primary or a secondary
depending on our configuration.

### Write Semantics

By default, a write operation is acknowledged as soon as the data is successfully written at the primary. However, that
may result in data loss if the primary goes down before the data is synchronously replicated to the secondaries. <br />
![write concern 1](assets/write_concern_1.jpg) <br />
So when we issue the write operation from our application, we can specify a write concern of two or more nodes. <br />
![write concern 2](assets/write_concern_2.jpg) <br />
Similarly, we can specify majority to force the write to be replicated to the majority of the nodes regardless of the
cluster size. This is one of the reasons it is recommended to have an odd number of nodes at each replication set.

### Read Preferences

All the read operations are also directed to the primary to guarantee strict consistency however we can change that
behaviour using the read preference. <br />
![read preference 1](assets/read_preference_1.jpg) <br />
When we issue the read request, we can set the read preference to "primaryPreferred" to still read from the primary when
it's available but if the primary has failed and a new primary is not being elected yet, we can still read from the
secondary nodes that are not too much out of sync. <br />
![read preference 2](assets/read_preference_2.jpg) <br />
If the number of reads is too much for the primary, and we are fine with relaxing our consistency requirements to
eventual consistency, we can even set the preference to "secondary". <br />
![read preference 3](assets/read_preference_3.jpg) <br />
And if we deployed our replication set across multiple physical locations, we can specify the read preference to "
nearest" to read from the node that has the lowest latency to our application client. This again can result in reading
stale data however that is a tradeoff we're maybe willing to pay for a lower latency depending on the business
requirement.

**Summary**

* How to create replication sets, and provide high availability through redundancy.
* MongoDB's Master-Slave architecture, where a primary node takes all the writes and reads by default for consistency.
* Data is replicated asynchronously to the secondary nodes
* How to trade off write operation latency for higher reliability using Write Concern (2+ nodes, "majority")
* Using Read Preference we can trade-off strict consistency for eventual consistency, but get higher read throughput or
  lower latency