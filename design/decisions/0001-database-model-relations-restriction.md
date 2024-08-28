# Convention in Database Model to enable filtering

* Status: accepted
* Deciders: Thomas Melville, Mohit Mohanty
* Date: 2020-01-09

Technical Story: https://jira-oss.seli.wh.rnd.internal.ericsson.com/browse/SM-32737

## Context and Problem Statement

The InstantiatedVnfInfo information was originally stored as a Json string.
To allow us to filter on the information it has to be migrated from a json string to a relational model.
From looking at the ETSI SOL003 spec the relational model would be VnfInstance -> InstantiatedVnfInfo -> ScaleInfo.
However, the filtering implementation can only handle 1 level of relation.

## Decision Drivers

* How many more cases of multiple levels of relations are there.
* cost & time

## Considered Options

* restrict the relations to one level.
* update the filtering to allow multiple levels.

## Decision Outcome

Chosen option: "restrict the relations to one level", because there are only 2 cases where there could be multiple levels of relations and the cost of updating the filtering is unknown at a glance

## Pros and cons of each option

### restrict the relations to one level

Good, easy to implement
Good, because we isolate our schema from our rest model we can map the data to the structure expected by ETSI
Bad, we have to follow this for every case where there is multiple levels

#### update the filtering to allow multiple levels.

Good, there would be no restriction on the levels in the relation model
Bad, the cost to implement it unknown. It is most likely high, and we don't have much wiggle room.