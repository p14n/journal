# 2. Use clojure/graphql/datomic query server

Date: 2017-11-21

## Status

Proposed

## Context

I'd like to experiment with event driven architecture to
* decouple parts of the system
* separate query for scale

I've identified a potential sweet spot with clojure's spec, graphql and datomic which could give me the flexibility I want by only specifying the schema of stored data in spec and using that to create both datomic and graphql schemas.

## References

https://github.com/walmartlabs/lacinia
https://github.com/metosin/spec-tools
https://github.com/lab-79/dspec

## Decision



## Consequences


