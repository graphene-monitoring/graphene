## Key document migration script for faster tag autocomplete request
Related issue: https://github.com/graphene-monitoring/graphene/issues/68

Simple python script for migrating documents to restructure indexed metric key for faster tag autocomplete request.

### Requirements
- Python3
- Python3 Elasticsearch client

### Usage
```python3 es-index-migration.py ${ELASTICSEARCH_HOST} ${ELASTICSEARCH_PORT}```
