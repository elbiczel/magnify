package magnify.services.metrics

import java.lang

import com.tinkerpop.blueprints.{Vertex, Element}
import com.tinkerpop.pipes.PipeFunction
import magnify.features.{FullGraphMetric, MetricNames, RevisionMetric}
import magnify.model.graph.{Actions, FullGraph, Graph}
import play.api.Logger
import com.tinkerpop.pipes.filter.OrFilterPipe
import com.tinkerpop.gremlin.pipes.filter.PropertyFilterPipe
import com.tinkerpop.pipes.filter.FilterPipe.Filter

class DistinctAuthorsMetric extends FullGraphMetric {

  final val logger = Logger(classOf[DistinctAuthorsMetric].getSimpleName)

  override final val name: String = MetricNames.distinctAuthors

  override final val dependencies: Set[String] = Set(MetricNames.experience)

  override def apply(graph: FullGraph): FullGraph = {
    graph.vertices
        .add(new OrFilterPipe[Vertex](
          new PropertyFilterPipe[Vertex, String]("kind", "commit", Filter.EQUAL),
          new PropertyFilterPipe[Vertex, String]("kind", "class", Filter.EQUAL)))
        .sideEffect(new CalculateDistinctAuthors)
        .iterate()
    graph
  }
}

class PackageDistinctAuthorsMetric extends RevisionMetric {

  final val logger = Logger(classOf[PackageDistinctAuthorsMetric].getSimpleName)

  override def apply(graph: Graph): Graph = {
    graph.vertices.has("kind", "package")
        .sideEffect(new CalculateDistinctAuthors)
        .iterate()
    graph
  }

  override final val name: String = MetricNames.distinctAuthors

  override final val dependencies: Set[String] = Set(MetricNames.experience)
}

private[this] class CalculateDistinctAuthors[T <: Element] extends PipeFunction[T, Int] with Actions {
  override def compute(v: T): Int = {
    val exp = getMetricValue[Map[String, Double]](MetricNames.experience, v)
    val authors: lang.Integer = exp.size
    setMetricValue[lang.Integer](MetricNames.distinctAuthors, v, authors)
    authors
  }
}
