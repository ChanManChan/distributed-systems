## Complex Data Delivery - Serialization & Deserialization

The process of packing and unpacking the data into a format we can send through the network is called serialization and
deserialization.

**Serialization -** Translation of a data structure or object into a format that can be sent (or stored somewhere) and
reconstructed later. <br/>
**Deserialization -** Reconstruction of the data back to a data structure or an object

Any time we have a HTTP transaction where we pass some data from one node to another the sender performs the data
serialization prior to sending over the wire and the recipient performs data deserialization after it receives the
message.

**Serialization Formats:**

1. JSON - Javascript Object Notation
2. Java Object Serialization
3. Google's Protocol Buffers

### JSON

**Disadvantages:**

1. Requires explicit serialization and deserialization
    * Often requires external libraries
    * Those libraries need explicit configurations and annotations to tell them how to serialize and name the fields.
2. JSON does not have a schema(source of truth)
3. Plain text parsing and transmission is suboptimal

### Java Object Serialization:

If we intend to communicate between nodes running on the JVM, instead of having an intermediate format like JSON, We
serialize the JAVA object into a byte stream directly and reconstruct it on the recipient node to get the original
object state. Marking the object with the Serializable interface is enough to make any Java object eligible for seamless
serialization and deserialization. <br />
By Implementing the Serializable interface, upon serialization all the members of the class are going to be
automatically serialized except for:

* Static members
* Transient members

For successful deserialization of the object on the recipients end the class definition has to match between the sender
and the receiver. The class has to have an accessible no-args constructor.

```Java
class Example {
    public byte[] serialize(Data data) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutput objectOutput = new ObjectOutputStream(byteStream);
        objectOutput.writeObject(data);
        objectOutput.flush(); // <- flush the stream to force the serialization
        return byteStream.toByteArray();
    }

    public Data deserialize(byte[] data) {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
        ObjectInput objectOutput = new ObjectInputStream(byteStream);
        return (Data) byteStream.readObject(); // <- deserialize our data back to a Java object
    }
}
```

**Advantages:**

1. Guarantees about correct state reconstruction without any type ambiguity
2. Very clear source of truth (schema) for the object we want to send over the network
3. Native support in all the JVM languages

### Google's Protocol Buffers:

We first define the structure or the schema for the message in a special proto format and stored in a proto file. Then
we use a Proto Compiler to generate a language specific stub with all fields in the message as well as the serialization
and deserialization methods. The Proto Compiler will generate a class for any language we need so we can use it directly
in our program. Using this operation between the schema definition in a proto file and the generated language specific
stubs, the message definition stays language independent but the applications written in different languages can use
their own language specific classes, types and methods directly.

```
syntax = "proto2"

message Task {
   required string city = 1;
   repeated Date date_range = 2;
   repeated string categories = 3;
   repeated int64 age_range = 4;
   optional bool debug = 5;
   
   message Date {
      required uint32 day = 1;
      required uint32 month = 2;
      required uint32 year = 3;
   }
}
```

After this proto file goes through the Proto Compiler we get a Java class that contains regular getter methods plus the
helper methods to serialize and deserialize the object to and from a byte array. This allows us to use this auto
generated class just like a regular class we wrote ourselves.

**Advantages:**

1. No type ambiguity.
2. Clear and well-defined schema(stored in the proto file)
3. Language independent thanks to the 2-step process:
    * Proto file definition
    * Language specific stub generation using proto compiler
4. Efficient serialization and deserialization
5. Security