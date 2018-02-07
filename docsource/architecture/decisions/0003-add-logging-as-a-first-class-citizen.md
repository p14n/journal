# 3. Add logging as a first-class citizen

Date: 2018-02-05

## Status

Accepted

## Characteristics addressed

* It always knows what happened

## Context

While working on the query component, no decision was made about logging and the code ended up with many `println` statements trying to debug.  This needed addressing, and I wanted to start of with a clear direction for application logging.  Prior art available at:

https://juxt.pro/blog/posts/logging.html

## Decision

The approach Juxt took passes a logger down the stack explicity.  I'm happy with this approach as in practice that only affected the API of a few functions.  It keeps the implmentation very simple and leaves us agnostic about where logs will be stored.

## Consequences

We now have detailed logging at INFO at the system boundaries, namespaced by area.

```
		{:service :graphql/mutate, :level :info, :body {:Person/email dan@p14n.com, :Person/firstname Decan}, :reqid 0}
		{:service :datomic/upsert, :level :info, :body {:desc [:Person/email dan@p14n.com :Person/firstname Decan :db/id #db/id[:db.part/user -1000038]]}, :reqid 0}
```

A logger is created at the network boundary that will add a request id for tracking to the log.  The request id is currently generated on the server - in the future this should be defaulted to one provided by the client.
