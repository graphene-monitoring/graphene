package com.graphene.reader.config

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import java.net.InetAddress.getLocalHost
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class GrapheneReaderConfiguration {

  @Bean
  fun commonTags(): MeterRegistryCustomizer<MeterRegistry> {
    return MeterRegistryCustomizer {
      r: MeterRegistry ->
      r.config().commonTags(
        listOf(
          Tag.of("applicationName", "graphene-reader"),
          Tag.of("hostName", getLocalHost().hostName)
        )
      )
    }
  }

  @Bean
  fun corsConfigurer(): WebMvcConfigurer {
    return object : WebMvcConfigurer {
      override fun addCorsMappings(registry: CorsRegistry?) {
        registry!!.addMapping("/**")
      }
    }
  }
}
