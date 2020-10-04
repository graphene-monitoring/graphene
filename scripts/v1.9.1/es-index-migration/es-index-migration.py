import sys
from getpass import getpass
try:
  from elasticsearch import Elasticsearch, helpers
except ImportError:
  raise ImportError("Please install python elasticsearch dependency.")


def migrate(es, tag_index, fetch_size, bulk_size):
  print("Migrate documents in index: " + tag_index)
  resp = es.search(
    index=tag_index,
    body={},
    size=fetch_size,
    scroll='60s',
    request_timeout=30
  )
  if resp['hits']['total'] == 0:
    print("SKIP! No documents to migrate.")
    return
  print("Migration will be done on total " + str(resp['hits']['total']) + " document(s) ...")
  print("Fetching all documents from index: " + tag_index)
  scroll_id = resp['_scroll_id']
  docs_to_migrate = []
  docs_to_migrate.extend(resp['hits']['hits'])
  while len(resp['hits']['hits']):
    resp = es.scroll(
      scroll_id=scroll_id,
      scroll='60s',
      request_timeout=30
    )
    print("Fetched total " + str(len(docs_to_migrate)) + " document(s).")
    docs_to_migrate.extend(resp['hits']['hits'])
  migrate_docs(es, docs_to_migrate, bulk_size)
  print("Finished migration in index: " + tag_index)


def migrate_docs(es, docs, bulk_size):
  if len(docs) == 0:
    return
  bulk = []
  total = len(docs)
  count = 0
  for doc in docs:
    if count > 0 and count % bulk_size == 0:
      print("Bulk updating ... (" + str(count) + "/" + str(total) + ")")
      helpers.bulk(es, bulk)
      bulk = []
    if '_id' not in doc.keys() or '_type' not in doc.keys() or '_index' not in doc.keys():
      print("Wrong doc!")
      continue
    doc_id = doc['_id']
    doc_type = doc['_type']
    doc_index = doc['_index']
    tag_list = []
    for tag in doc['_source'].keys():
      if tag not in tag_list and not tag.startswith('@'):
        tag_list.append(tag)
    update_doc = {'_op_type': 'update', 'doc': {}, '_index': doc_index, '_id': doc_id, '_type': doc_type}
    update_doc['doc']['@tags'] = tag_list
    bulk.append(update_doc)
    count = count + 1
  if len(bulk) > 0:
    print("Bulk updating remaining " + str(len(bulk)) + " document(s) ...")
    helpers.bulk(es, bulk)
  print("Finished migration for " + str(count) + " among total " + str(total) + " document(s)!")


if len(sys.argv) < 3:
  raise Exception("Please provide elasticsearch host and port.")

es_host = str(sys.argv[1])
es_port = int(sys.argv[2])
es_username = str(input("Please enter the Elasticsearch username(leave empty for no authentication): "))
if len(es_username.strip()) != 0:
  es_password = str(getpass("Please enter the Elasticsearch password: "))

if len(es_username.strip()) > 0:
  es = Elasticsearch(
    [es_host],
    port=es_port,
    http_auth=(es_username, es_password)
  )
else:
  es = Elasticsearch(
    [es_host],
    port=es_port
  )
bulk_size = 10000
fetch_size = 10000

tag_indices = list(es.indices.get_alias("tag*").keys())
for tag_index in tag_indices:
  migrate(es, tag_index, fetch_size, bulk_size)
print("Migration Complete!")
