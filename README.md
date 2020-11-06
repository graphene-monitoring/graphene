![Graphene](docs/logo/GrapheneSignature.png)
[![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/)  [![CircleCI](https://circleci.com/gh/graphene-monitoring/graphene.svg?style=svg)](https://circleci.com/gh/graphene-monitoring/graphene)   [![Join the chat at https://gitter.im/graphene-community/community](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/graphene-community/community)

## What is Graphene?
Graphene is a java version of [Graphite](https://graphiteapp.org). To
store metrics, it uses [Cassandra](https://github.com/apache/cassandra)
and [Elasticsearch](https://github.com/elastic/elasticsearch) instead of
[Whisper](https://github.com/graphite-project/whisper) to handle data in
a scalable way. It is designed to be compatible with Graphite's API and
easily integrates with Grafana's Graphite datasource.
In a cloud environment, the metric key increases exponentially. To cope
with this, each metric needs to be managed by time and removed or
archived according to the policy. Graphene is a system that supports
these methods later to help you manage your metrics cost-effectively. It
also supports multi-tenancy for metrics later, so that large numbers of
metrics can be maintained and managed in isolated clusters.

## Architecture Overview

![Graphene](docs/main/GrapheneArchitecture.png)

## Function

### [transformNull](https://graphite.readthedocs.io/en/latest/functions.html#graphite.render.functions.transformNull)

Origin:
```
&target=transformNull(webapp.pages.*.views,0)
&target=transformNull(webapp.pages.*.views,-1)
```

New:
The most recent value can be used through 'recent' argument.
```
&target=transformNull(webapp.pages.*.views,'recent')
```

## API Support

### Graphite

|           API             | Support |
|---------------------------|---------|
| /render                   |    O    |
| /find                     |    O    |
| /metrics/find             |    O    |
| /tags/autoComplete/tags   |    O    |
| /tags/autoComplete/values |    O    |

### InfluxDB

|     API       | Support |
|---------------|---------|
| /write        |    O    |

## Prerequisite

| Storage       | Version |
|---------------|---------|
| Elasticsearch | 6.8.x   |
| Cassandra     | 3.11.x  |

## Commit convention
This project uses the git conventional commit rule provided by [conventional commits](https://www.conventionalcommits.org/en/v1.0.0-beta.4/)

## Configuration
Please check [this document](https://github.com/graphene-monitoring/graphene/wiki/Configuration) for the Graphene configuration

## Thanks

Thanks, this project is useless without their work on **cyanite**, **graphite-api**, **graphite-cyanite**, **disthene-reader**, **disthene**

- Pierre-Yves Ritschard ([https://github.com/pyr](https://github.com/pyr))
- Bruno Renié ([https://github.com/brutasse](https://github.com/brutasse))
- EinsamHauer ([https://github.com/EinsamHauer/disthene-reader](https://github.com/EinsamHauer/disthene-reader))

Many people helped with the Graphene project.

- Keyn : Keyn had provided a lot of insights to improve Graphene system. Without Keyn, this project could not grow.
- Tiger : Tiger showed how to modify the open source and apply it to the production environment in order to improve the Graphite system operated by one server. This led to an understanding of the metric system and the ability to continue to expand the current system.
- El : El provided continuous insight based on superior technology than others. Based on this, the system can be viewed from various points of view instead of just one part.
- drunkencoding / panda84 : They developed and maintained the entire metric core system like Elasticsearch indexing logic and time life cycle in the metric key (path). Without them, the metric system would only be able to handle simple traffic.
- jerome89 ([https://github.com/jerome89]) : He found the grammar of Graphite functions in the disthene project. This helped us to create a better metric system. Additionally, he helped develop the faulty function of the graphene itself and the overall system development.

## License

The MIT License (MIT)

Copyright (C) 2015 Andrei Ivanov

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
