package magnify.services

import scala.collection.JavaConversions._

import japa.parser.ast.body.TypeDeclaration
import magnify.features._
import magnify.model.graph.{FullGraph, Graph}

sealed abstract class DependenciesResolvedMetricsProvider[A, B, C <: Metric[A, B]](
    metrics: java.util.Set[C])
  extends MetricsProvider[A, B, C] {

  // TODO(biczel): Add dependencies between metrics
  override def apply(): Seq[C] = metrics.toSeq
}

class FullGraphDependenciesResolvedMetricsProvider(
    metrics: java.util.Set[FullGraphMetric])
  extends DependenciesResolvedMetricsProvider[FullGraph, FullGraph, FullGraphMetric](metrics)

class RevisionDependenciesResolvedMetricsProvider(
    metrics: java.util.Set[RevisionMetric])
    extends DependenciesResolvedMetricsProvider[Graph, Graph, RevisionMetric](metrics)

class AstDependenciesResolvedMetricsProvider(
    metrics: java.util.Set[AstMetric])
    extends DependenciesResolvedMetricsProvider[TypeDeclaration, AnyRef, AstMetric](metrics)
