package com.graphene.writer.config

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import java.net.InetAddress.getLocalHost
import java.util.concurrent.Executor
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
class GrapheneWriterConfiguration {

  @Bean
  fun commonTags(): MeterRegistryCustomizer<MeterRegistry> {
    return MeterRegistryCustomizer {
      r: MeterRegistry ->
      r.config().commonTags(
        listOf(
          Tag.of("applicationName", "graphene-writer"),
          Tag.of("hostName", getLocalHost().hostName)
        )
      )
    }
  }

  @Bean
  fun grapheneProcessorExecutor(): Executor {
    val executor = ThreadPoolTaskExecutor()
    executor.corePoolSize = Runtime.getRuntime().availableProcessors() * 2
    executor.maxPoolSize = Runtime.getRuntime().availableProcessors() * 2
    executor.setQueueCapacity(4 * 1024 * 1024)
    executor.threadNamePrefix = "GrapheneProcessorExecutor-"
    executor.setWaitForTasksToCompleteOnShutdown(true)
    executor.setAwaitTerminationSeconds(30)
    executor.initialize()
    return executor
  }
}
