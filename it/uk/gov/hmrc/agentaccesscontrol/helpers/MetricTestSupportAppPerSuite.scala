package uk.gov.hmrc.agentaccesscontrol.helpers

import com.codahale.metrics.MetricRegistry
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.{GuiceOneAppPerSuite, GuiceOneServerPerTest}
import play.api.Application
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import scala.jdk.CollectionConverters._

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