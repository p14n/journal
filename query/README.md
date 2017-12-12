Journal - Query component
=============

Goals

* All of the project goals, plus
* GraphQL interface
* Data-driven (this means defining clojure spec should get you most of the way to having a graph CRUD)

Structure
* src/journal - library code 
* src/app - app-specific code
* src/app/spec.clj - app spec/domain

This spec is used to generate both the datomic schema (resources/datomic/*.edn) and the lacinia graphql schema (resources/graphql.edn).  These schemas are read on startup to give our full implementation.


# Usage ##

## Run the server ##
```
boot repl
> (dev)
> (start)
> (boot.user/rest) //reloads the namespaces
> (stop)
```
Now browse to http://localhost:8085/query/graphiql/graphiql.html


## Run the tests ##
```
boot watch-test
```

## Update the datomic/lacinia schemas ##
```
boot repl
> write-schema
```

