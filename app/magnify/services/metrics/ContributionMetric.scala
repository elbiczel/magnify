package magnify.services.metrics


import scala.collection.JavaConversions._

import com.tinkerpop.blueprints.{Direction, Edge, Vertex}
import com.tinkerpop.gremlin.java.GremlinPipeline
import com.tinkerpop.pipes.PipeFunction
import magnify.features.{LoggedFunction, Metric, RevisionMetric}
import magnify.model.graph._
import play.api.Logger

class ContributionMetric extends Metric with Actions {

  val logger = Logger(classOf[ContributionMetric].getSimpleName)

  override def apply(graph: FullGraph): FullGraph = {
    graph.edges.has("label", "commit").transform(new AsEdge).sideEffect(new GetClassContribution).iterate()
    getRevisionClasses(graph.getTailCommitVertex).sideEffect(new PipeFunction[Vertex, Double] {
      override def compute(v: Vertex): Double = {
        val loc = getMetricValue[Double]("lines-of-code", v)
        setMetricValue("contribution", v, loc)
        loc
      }
    }).iterate()
    graph.vertices.has("kind", "commit").transform(new AsVertex).sideEffect(new GetCommitContribution).iterate()
    graph.vertices.has("kind", "author").transform(new AsVertex).sideEffect(new GetAuthorContribution).iterate()
    graph
  }
}

class LoggedContributionMetric extends ContributionMetric with LoggedFunction[FullGraph, FullGraph]

class RevisionContributionMetric extends RevisionMetric {

  val logger = Logger(classOf[RevisionContributionMetric].getSimpleName)

  override def apply(g: Graph): Graph = {
    g.vertices.has("kind", "package").transform(new AsVertex)
        .sideEffect(new GetAggregatedContribution(Direction.IN, "in-package", "class"))
        .iterate()
    g
  }
}

class LoggedRevisionContributionMetric extends RevisionContributionMetric with LoggedFunction[Graph, Graph]

private[this] class GetClassContribution extends PipeFunction[Edge, Double] with Actions {
  override def compute(e: Edge): Double = {
    val newV = e.getVertex(Direction.IN)
    val oldV = e.getVertex(Direction.OUT)
    if (oldV.getProperty[String]("kind") != "class") { 0.0 } else {
      val newLOC = getMetricValue[Double]("lines-of-code", newV)
      val oldLOC = getMetricValue[Double]("lines-of-code", oldV)
      val contribution = Math.abs(newLOC - oldLOC) + 1
      setMetricValue("contribution", newV, contribution)
      contribution
    }
  }
}

private[this] class GetAggregatedContribution(dir: Direction, label: String, kind: String)
    extends AggregatingMetricTransformation[Double](dir, label, kind, "contribution") {
  override final def metricValue(pipe: GremlinPipeline[Vertex, Vertex]): Double = {
    pipe.toList.toSeq.map(getMetricValue[Double]("contribution", _)).sum
  }
}

private[this] class GetCommitContribution extends GetAggregatedContribution(Direction.IN, "in-revision", "class")

private[this] class GetAuthorContribution extends GetAggregatedContribution(Direction.OUT, "committed", "commit")

