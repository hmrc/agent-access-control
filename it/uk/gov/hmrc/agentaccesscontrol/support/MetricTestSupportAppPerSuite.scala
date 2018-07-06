package uk.gov.hmrc.agentaccesscontrol.support

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import org.scalatest.Matchers
import org.scalatestplus.play.{OneAppPerSuite, OneServerPerTest}
import play.api.Application

import scala.collection.JavaConversions

trait MetricTestSupport extends Matchers {
  protected var metricsRegistry: MetricRegistry = _

  protected def givenCleanMetricRegistry(app: Application): Unit = {
    val registry = app.injector.instanceOf[Metrics].defaultRegistry
    for (metric <- JavaConversions.asScalaIterator[String](registry.getMetrics.keySet().iterator())) {
      registry.remove(metric)
    }
    metricsRegistry = registry
  }

  def timerShouldExistsAndBeenUpdated(metric: String): Unit =
    metricsRegistry.getTimers.get(s"Timer-$metric").getCount should be >= 1L

}

trait MetricTestSupportAppPerSuite extends MetricTestSupport {
  self: OneAppPerSuite =>

  def givenCleanMetricRegistry(): Unit = givenCleanMetricRegistry(app)
}

trait MetricTestSupportServerPerTest extends MetricTestSupport {
  self: OneServerPerTest =>

  def givenCleanMetricRegistry(): Unit = givenCleanMetricRegistry(app)
}
