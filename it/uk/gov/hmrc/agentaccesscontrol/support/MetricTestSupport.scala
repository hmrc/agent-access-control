package uk.gov.hmrc.agentaccesscontrol.support

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import org.scalatest.Matchers
import org.scalatestplus.play.OneAppPerSuite

import scala.collection.JavaConversions

trait MetricTestSupport {
  self: OneAppPerSuite with Matchers =>

  private var metricsRegistry: MetricRegistry = _

  def givenCleanMetricRegistry(): Unit = {
    val registry = app.injector.instanceOf[Metrics].defaultRegistry
    for (metric <- JavaConversions.asScalaIterator[String](registry.getMetrics.keySet().iterator())) {
      registry.remove(metric)
    }
    metricsRegistry = registry
  }

  def timerShouldExistsAndBeenUpdated(metric: String): Unit = {
    metricsRegistry.getTimers.get(s"Timer-$metric").getCount should be >= 1L
  }

}
