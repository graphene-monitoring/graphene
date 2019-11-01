package com.graphene.writer.store.key.property

import com.graphene.writer.config.IndexBulkConfiguration

open class ElasticsearchKeyStoreHandlerProperty(
  open var clusterName: String,
  open var tenant: String,
  open var templateIndexPattern: String,
  open var index: String,
  open var type: String,
  open var cluster: List<String>,
  open var port: Int,
  open var bulk: IndexBulkConfiguration?
)