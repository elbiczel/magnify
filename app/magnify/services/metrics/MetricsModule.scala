package magnify.services.metrics

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import magnify.common.reflect._
import magnify.features.{AstMetric, FullGraphMetric, MetricNames, RevisionMetric}
import magnify.model.graph.Graph

private[services] class MetricsModule extends AbstractModule {
  override def configure(): Unit = {
    val metricsBinder = Multibinder.newSetBinder(binder(), classOf[FullGraphMetric])
    metricsBinder.addBinding().toConstructor(constructor[LoggedExperienceMetric])
    metricsBinder.addBinding().toConstructor(constructor[LoggedContributionMetric])
    metricsBinder.addBinding().toConstructor(constructor[LoggedAggregatedContributionMetric])
    metricsBinder.addBinding().toConstructor(constructor[LoggedMcCabeCyclomaticComplexityMetric])
    metricsBinder.addBinding().toConstructor(constructor[CommitLocMetric])
    metricsBinder.addBinding().toConstructor(constructor[DistinctAuthorsMetric])
    val pkgMetricsBinder = Multibinder.newSetBinder(binder(), classOf[RevisionMetric])
    pkgMetricsBinder.addBinding().toConstructor(constructor[LoggedRevisionLocMetric])
    pkgMetricsBinder.addBinding().toConstructor(constructor[LoggedRevisionAvgLocMetric])
    pkgMetricsBinder.addBinding().toConstructor(constructor[LoggedRevisionExperienceMetric])
    pkgMetricsBinder.addBinding().toConstructor(constructor[LoggedRevisionContributionMetric])
    pkgMetricsBinder.addBinding().toConstructor(constructor[LoggedRevisionAggregatedContributionMetric])
    pkgMetricsBinder.addBinding().toConstructor(constructor[LoggedRevisionMcCabeCyclomaticComplexityMetric])
    pkgMetricsBinder.addBinding().toConstructor(constructor[DummyRevisionLocMetric])
    pkgMetricsBinder.addBinding().toConstructor(constructor[DummyRevisionPageRankMetric])
    pkgMetricsBinder.addBinding().toConstructor(constructor[PackageDistinctAuthorsMetric])
    val astMetricsBinder = Multibinder.newSetBinder(binder(), classOf[AstMetric])
    astMetricsBinder.addBinding().toConstructor(constructor[ClassMcCabeCyclomaticComplexityMetric])
  }
}

private[this] final class DummyRevisionLocMetric extends RevisionMetric {
  override def name: String = MetricNames.linesOfCode

  override def apply(g: Graph): Graph = g
}

private[this] final class DummyRevisionPageRankMetric extends RevisionMetric {
  override def name: String = MetricNames.pageRank

  override def apply(g: Graph): Graph = g
}
