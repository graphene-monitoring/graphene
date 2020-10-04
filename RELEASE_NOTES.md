# 1.9.1

## Feature
- Improved tag autocomplete request performance. [#69](https://github.com/graphene-monitoring/graphene/pull/69)
  * Users have to migrate documents in Elasticsearch with script located in scripts/v1.9.1/es-index-migration.py

# 1.9.0

## Feature
- support Prometheus Lexer. [#62](https://github.com/graphene-monitoring/graphene/pull/62)
- Support basic auth for Elasticsearch client [#64](https://github.com/graphene-monitoring/graphene/pull/64)
- Drop metric if metric id is greater than 512 bytes. It is es key hard limit. [#64](https://github.com/graphene-monitoring/graphene/pull/67)

## Improvement
- improve local key cache [#60](https://github.com/graphene-monitoring/graphene/pull/60)
- include all option on tag value whenever result set is not empty [#59](https://github.com/graphene-monitoring/graphene/pull/59)
- configurable resolution(rollup) [#63](https://github.com/graphene-monitoring/graphene/pull/63)

# 1.8.0

## Feature
- read and write metrics from Cassandra by following offset based scheme [#44](https://github.com/graphene-monitoring/graphene/issues/44)

# 1.7.1

## Feature
- should deserialize converted metric if label value has whitespace [#54](https://github.com/graphene-monitoring/graphene/pull/54)

# 1.7.0

## Feature
- add input kafka type with graphite deserializer [#43](https://github.com/graphene-monitoring/graphene/pull/43)
- search Graphene metrics by tags [#38](https://github.com/graphene-monitoring/graphene/pull/38)
- add prometheus deserializer in kafka input. [#48](https://github.com/graphene-monitoring/graphene/pull/48)

## Improvement
- extract elasticsearch port and protocol to property [#42](https://github.com/graphene-monitoring/graphene/pull/42) Contributed by @jacek-bakowski
- use index property each handler type [#51](https://github.com/graphene-monitoring/graphene/pull/51)

## Bug fix
- increase graphene cassandra handler max queue size [#40](https://github.com/graphene-monitoring/graphene/pull/40)
- fix several bugs [#46](https://github.com/graphene-monitoring/graphene/pull/46)
- fix prometheus deserializer edge case [#51](https://github.com/graphene-monitoring/graphene/pull/51)

# 1.6.0

## Feature
- add InfluxDB write api support [#35](https://github.com/graphene-monitoring/graphene/pull/35)
- forward to another graphene server for zero downtime upgrade [#36](https://github.com/graphene-monitoring/graphene/pull/36)

## Improvement
- add Errors enum for handling exception more easily [#37](https://github.com/graphene-monitoring/graphene/pull/37)

## Bug fix
- fix rotated index with leading zero [#34](https://github.com/graphene-monitoring/graphene/pull/34)

# 1.5.0

## Feature
- add index-based key search handler & query optimizer for performance [#32](https://github.com/graphene-monitoring/graphene/pull/32)

# 1.4.0

## Feature
- add KeyCache interface for checking duplication metric key [#25](https://github.com/graphene-monitoring/graphene/pull/25)
- add helm charts for support easy installation on k8s [#28](https://github.com/graphene-monitoring/graphene/pull/28) Contributed by @hwanjin-jeong
- add nginx docker image for graphene reader's proxy [#27](https://github.com/graphene-monitoring/graphene/pull/27)
- add dynamic template to store each key's parts in keyword type [#30](https://github.com/graphene-monitoring/graphene/pull/30)

## Improvement
- add bulkAsync feature for indexing performance [#20](https://github.com/graphene-monitoring/graphene/pull/20)
- change package name to com.graphene [#31](https://github.com/graphene-monitoring/graphene/pull/31)
- bump up dependency version [#29](https://github.com/graphene-monitoring/graphene/pull/29)

## Bug fix
- ignore unavailable index when read. [#21](https://github.com/graphene-monitoring/graphene/pull/21)
- improve Key Store throughput [#23](https://github.com/graphene-monitoring/graphene/pull/23)
- prevent calling clearScroll if there's no scrollId. [#24](https://github.com/graphene-monitoring/graphene/pull/24)
- fixed some bugs and removed unnecessary codes and operations. [#26](https://github.com/graphene-monitoring/graphene/pull/26)

# 1.3.0

## Feature
- support past metric indexing based on metric timestamp. [#18](https://github.com/graphene-monitoring/graphene/pull/18)
- full support for MapSeries, ReduceSeries functions. [#19](https://github.com/graphene-monitoring/graphene/pull/19)

## Bug fix
- Index is not determined by metric's timestamp when writing. [#17](https://github.com/graphene-monitoring/graphene/issues/17)

# 1.2.1

## Refactoring
- Modify bulk interval unit to milliseconds
- Bump up elasticsearch client version to 6.8.1
- Add auto_expand_replicas property

# 1.2.0

## Feature
- Reduce unnecessary copy operation when using StringBuilder to make response. [#5](https://github.com/graphene-monitoring/graphene/pull/5)
- Support multiple handlers using property. [#6](https://github.com/graphene-monitoring/graphene/pull/6)
- Add the index based key store handler for performance. [#7](https://github.com/graphene-monitoring/graphene/pull/7)
- The Controller just uses the content set in FullHttpResponse and change it to String AGAIN to make response body to be handled by Spring's RestController, which are all unnecessary. [#10](https://github.com/graphene-monitoring/graphene/pull/10)
- Support elasticsearch 6.x.x with index template and create automatically index. [#11](https://github.com/graphene-monitoring/graphene/pull/11)
- Add index rotating feature with the tenant ( daily, weekly ). [#13](https://github.com/graphene-monitoring/graphene/pull/13)
- Support rotating index query. [#14](https://github.com/graphene-monitoring/graphene/pull/14)
- Add multi-key store handler. [#15](https://github.com/graphene-monitoring/graphene/pull/15)

## Bug fix
- If time-series data is empty, Graphene returns ']' single string. [#8](https://github.com/graphene-monitoring/graphene/pull/8)

## Refactoring
- Try entire code refactoring to apply index based key store handler
- Massive performance and stability improvements

# 1.1.2

## Feature
- aliasByNode function now supports minus index [#2](https://github.com/graphene-monitoring/graphene/pull/2)

## Bug fix
- Fix metrics/find API duplicated path issue [#3](https://github.com/graphene-monitoring/graphene/pull/3)

# 1.1.1

## Feature
- Remove local aggregation and rollup feature

# 1.1.0

## Feature
- Development environment to use docker-compose.yml
- Support the /metrics/find API to use at Grafana [#1](https://github.com/Dark0096/disthene-reader/issues/1)
- Implement StatsService interface to be disable graphite stats service
- Change project build tool from maven to gradle
- Use spring framework for graphene

## Bug fix
- Fix /render api not supported about asterisk query [#3](https://github.com/Dark0096/disthene-reader/issues/3)
- Fix the duplication metric path to use /metrics/find API [#5](https://github.com/Dark0096/disthene-reader/issues/5)
- Add the elasticsearch client ping config to keep safely connection between elasticsearch cluster [#11](https://github.com/Dark0096/disthene-reader/issues/11)
