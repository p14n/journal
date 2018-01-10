# Journal

## What's this all about?

This software is the result of iterating over ideas I've heard about in books, on twitter, and from friends.  While it's primarily a playground for ideas, each idea builds on the previous, adding some new quality or ability.  Tests, docs, deployment - everything should be production quality.

## Setup

    brew install boot-clj
    git clone https://github.com/p14n/journal
    cd query
    boot repl
    > (dev)
    > (start)

Navigate to http://localhost:8085/query/graphiql/graphiql.html

## Architecture

### Decision records

* [Record architecture decisions](/architecture/decisions/0001-record-architecture-decisions/)
* [Implement a query server with graphql](/architecture/decisions/0002-use-clojure-graphql-datomic-query-server/)



