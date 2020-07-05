package com.graphene.reader.controller.graphite

import com.graphene.common.HierarchyMetricPaths
import com.graphene.reader.controller.graphite.request.MetricsFindRequest
import com.graphene.reader.controller.graphite.request.RenderRequest
import com.graphene.reader.controller.graphite.request.TagsAutoCompleteRequest
import com.graphene.reader.handler.RenderHandler
import com.graphene.reader.handler.RenderParameters
import com.graphene.reader.service.index.KeySearchHandler
import com.graphene.reader.service.tag.TagSearchHandler
import com.graphene.reader.utils.MetricRule
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

/**
 *
 * This controller provides Graphite api like below:
 * https://graphite-api.readthedocs.io/en/latest/api.html
 *
 * @since 1.0.0
 * @author dark
 */
@RestController
class GraphiteController(
  private val renderHandler: RenderHandler,
  private val keySearchHandler: KeySearchHandler,
  private val tagSearchHandler: TagSearchHandler
) {

  @PostMapping("/render")
  fun postRender(
    @ModelAttribute("renderRequest") renderRequest: RenderRequest
  ): ResponseEntity<*> {
    return renderHandler.handle(RenderParameters.from(renderRequest))
  }

  @RequestMapping("/metrics/find", "/find", method = [RequestMethod.POST, RequestMethod.GET])
  fun metricsFindAboutFormUrlEncoded(
    @ModelAttribute("metricsFindRequest") metricsFindRequest: MetricsFindRequest
  ): Collection<HierarchyMetricPaths.HierarchyMetricPath> {

    return getPathsAsHierarchyMetricPath(metricsFindRequest)
  }

  @RequestMapping("/tags/autoComplete/tags", method = [RequestMethod.GET])
  fun getTagsAutoComplete(
    @ModelAttribute("tagsAutoCompleteRequest") tagsAutoCompleteRequest: TagsAutoCompleteRequest
  ): Collection<String> {
    return tagSearchHandler.getTags(
      tagsAutoCompleteRequest.tagPrefix,
      tagsAutoCompleteRequest.expr,
      tagsAutoCompleteRequest.from * 1_000,
      tagsAutoCompleteRequest.until * 1_000
    )
  }

  @RequestMapping("/tags/autoComplete/values", method = [RequestMethod.GET])
  fun getTagValuesAutoComplete(
    @ModelAttribute("tagsAutoCompleteRequest") tagsAutoCompleteRequest: TagsAutoCompleteRequest
  ): Collection<String> {
    return tagSearchHandler.getTagValues(
      tagsAutoCompleteRequest.valuePrefix,
      tagsAutoCompleteRequest.expr,
      tagsAutoCompleteRequest.tag,
      tagsAutoCompleteRequest.from * 1_000,
      tagsAutoCompleteRequest.until * 1_000
    )
  }

  private fun getPathsAsHierarchyMetricPath(metricsFindRequest: MetricsFindRequest): Collection<HierarchyMetricPaths.HierarchyMetricPath> {
    return keySearchHandler.getHierarchyMetricPaths(
      MetricRule.defaultTenant(),
      metricsFindRequest.query,
      metricsFindRequest.fromMillis(),
      metricsFindRequest.untilMillis()
    )
  }
}
