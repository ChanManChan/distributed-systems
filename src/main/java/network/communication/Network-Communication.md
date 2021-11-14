## Multithreading vs Distributed Systems

In Multithreading passing a message from one thread to another thread was a very easy task. Since all our threads ran in
a context of the same application, they all had a shared memory where they could pass data to each other. We had to do
it carefully to avoid race conditions, so we used locks for protection and Semaphore or condition variables for
signalling.

In Distributes systems we don't have the shared memory luxury anymore so the only efficient way for our nodes to
communicate is using the network.

## TCP/IP Network Model:-

All the work that happens behind the scene to allow our computers to exchange messages with each other can be described
through 4 layers of abstraction. In this model, each layer is getting serviced from the layer underneath it and is
communicating with the same layer on the other machine or station using the relevant protocols.

```
Application  ------ HTTP, FTP, SMTP
Transport    ------ TCP, UDP
Internet     ------ IP, ICMP
Data Link    ------ Ethernet, 802.11, ARP, RARP
```

### Layer 1 - Data Link Layer:-

Physical delivery of data over a single link

```
PC1 <------------------> Router <------------------> PC2
            (Link 1)                    (Link 2)
```

* In charge of:- 
      1. Encapsulation of the data
      2. Flow control 
      3. Error detection 
      4. Error correction, etc...

#### Ethernet protocol-

Wraps our data packets into frames and uses the devices MAC addresses for packet delivery DLL is equivalent to the post
office logistics department that takes care of the post service planes, trucks and the postman schedules and makes sure
that the letter moves from one point to another point.

### Layer 2 - Internet Layer:-

Gets service from the DLL and takes a higher level role. In charge of delivering the data potentially across multiple
networks and routing the packets from the source computer to the destination computer.

#### Internet protocol -

Each device in the network is assigned an IP address and this address allows the packets to travel across the networks
from the source host to the destination host. Only we care in this layer is obtaining the IP address of the computer we
want to communicate with. In the postal service analogy, IP address is equivalent to the address of the building where
the recipient is located. Building analogy is not by accident because the Internet layer only takes care of delivering a
packet from computer to another, but it doesn't know which application process the packet is intended for nor does it
know which process sent it and where the response should go to.

### Layer 3 - Transport Layer:-

Takes care of delivering the messages end to end from the process on one machine to another process on the other
machine. Each endpoint or socket identifies itself by 16 bit port. The listening port is chosen ahead of time by the
destination application but the source port is generated on the fly by the sender depending on the available ports at
the moment. Using the port, once the packet arrives at the destination computer the operating system knows which process
the packet belongs to. In the postal service analogy, port numbers would be your exact apartment address and name which
ensures that the package that arrived at the apartment complex door would end up in your hands instead of your
neighbours or roommates.

#### User Datagram Protocol (UDP) -

Connectionless Guarantees only best effort - Unreliable Messages can be lost, duplicated or reordered Based on a unit
called Datagram which is limited in size UDP is preferred when the speed and simplicity is more important than
reliability Use cases in Distributed systems- Sending debug information to a distributed logging service Real time data
stream service such as video or audio (better to lose a few frames rather than slowing down the entire stream until the
lost frame is redelivered)
If the Distributed System connects users playing an online game it's better to keep the latency as low as possible and
most past events such as character position even if lost can be easily extrapolated from newer events. Allows
broadcasting (Decoupling between the sender and receivers)

#### Transmission Control Protocol (TCP) -

Premium mailing service Reliable - Guarantees data delivery as sent, without any loses duplications or reordering Unlike
UDP it is based on connection between exactly 2 points * Connection needs to be created before data is sent * Shut down
in the end gracefully Works as a streaming interface (TCP works as a stream of bytes rather than individual datagrams
like in UDP)
Even if two sources connects to the same destination IP and port, the data flow will be split into two separate sockets
and will be handled completely separately by the operating system and the application.

```
Source 1                |                     |Destination IP:24.289.158.147
IP:23.241.168.123       |-------------------->|socket 1
process 3 (port:3456)   |                     |
                                              |process 1 (port 8081)
                                              |
Source 2                |                     |
IP:23.241.168.124       |-------------------->|socket 2
process 13 (port:1234)  |                     |
```

Each TCP connection is uniquely identified by the four tuple of source IP and port and destination IP and port This
feature allows us to build web servers very easily, we simply need to listen on a well known port using TCP and every
host that wants to communicate with our web server, simply needs to connect to that port. Since each request comes from
a different source IP address and port number, it will create a separate connection and a separate stream of data. The
only problem is TCP works as a plain stream of bytes not distinguishing which byte belongs to what message and parsing
messages out of a stream of bytes in very difficult, we need to know where it starts and ends and how the data is
formatted.

### Layer 4 - Application Layer:-

Protocol                             | Purpose
------------------------------------ | ------------------------------------
FTP (File Transfer Protocol)         | Transferring files through the web
SMTP (Simple Mail Transfer Protocol) | Sending and receiving emails
DNS (Domain Name System)             | Translating host names into IP addresses
HTTP (Hypertext Transfer Protocol)   | Transmitting Hypermedia documents, video, sound, images

```
http://27.15.97.15:8081
1---><------2-----><-3->
```

1. Protocol we are using in the application layer
2. Address on the internet layer
3. Port we listen to on the transport layer