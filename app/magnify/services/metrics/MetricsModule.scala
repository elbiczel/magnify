package magnify.services.metrics

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import magnify.features.{RevisionMetric, Metric}
import magnify.common.reflect._

private[services] class MetricsModule extends AbstractModule {
  override def configure(): Unit = {
    // TODO(biczel): Add dependencies between metrics
    // TODO(biczel): Add CC metric:
    // http://gmetrics.sourceforge.net/gmetrics-CyclomaticComplexityMetric.html
    // http://stackoverflow.com/questions/12355783/tools-to-automate-calculation-of-cyclomatic-complexity-in-java
    // http://stackoverflow.com/questions/13571635/how-to-calculate-cyclomatic-complexity-of-a-project-not-a-class-function
    val metricsBinder = Multibinder.newSetBinder(binder(), classOf[Metric])
    metricsBinder.addBinding().toConstructor(constructor[LoggedExperienceMetric])
    metricsBinder.addBinding().toConstructor(constructor[LoggedContributionMetric])
    val pkgMetricsBinder = Multibinder.newSetBinder(binder(), classOf[RevisionMetric])
    pkgMetricsBinder.addBinding().toConstructor(constructor[LoggedRevisionLocMetric])
    pkgMetricsBinder.addBinding().toConstructor(constructor[LoggedRevisionAvgLocMetric])
    pkgMetricsBinder.addBinding().toConstructor(constructor[LoggedRevisionContributionMetric])
    pkgMetricsBinder.addBinding().toConstructor(constructor[LoggedRevisionExperienceMetric])
  }
}
