## TF-IDF Algorithm

We can express the TF-IDF algorithm as a sequential double nested loop. We have two major dimensions to our problem:

* D - Number of documents
* T - Number of Search Terms <br />
  Decide on which dimension we want to parallelize the algorithm (which dimension we are going to partition our data
  among the nodes)

```Java
class Example {
    public void sortDocumentsBasedOnQuery(List<Document> documents, List<Term> query) {
        for (Document doc : documents) {
            for (Term term : query) {
                doc.score += tf(term, doc) * idf(term);
            }
        }
        // sort(documents, by score) -> list of relevant documents
    }
}
```

### Parallelizing / Splitting the workload

Since we have the search terms on one side and documents on the other we can split the work only on one of those
dimensions

* We can either partition the problem by terms, giving each node a subset of the search terms and all the documents are
  going to be shared.
* Or we can partition the data by documents giving each node a subset of the documents in our repository and all the
  search terms are going to be shared by all nodes.

The decision on how to parallelize the algorithm is critical to make the algorithm scalable (should scale horizontally
and should not choke as soon as the dataset grows) <br />
We need to choose the dimension on which the input to our system is expected to grow the most. <br />
If/ When the data in that dimension does grow, then we can simply add more machines to the cluster, and the system will
easily scale.

### Choosing the Dimension for data partitioning

* Do we expect the number of terms in the query to grow significantly overtime? No (number of terms is typically within
  the boundaries of a sentence or a few sentences at most)
* Do we expect the numbers of documents to grow overtime? Yes (more documents we have, better our search results are
  going to be)
  Then we choose to parallelize the TF-IDF algorithm by the documents

### Parallelize TF-IDF to run in a cluster

To calculate each document score we need to compute two components, tf and the idf. <br />
We can easily calculate the term frequency in parallel by different nodes as each term frequency depends on the term and
the words in one particular document. This is good news because scanning through all the words of a document is the most
intensive operation in the algorithm. <br />
Unfortunately Inverse document frequency cannot be calculated in parallel by different nodes. That's because to
calculate IDF for each given node, we need the information from all the documents in the repository.

```
idf("term1") = log(N/nt)
N - Number of documents
nt - Number of documents containing the term ---> (Needs information from [Doc1, Doc2, Doc3... DocN])
```

```Java
class Example {
    public void sortDocumentsBasedOnQueryInParallel(List<Document> documents, List<Term> query) {
        for (Document doc : documents) {
            for (Term term : query) {
                doc.score += tf(term, doc) * idf(term);
            }//                   ^             ^
        }//                       |             |
        //       Can calculate in parallel  Cannot calculate in parallel
        // sort(documents, by score) -> list of relevant documents
    }
}
```

### Parallel Term Frequency Calculation

1. The leader will take the documents and split them evenly among the nodes
2. Then the leader will send a task to each node that contains all the search terms and the subset of documents
   allocated to that particular node
3. Each node will create Map for each document allocated to it and each of those Maps will map from a search term to its
   term frequency in that particular document
4. Each node will aggregate all the DocumentData objects for all its allocated documents and send the results back to
   the leader
5. At this point the leader will have all the term frequencies for all the terms in all the documents
6. At the final aggregation step the leader will calculate the IDF for all the terms (easy to derive from the term
   frequency)
7. Finally, it will score the documents
8. Sort the documents by relevance

### System Architecture

1. We have our cluster of search nodes and the front-end server
2. Using Zookeeper we elect a leader from the cluster to act as a coordinator
3. Once the roles are assigned, the worker nodes will register themselves with the Zookeeper base service registry
4. The leader will pull those addresses, so it can send tasks to those worker nodes
5. The leader will register itself to a different ZNode in the service registry to allow the front-end server to find
   its address
6. Once the search query comes as an input to our system the front-end server will look up the search coordinators
   address in the service registry and forward the search query to the search coordinator
7. Once the coordinator receives the search query it will inspect all the currently available workers and send the task
   through http to all the worker nodes
8. Once a node gets its allocated set of documents it will read them from the document repository, perform the
   calculation and sends the result back to the leader
9. When the leader gets all the results, it will perform the final aggregation, scoring and sorting and sends the result
   back to the upstream system
