# Journal

Welcome to the Journal docs.  This page talks about the characteristics the architecure exhibits (or plans to exhibit) introduces each component, and shows you how to see it in action on your own laptop.

## Characteristics

### Its user-access agnostic (it's not just a web app)

Who knows what we're building here?  It's probably web, could be mobile, might just be a SaaS api. It's an application designed to be access remotely (I'm hoping it's not a device driver) but beyond that we should make no assumptions and treat all types of acces in the same way.

### It's got X-Factor scale (yes, the TV show)

The 2010 UK X-Factor final attracted 17.7m viewers.  Let's say each of those people, during the time of the final (2 hours), wanted to make 20 requests to our app.  That gives us a goal of 50k requests per second (17.7m / 120 mins / 60 seconds * 20 requests).  That seems like a reasonable goal while still working on a toy infrastructure.  Yes, I know it's just made up, but so are viewing figures  - they're based on 5000 houses.

### It always knows what happened

Testing new ideas, application monitoring and issue resolution all require information about what is going on, and what has happened. We should never need to dial up the logging or find ourselves guessing - the application ahould be publishing this information.

### It's cheap and fast to experiment with

It's not going to get used unless it's more like using Ruby on Rails than JavaEE. We should be able to try out new business ideas quickly and ditch them quicker.

### It can deploy to production in under 1 hour from code commit

The one hour should include going through tests that give us confidence to deploy to production from the Christmas Party.

### It expects to change

The application should not be locked into a vendor, API or language.  It should be data-centric (preferably with a schema) - we don't want to restrict which platforms we use to build on. 

### It's resistant to failure

Losing a particular node won't matter, losing a system component will not prevent the others from working.

### It knows where its vulnerabilities are

There will always be information available to show where security vulnerabilities are as of the last check-in.  

### It knows what's wrong

There will always be information available to show what's happening under the covers, and alert you when they're not quite right.

### It's usable by anyone, regardless of physical ability or language

The UI should support screen readers, be flexible in display sizing and use any language including double byte and right to left.

## Architecture

### Components

* [Query](/components/query/)

### Decision records

* [Record architecture decisions](/architecture/decisions/0001-record-architecture-decisions/)
* [Implement a query server with graphql](/architecture/decisions/0002-use-clojure-graphql-datomic-query-server/)

## Setup

    brew install boot-clj
    git clone https://github.com/p14n/journal
    cd journal/query
    boot repl
    > (dev)
    > (start)

Navigate to http://localhost:8085/query/graphiql/graphiql.html

## Documentation

This documentation uses mkdocs (configured in mkdocs.yml).  The documentation source is in `./docsource`.  Running `mkdocs build` will generate the documentation in `./docs`


