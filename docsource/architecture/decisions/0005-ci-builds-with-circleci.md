# 5. CI builds with CircleCI

Date: 2018-02-22

## Status

Accepted

## Context

The query server introduced some automated tests we want to keep maintained and passing, and I'd like one of those little <passing> badges, and a place to publish our coverage reports. Onyx is probably about to make our deployment a bit more complex so it would be good to have something that can deploy to a cloud server.  Having seen first hand how complex Jenkins can get I am drawn towards CI/CD servers that rely entirely on a file in the repository for configuration.  I've recently had experience with circleci and particularly liked the ability to run local builds when testing.

## Decision

We'll go with circleci for now - it was simple to set up and will be simple to leave if it doesn't fit the bill later.  If we were running a server locally I'd look at https://concourse.ci/

## Consequences

The documentation can be found here: https://circleci.com/docs/2.0/
The configuration file is in ./circleci/config.yml.  You can click on the badge to see the build runs.
