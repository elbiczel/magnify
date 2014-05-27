package magnify.services.metrics


import scala.collection.JavaConversions._

import com.tinkerpop.blueprints.{Direction, Edge, Vertex}
import com.tinkerpop.gremlin.java.GremlinPipeline
import com.tinkerpop.pipes.PipeFunction
import magnify.features.{FullGraphMetric, LoggedFunction, MetricNames, RevisionMetric}
import magnify.model.graph._
import play.api.Logger

class ContributionMetric extends FullGraphMetric with Actions {

  val logger = Logger(classOf[ContributionMetric].getSimpleName)

  override def apply(graph: FullGraph): FullGraph = {
    graph.edges.has("label", "commit").transform(new AsEdge).sideEffect(new GetClassContribution).iterate()
    graph.vertices.has("kind", "commit").transform(new AsVertex).sideEffect(new GetCommitContribution).iterate()
    graph.vertices.has("kind", "author").transform(new AsVertex).sideEffect(new GetAuthorContribution).iterate()
    graph
  }

  override final val name: String = MetricNames.contribution

  override final val dependencies: Set[String] = Set(MetricNames.linesOfCode)
}

class LoggedContributionMetric extends ContributionMetric with LoggedFunction[FullGraph, FullGraph]

class RevisionContributionMetric extends RevisionMetric {

  val logger = Logger(classOf[RevisionContributionMetric].getSimpleName)

  override def apply(g: Graph): Graph = {
//    g.vertices.has("kind", "package").transform(new AsVertex)
//        .sideEffect(new GetAggregatedContribution(Direction.IN, "cls-in-pkg", "class"))
//        .iterate()
    g
  }

  override final val name: String = MetricNames.contribution
}

class LoggedRevisionContributionMetric extends RevisionContributionMetric with LoggedFunction[Graph, Graph]

private[this] class GetCommitContribution extends PipeFunction[Vertex, Double] with Actions {
  override def compute(v: Vertex): Double = {
    val individual = getRevisionClasses(v)
        .transform(new PipeFunction[Vertex, Double] {
          override def compute(v: Vertex): Double = {
            val inCommit = v.getEdges(Direction.IN, "commit").toSeq
            if (!inCommit.isEmpty) {
              val commitE = inCommit.head
              if (commitE.getProperty[String]("rev") == v.getProperty[String]("rev")) {
                getMetricValue[Double](MetricNames.contribution, inCommit.head)
              } else {
                0.0
              }
            } else {
              getMetricValue[Double](MetricNames.linesOfCode, v) + 5
            }
          }
        })
        .toList.toSeq
    val sum = individual.sum
    setMetricValue(MetricNames.contribution, v, sum)
    sum
  }
}

private[this] class GetClassContribution extends PipeFunction[Edge, Double] with Actions {
  override def compute(e: Edge): Double = {
    val newV = e.getVertex(Direction.IN)
    val oldV = e.getVertex(Direction.OUT)
    if (oldV.getProperty[String]("kind") != "class") { 0.0 } else {
      val newLOC = getMetricValue[Double](MetricNames.linesOfCode, newV)
      val oldLOC = getMetricValue[Double](MetricNames.linesOfCode, oldV)
      val contribution = Math.abs(newLOC - oldLOC) + 2
      setMetricValue(MetricNames.contribution, e, contribution)
      contribution
    }
  }
}

private[this] class GetAuthorContribution
    extends AggregatingMetricTransformation[Double](Direction.OUT, "committed", "commit", MetricNames.contribution) {
  override final def metricValue(pipe: GremlinPipeline[Vertex, Vertex]): Double = {
    pipe.toList.toSeq.map(getMetricValue[Double](metricName, _)).sum
  }
}
