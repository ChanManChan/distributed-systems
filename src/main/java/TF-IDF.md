## Distributed Search

In a typical search problem as an input to out system ahead of time we have a large set of documents which can be:

* Books
* Academic
* Articles
* Legal Documents
* Websites

User then provides us with the search query in real time to get the most relevant documents or links to their search.
Based on that search query, we want to identify which documents are more relevant and which are less relevant and
present all those results to the user.

We have to sort or rank the documents we have in our repository by their relevance based on the terms or words in the
search query that the user provided.

**Example**:
_Search Query_: "Very Fast Cars"
_Output_:

* Formula 1 tournament news
* Fastest new cars of the year
* ...
* ...
* Top 10 recipes for a chocolate cake

### Search Algorithm - Attempt 1

Word count of the search terms. For every document, count how many times each search term appears in the document. The
documents with the highest count are the most relevant.

```
search query = [term1, term2, term3]
score(document) = term1_count + term2_count + term3_count
sort(documents, by score) -> list of relevant documents in descending order
```

**Example**:
search query = ["very", "fast", "cars"]
score(document) = 20 + 10 + 30 = 60

**Problem**:
The algorithm favors larger documents with more words in them. <br/>
**Example**: <br />
search term = "car" <br />

|       |  Articles about Cars (500 words)  | Long book about Food (500,000 words) |
|:------|:---------------------------------:|-------------------------------------:|
| count | 50                                | 60                                   |
| score | 50                                | 60                                   |

Long and irrelevant book scored higher than the clearly more relevant article about racing cars

### Search Algorithm - Attempt 2

> Term Frequency = (term count) / (total number of words in document)

_tf = term frequency_
The above formula gives us the relative term frequency in each document instead of just the raw count.

|                |  Articles about Cars (500 words)  | Long book about Food (500,000 words) |
|:---------------|:---------------------------------:|-------------------------------------:|
| count          | 50                                | 60                                   |
| Term Frequency | 50/500 = 0.1 (10%)                | 60 / 500,000 = 0.00012 (0.012%)      |

Natural next attempt to score documents is by calculating the term frequency of each term in that document and sum all
the term frequencies to get the total document score

```
score(doc1) = tf(term1, doc1) + tf(term2, doc1) + tf(term3, doc1)
score(doc2) = tf(term1, doc2) + tf(term2, doc2) + tf(term3, doc2)
...
score(docN) = tf(term1, docN) + tf(term2, docN) + tf(term3, docN)
sort(documents, by score) -> list of relevant documents in a descending order
```

This is much better than what we had before, but we still have a major problem with this approach.

**Example:** <br />
search query = "The fast Car" (the user is clearly interested in documents about cars and in particular fast cars)

based on our algorithm our document score will be incorrectly skewed towards documents that have the common and less
important word "The" more frequently.

```
score(doc_i) = tf("The", doc_i) + tf("fast", doc_i) + tf("Car", doc_i)
                    ^                                       ^
                    |                                       |
                Less important                          More important
```

The reason for this problem is that our terms are equally weighted in the algorithm but are not equal in their
importance for the search.

### TF-IDF Algorithm

* Term Frequency - Inverse Document Frequency (tf-idf)
* Measures how much information each term in the search query provides us.
* Common terms that appear in many documents do not provide us any value, and get lower weight
* Terms that are rarer across all documents get higher weight

Idea of the algorithm is to calculate the term frequency for each term in the document just like before but also
multiply each term frequency by the inverse document frequency of that term across all the documents which acts as the
weight we give to each search term.

```
score(doc_i) = tf("The", doc_i) X idf("The")
            +  tf("fast", doc_i) X idf("fast")
            +  tf("Car", doc_i) X idf("Car")

idf(term) = log(N/nt)
N - Number of documents
nt - Number of documents containing the term

Example:
idf("The") = log(10/10) = log(1) = 0
We have 10 documents in our repository
Given that the term "The" appears in all of them which is quite likely.

idf("Car") = log(10/1) = log(10) = 1  <- the word "Car" appears only in one document
idf("fast") = log(10/5) = log(2) = 0.3 <- the word "fast" appears in five documents

score(doc_i) = tf("The", doc_i) X [idf("The") = 0] <- because the word "The" appears in all the documents, it weighs down it's term frequency to 0
            +  tf("fast", doc_i) X [idf("fast") = 0.3]
            +  tf("Car", doc_i) X [idf("Car") = 1] <- weight of the more rare term, "Car" is the heighest

for(Document doc : documents) {
    doc.score = tf("The", doc_i) X 0
            +   tf("fast", doc_i) X 0.3
            +   tf("Car", doc_i) X 1
}

sort(documents, by score) -> list of relevant documents
```

The more important terms are weight higher and the term that appear everywhere will be simply ignored. TF-IDF is a
statistical algorithm and requires a large enough set of documents to give good results.

**Example:** <br/>

We have two documents containing the word "Car" 


