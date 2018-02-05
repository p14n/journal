# Query server

## Demonstration

<iframe allowFullScreen frameborder="0" height="564" mozallowfullscreen src="https://player.vimeo.com/video/254346824" webkitAllowFullScreen width="640"></iframe>

## Code structure

All code is in the subdirectory `query`

* `app` is domain specific - it defines the spec to use in query generation as well as the startup of the application
* `journal` is framework code for the query server
* `journal/spectool` is framework code for the query server generation

## What it does

There are two parts to this component
* Query server generation
* Query server

### Query server generation

The functions defined in `journal/spectool` use
* clojure.spec definitions
* An application schema (a clojure map that adds information to this spec)
* A map of resolver functions
to create a lacinia schema (written to `resources/graphql.edn`) and a datomic schema (written to `resources/datomic/*.edn`)

This action is attached to the boot task `write-schema` which calls `app.spec/write-app-schema`.

### Query server

The query server takes the schemas above and starts a datomic instance and http-kit server, using the map of resolver functions to integrate lacinia and datomic.

## How to use it

    brew install boot-clj
    git clone https://github.com/p14n/journal
    cd journal/query

### Query server generation

    boot write-schema

### Query server

    boot repl
    > (dev)
    > (start)

To run unit tests while developing

    boot watch-test

To reload the server after changes

    > (boot.user/reset)

Navigate to http://localhost:8085/query/graphiql/graphiql.html

### Testing

```
boot coverage
```
