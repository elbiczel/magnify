package magnify.services.metrics

import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.pipes.PipeFunction
import magnify.features.{FullGraphMetric, LoggedFunction, MetricNames, RevisionMetric}
import magnify.model.graph.{Actions, AsVertex, FullGraph, Graph}
import play.api.Logger


abstract class OwnerMetric extends FullGraphMetric {

  val logger = Logger(classOf[OwnerMetric].getSimpleName)

  override def apply(graph: FullGraph): FullGraph = {
    graph.vertices.has("kind", "commit").transform(new AsVertex).sideEffect(GetOwner).iterate()
    graph.vertices.has("kind", "class").transform(new AsVertex).sideEffect(GetOwner).iterate()
    graph
  }

  override final val name: String = GetOwner.name

  override final val dependencies: Set[String] = Set(GetOwner.dependency)
}

class LoggedOwnerMetric extends OwnerMetric with LoggedFunction[FullGraph, FullGraph]

abstract class RevisionOwnerMetric extends RevisionMetric {

  val logger = Logger(classOf[RevisionOwnerMetric].getSimpleName)

  protected def apply(g: Graph): Graph = {
    g.vertices.has("kind", "package").transform(new AsVertex).sideEffect(GetOwner).iterate()
    g
  }

  override final val name: String = GetOwner.name

  override final val dependencies: Set[String] = Set(GetOwner.dependency)
}

class LoggedRevisionOwnerMetric extends RevisionOwnerMetric with LoggedFunction[(Graph, Vertex), Graph]

private object GetOwner extends PipeFunction[Vertex, String] with Actions {
  override def compute(vertex: Vertex): String = {
    val dependencies: Map[String, Double] = getMetricValue(dependency, vertex)
    val owner = if (dependencies.isEmpty) "" else dependencies.maxBy(_._2)._1
    setMetricValue(name, vertex, owner)
    owner
  }

  val name: String = MetricNames.owner
  val dependency: String = MetricNames.aggregatedContribution
}
