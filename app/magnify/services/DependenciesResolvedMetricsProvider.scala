package magnify.services

import scala.collection.mutable
import scala.collection.JavaConversions._

import japa.parser.ast.body.TypeDeclaration
import magnify.features._
import magnify.model.graph.{FullGraph, Graph}

sealed abstract class DependenciesResolvedMetricsProvider[A, B, C <: Metric[A, B]](
    metrics: java.util.Set[C])
  extends MetricsProvider[A, B, C] {

  private val metricByName = metrics.map((metric) => (metric.name -> metric)).toMap
  private val adding = mutable.Set[String]()
  private val addedMetrics = mutable.Set[String]()

  override def apply(): Seq[C] = {
    addedMetrics.clear()
    adding.clear()
    metrics.foldLeft(Seq[C]()) { (metricsSeq, metric) =>
      addMetric(metric, metricsSeq)
    }
  }

  private def addMetric(metric: C, metrics: Seq[C]): Seq[C] = if (addedMetrics(metric.name)) { metrics } else {
    if (adding(metric.name)) { throw new IllegalStateException("Cycle in dependencies for: " + metric.name) }
    adding.add(metric.name)
    val dependencies = metric.dependencies.map((dependencyName) => metricByName.getOrElse(dependencyName, {
      throw new IllegalStateException("Unresolved dependency: " + dependencyName + " for " + metric.name)
    }))
    val withDependencies = dependencies.foldLeft(metrics) { (metricsSeq, dependency) =>
      addMetric(dependency, metricsSeq)
    }
    adding.remove(metric.name)
    addedMetrics.add(metric.name)
    withDependencies ++ Seq(metric)
  }
}

class FullGraphDependenciesResolvedMetricsProvider(
    metrics: java.util.Set[FullGraphMetric])
  extends DependenciesResolvedMetricsProvider[FullGraph, FullGraph, FullGraphMetric](metrics)

class RevisionDependenciesResolvedMetricsProvider(
    metrics: java.util.Set[RevisionMetric])
    extends DependenciesResolvedMetricsProvider[Graph, Graph, RevisionMetric](metrics)

class AstDependenciesResolvedMetricsProvider(
    metrics: java.util.Set[AstMetric])
    extends DependenciesResolvedMetricsProvider[TypeDeclaration, AnyRef , AstMetric](metrics)
