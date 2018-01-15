# 2. Use clojure/graphql/datomic query server

Date: 2017-11-21

## Status

Proposed

## Characteristics addressed

* Its user-access agnostic (it's not just a web app)
* It's cheap and fast to experiment with
* It expects to change

## Context

The first thing we want to do is store and read whatever our domain needs. This domain will be changing frequently, so we want to minimise boilerplate that's connected to the domain structure, but we also want to avoid too much magic that will make partially or fully stepping away from this experiement difficult.  Can we create something that allows us to specify our domain in clojure.spec and automatically convert that into Datomic and GraphQL schemas?

## References

https://github.com/walmartlabs/lacinia
https://github.com/metosin/spec-tools
https://github.com/lab-79/dspec

## Decision

Why, yes we can!

It was simple to create functions that turned spec into lacinia/datomic schema.  It removes the chore of defining individual attibutes in three places
.  It remains to be seen whether it saves time overall or the productivity gain is limited to only simple cases.

We're happy introducing clojure.spec as a system of record as we can transpile to another format easily (as here with lacinia and datomic).

GraphQL is a welcome addition with a growing platform-agnostic (but primarily javascript focused) community that doesn't limit how we access the app.

## Consequences

* Clojure, datomic and graphql have been adopted for this component
* Our domain is now currently specified in clojure.spec - it will need to remain there or be created from another format
* We're using the boot build tool because we can write functions to define build tasks
* boot-alt-test gave vastly improved test performance

## Included but unassessed

* http-kit
* compojure/ring
