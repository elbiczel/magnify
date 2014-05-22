package magnify.services.metrics


import scala.collection.JavaConversions._

import com.tinkerpop.blueprints.{Direction, Edge, Vertex}
import com.tinkerpop.pipes.PipeFunction
import magnify.features.{LoggedMetric, Metric}
import magnify.model.graph.{AsEdge, AsVertex, FullGraph}
import play.api.Logger

class ContributionMetric extends Metric {

  val logger = Logger(classOf[ContributionMetric].getSimpleName)

  override def apply(graph: FullGraph): FullGraph = {
    graph.edges.has("label", "commit").transform(new AsEdge).sideEffect(new GetClassContribution).iterate()
    graph.vertices.has("kind", "commit").transform(new AsVertex).sideEffect(new GetCommitContribution).iterate()
    graph.vertices.has("kind", "author").transform(new AsVertex).sideEffect(new GetAuthorContribution).iterate()
    graph
  }
}

class LoggedContributionMetric extends ContributionMetric with LoggedMetric

private[this] class GetClassContribution extends PipeFunction[Edge, Double] {
  override def compute(e: Edge): Double = {
    val newV = e.getVertex(Direction.IN)
    val oldV = e.getVertex(Direction.OUT)
    if (oldV.getProperty[String]("kind") != "class") { 0.0 } else {
      val newLOC = newV.getProperty[Double]("metric--lines-of-code")
      val oldLOC = oldV.getProperty[Double]("metric--lines-of-code")
      val contribution = Math.abs(newLOC - oldLOC)
      newV.setProperty("metric--contribution", contribution)
      contribution
    }
  }
}

private[this] class GetAggregatedContribution(dir: Direction, label: String, kind: String)
    extends PipeFunction[Vertex, Double] {
  override final def compute(v: Vertex): Double = {
    val individuals = v.getVertices(dir, label).toSeq
        .filter(_.getProperty[String]("kind") == kind)
    val aggregatedContribution = individuals.map(_.getProperty[Double]("metric--contribution")).sum
    v.setProperty("metric--contribution", aggregatedContribution)
    aggregatedContribution
  }
}

private[this] class GetCommitContribution extends GetAggregatedContribution(Direction.IN, "in-revision", "class")

private[this] class GetAuthorContribution extends GetAggregatedContribution(Direction.OUT, "committed", "commit")

