## Message Brokers motivation

**Synchronous Network Communication** - <br />
One of the properties of direct network communication is that its inherently synchronous. In this case we are not
referring to the internal threading model or the network API used by the servers, but to the fact that throughout the
entire data transfer both the server and client have to be present and maintain a connection with each other. <br />
**Synchronous Network Communication Through Load Balancers** - <br />
And even if the servers are part of different logical clusters or if they communicate with each other through a load
balancer, both the servers have to maintain a TCP connection to each other or to the load balancer all the way from the
first byte of the http request to the last byte of the http response.

**Synchronous Communication example** <br />
_Financial Event Pipeline_ <br />
![sync communication example](assets/sync-communication.jpg) <br />
Service A is an online store web application which receives a purchase request from a user, then to complete the
purchase we have to withdraw the money from the users account. So Service A would call the Service B which is the
billing service that connects to different credit card companies and banks. After the money has been withdrawn from the
user, we need to update the inventory of the product to make sure multiple users would not buy the same item. So Service
B will pass the request to Service C which is the inventory service. Finally, we need to request the shipment of the
item to the users address so Service C will call the shipping service, Service D. <br />
Service A cannot confirm the purchase until Service B sends back a confirmation and Service B would not send that
confirmation until it gets the response from Service C and Service C would not send the confirmation until it gets the
response from Service D. <br />
The reason for this chain of dependencies is for example if Service A responds before it gets confirmation from Service
B but then Service B crashes and doesn't bill the user, the user will get a false confirmation about an order he will
never receive. Alternately if Service B sends back the confirmation after billing the user but before a response from
Service C arrives, and Service C fails. The user will end up paying for a product that he will never receive. <br />
So Inevitably we are holding the user for a long time until we can safely send the confirmation and the longer we keep
the connections, the more chances are that one of them will actually break.

**Broadcasting Event to Many Services** <br />
In this scenario, one of our servers wants to broadcast an event to many services that are interested in receiving it.
The problem with this scenario is that the more servers wants to receive the event, the more direct connections the
publisher server will have to open. So this approach just would not scale.

**Traffic Peaks and Valleys** <br />
When the traffic to our systems has drastic peaks during which our system just cannot handle the incoming events all at
once. Example if we have a social media distributed system where sometimes there is a live event during which millions
of users comment all at once. During that time our analytics backend service may not keep up with the rate of events and
would simply crash. So having some sort of queue where we could store all those events in order temporarily would
definitely help to solve the problem. <br />

_To address all these scenarios we will use a system called message broker_

## Message Broker

1. Intermediary software (middleware) that passes messages between senders and receivers
2. May provide additional capabilities like:
    * Data transformation
    * Validation
    * Queuing
    * Routing
3. Full decoupling between senders and receivers

Contrary to load balancing proxies that make themselves unnoticeable to clients and servers and create an illusion of
servers talking to each other directly, the message broker enforces its own network protocol, data formats and APIs. The
best part about message brokers is they decouple the system to the extent that a receiver does not have to be present
when a message is sent. And that message can be successfully received without the presence of the sender. If the message
processing is complex, the message broker can also act as a load balancer but with a lot more powerful queuing
capabilities which allows us to scale the receivers horizontally but also keep the messages from getting lost if the
receivers are overwhelmed. <br />
Using this queuing we can decouple our synchronous system into a fully asynchronous system that provides stronger
guarantees that the messages will be delivered to the intended recipients. <br />
Using the Publish/Subscribe paradigm a sender can publish a message to multiple servers by sending a single message to
the message broker. And the message broker will take care of broadcasting to all the intended subscribers without the
sender even knowing who they are. <br />
On the other hand, a receiver may subscribe to a single topic/queue and receive messages from many publishers without
knowing anything about them. <br />
Finally when we use direct network communication, the client is always pushing the message to a passively listening
server. And as long as the server is listening on a port, it doesn't have any control on when and how frequently to
receive the messages. However, when we are using a message broker, the sender pushes the messages to the message broker
and the receiver pulls those messages whenever it chooses to. That pulling approach gives a lot more control to the
receivers over the rate of messages consumption.

#### Message Brokers Scalability

1. To avoid the Message Broker from being a single point of failure or a bottleneck
2. Message Brokers need to be scalable and fault-tolerant
3. The latency, when using a message broker, in most cases, is higher than when using direct communication

### Most popular use cases

1. Distributed Queue - Message delivery from a single producer to a single consumer
2. Publish/Subscribe - Publishing of a message from a single publisher to a group of subscribed consumers