package uk.gov.hmrc.agentaccesscontrol.utils

import scala.jdk.CollectionConverters._

import com.codahale.metrics.MetricRegistry
import org.scalatest.matchers.should.Matchers
import org.scalatest.Assertion
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

trait MetricTestSupport extends Matchers {
  self: GuiceOneServerPerSuite =>

  private val registry: MetricRegistry = app.injector.instanceOf[Metrics].defaultRegistry
  def cleanMetricRegistry(): Unit =
    for (metric <- registry.getMetrics.keySet().iterator().asScala) {
      registry.remove(metric)
    }

  def timerShouldExistAndHasBeenUpdated(metric: String): Assertion =
    registry.getTimers.get(s"Timer-$metric").getCount should be >= 1L

}
