# 4. Event-driven decoupling with Onyx

Date: 2018-02-07

## Status

Proposed

## Characteristics addressed

* It's got X-Factor scale (yes, the TV show)
* It always knows what happened
* It expects to change
* It's resistant to failure

## Context

The main driver for moving to an event-driven architecture is flexibility - when everything is an event, more parts of the application can be added that simply listen out for those events, and perform their own actions with them.  Where we'd like to take this further is to store these events forever, meaning at any point we can add a new component and replay the data from the applications inception.  There are examples of this having been experimented with, such as 

https://github.com/Yuppiechef/cqrs-server

## Decision

query
 - direct to datomic

mutation
 - send to onyx
 - wait for response and perform query if necessary
 
 :in :command
 :command :response
 

## Consequences

Consequences here...
