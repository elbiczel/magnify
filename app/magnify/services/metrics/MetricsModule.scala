package magnify.services.metrics

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import magnify.common.reflect._
import magnify.features.{AstMetric, Metric, RevisionMetric}

private[services] class MetricsModule extends AbstractModule {
  override def configure(): Unit = {
    // TODO(biczel): Add dependencies between metrics
    val metricsBinder = Multibinder.newSetBinder(binder(), classOf[Metric])
    metricsBinder.addBinding().toConstructor(constructor[LoggedExperienceMetric])
    metricsBinder.addBinding().toConstructor(constructor[LoggedContributionMetric])
    metricsBinder.addBinding().toConstructor(constructor[LoggedAggregatedContributionMetric])
    metricsBinder.addBinding().toConstructor(constructor[LoggedMcCabeCyclomaticComplexityMetric])
    val pkgMetricsBinder = Multibinder.newSetBinder(binder(), classOf[RevisionMetric])
    pkgMetricsBinder.addBinding().toConstructor(constructor[LoggedRevisionLocMetric])
    pkgMetricsBinder.addBinding().toConstructor(constructor[LoggedRevisionAvgLocMetric])
    pkgMetricsBinder.addBinding().toConstructor(constructor[LoggedRevisionExperienceMetric])
    pkgMetricsBinder.addBinding().toConstructor(constructor[LoggedRevisionContributionMetric])
    pkgMetricsBinder.addBinding().toConstructor(constructor[LoggedRevisionAggregatedContributionMetric])
    pkgMetricsBinder.addBinding().toConstructor(constructor[LoggedRevisionMcCabeCyclomaticComplexityMetric])
    val astMetricsBinder = Multibinder.newSetBinder(binder(), classOf[AstMetric])
    astMetricsBinder.addBinding().toConstructor(constructor[ClassMcCabeCyclomaticComplexityMetric])
  }
}
