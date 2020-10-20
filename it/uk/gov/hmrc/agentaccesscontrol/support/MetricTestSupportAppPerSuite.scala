package uk.gov.hmrc.agentaccesscontrol.support

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import org.scalatest.{Assertion, Matchers}
import org.scalatestplus.play.guice.{GuiceOneAppPerSuite, GuiceOneServerPerTest}
import play.api.Application

import scala.collection.JavaConverters._

trait MetricTestSupport extends Matchers {
  protected var metricsRegistry: MetricRegistry = _

  protected def givenCleanMetricRegistry(app: Application): Unit = {
    val registry = app.injector.instanceOf[Metrics].defaultRegistry
    for (metric <- registry.getMetrics.keySet().iterator().asScala) {
      registry.remove(metric)
    }
    metricsRegistry = registry
  }

  def timerShouldExistsAndBeenUpdated(metric: String): Assertion =
    metricsRegistry.getTimers.get(s"Timer-$metric").getCount should be >= 1L

}

trait MetricTestSupportAppPerSuite extends MetricTestSupport {
  self: GuiceOneAppPerSuite =>

  def givenCleanMetricRegistry(): Unit = givenCleanMetricRegistry(app)
}

trait MetricTestSupportServerPerTest extends MetricTestSupport {
  self: GuiceOneServerPerTest =>

  def givenCleanMetricRegistry(): Unit = givenCleanMetricRegistry(app)
}
