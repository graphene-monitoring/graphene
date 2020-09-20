package com.graphene.writer.store.key.elasticsearch

import com.graphene.common.key.RotationProperty
import com.graphene.common.key.RotationStrategy
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.reactor.IOReactorConfig
import org.apache.logging.log4j.LogManager
import org.elasticsearch.action.ActionListener
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest
import org.elasticsearch.action.admin.indices.get.GetIndexRequest
import org.elasticsearch.action.admin.indices.get.GetIndexResponse
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.action.get.MultiGetRequest
import org.elasticsearch.action.get.MultiGetResponse
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.NodeSelector
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.client.sniff.Sniffer
import org.elasticsearch.common.xcontent.XContentType

class ElasticsearchClientTemplate(
  httpHosts: Array<HttpHost>,
  userName: String?,
  userPassword: String?,
  rotationProperty: RotationProperty
) : ElasticsearchClient, RotatedIndexAware {

  private val logger = LogManager.getLogger(ElasticsearchClientTemplate::class.java)
  private var restHighLevelClient: RestHighLevelClient
  private var sniffer: Sniffer
  private var rotationStrategy = RotationStrategy.of(rotationProperty)

  init {
    val restClientBuilder = RestClient.builder(*httpHosts)
      .setRequestConfigCallback { config ->
        config.setConnectTimeout(5_000)
        config.setSocketTimeout(30_000)
      }
      .setHttpClientConfigCallback { config ->
        config.setDefaultIOReactorConfig(
          IOReactorConfig.custom()
            .setIoThreadCount(Runtime.getRuntime().availableProcessors())
            .build()
        )
      }
      .setNodeSelector(NodeSelector.SKIP_DEDICATED_MASTERS)

    if (!userName.isNullOrBlank()) {
      restClientBuilder.setHttpClientConfigCallback {
        val basicCredentialsProvider = BasicCredentialsProvider()
        basicCredentialsProvider.setCredentials(AuthScope.ANY, UsernamePasswordCredentials(userName, userPassword))
        it.setDefaultCredentialsProvider(basicCredentialsProvider)
      }
    }

    restHighLevelClient = RestHighLevelClient(restClientBuilder)

    sniffer = Sniffer.builder(restHighLevelClient.lowLevelClient)
      .setSniffIntervalMillis(30_000)
      .build()
  }

  override fun existsAlias(index: String, currentAlias: String): Boolean {
    val alias = restHighLevelClient.indices().getAlias(GetAliasesRequest(currentAlias), RequestOptions.DEFAULT)
    val aliases = alias.aliases

    for (entry in aliases.entries) {
      logger.debug("index : ${entry.key} / aliasMeta : ${entry.value}")

      if (entry.key == index) {
        return true
      }
    }

    return false
  }

  override fun getIndexWithCurrentDate(index: String, tenant: String): String {
    return rotationStrategy.getIndexWithCurrentDate(index, tenant)
  }

  override fun getIndexWithDate(index: String, tenant: String, timestampMillis: Long): String {
    return rotationStrategy.getIndexWithDate(index, tenant, timestampMillis)
  }

  override fun getRangeIndex(index: String, tenant: String, from: Long, to: Long): Set<String> {
    return rotationStrategy.getRangeIndex(index, tenant, from, to)
  }

  override fun mget(multiGetRequest: MultiGetRequest, default: RequestOptions): MultiGetResponse {
    return restHighLevelClient.mget(multiGetRequest, default)
  }

  override fun getIndices(): GetIndexResponse {
    val request = GetIndexRequest().indices("*")
    return restHighLevelClient.indices().get(request, RequestOptions.DEFAULT)
  }

  override fun bulk(index: String, type: String, tenant: String, grapheneIndexRequests: List<GrapheneIndexRequest>, default: RequestOptions): BulkResponse {
    val bulkRequest = BulkRequest()
    for (grapheneIndexRequest in grapheneIndexRequests) {
      val indexRequest = IndexRequest(getIndexWithDate(index, tenant, grapheneIndexRequest.timestampMillis), type, grapheneIndexRequest.id)
      indexRequest.source(grapheneIndexRequest.source)
      bulkRequest.add(indexRequest)
    }
    return restHighLevelClient.bulk(bulkRequest, default)
  }

  override fun bulkAsync(index: String, type: String, tenant: String, grapheneIndexRequests: List<GrapheneIndexRequest>, default: RequestOptions) {
    val bulkRequest = BulkRequest()
    for (grapheneIndexRequest in grapheneIndexRequests) {
      val indexRequest = IndexRequest(getIndexWithDate(index, tenant, grapheneIndexRequest.timestampMillis), type, grapheneIndexRequest.id)
      indexRequest.source(grapheneIndexRequest.source)
      bulkRequest.add(indexRequest)
    }

    restHighLevelClient.bulkAsync(
      bulkRequest, default,
      ActionListener.wrap(
        {
          logger.info("Succeed to be in ${it.items.size} indexes")
        },
        {
          logger.error("Fail to index", it)
        }
      )
    )
  }

  override fun createIndexIfNotExists(index: String, tenant: String, from: Long?, to: Long?) {
    val rangeIndices = getRangeIndex(index, tenant, from!!, to!!)
    for (rangeIndex in rangeIndices) {
      if (restHighLevelClient.indices().exists(GetIndexRequest().indices(rangeIndex), RequestOptions.DEFAULT)) {
        continue
      }

      val createIndexRequest = CreateIndexRequest(rangeIndex)
      restHighLevelClient.indices().create(createIndexRequest, RequestOptions.DEFAULT)
    }
  }

  override fun createTemplateIfNotExists(templateIndexPattern: String, templateName: String, templateSource: String) {
    val putIndexTemplateRequest = PutIndexTemplateRequest(templateName)
    putIndexTemplateRequest.patterns(listOf(templateIndexPattern))
    putIndexTemplateRequest.source(templateSource, XContentType.JSON)

    restHighLevelClient.indices().putTemplate(putIndexTemplateRequest, RequestOptions.DEFAULT)
  }

  override fun close() {
    logger.info("Closing ES client")
    restHighLevelClient.close()
    sniffer.close()
    logger.info("Closed ES client")
  }
}
