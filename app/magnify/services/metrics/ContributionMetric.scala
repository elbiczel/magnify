package magnify.services.metrics


import scala.collection.JavaConversions._

import com.tinkerpop.blueprints.{Direction, Edge}
import com.tinkerpop.pipes.PipeFunction
import magnify.features.{LoggedMetric, Metric}
import magnify.model.graph.{AsEdge, FullGraph}
import play.api.Logger
import com.tinkerpop.pipes.sideeffect.GroupByReducePipe

class ContributionMetric extends Metric {

  val logger = Logger(classOf[ContributionMetric].getSimpleName)

  override def apply(graph: FullGraph): FullGraph = {
    val groupAndReduceByPipe = new GroupByReducePipe[Edge, String, Long, Long](
      new GetRev, new GetClassContribution, new AggregateContributions)
    graph.edges.has("label", "commit").transform(new AsEdge).add(groupAndReduceByPipe).iterate()
    graph
  }
}

class LoggedContributionMetric extends ContributionMetric with LoggedMetric

private[this] class AggregateContributions extends PipeFunction[java.util.Iterator[Long], Long] {
  override def compute(list: java.util.Iterator[Long]): Long = list.toSeq.sum
}

private[this] class GetRev extends PipeFunction[Edge, String] {
  override def compute(e: Edge): String = e.getProperty[String]("rev")
}

private[this] class GetClassContribution extends PipeFunction[Edge, Long] {
  override def compute(e: Edge): Long = {
    val newV = e.getVertex(Direction.IN)
    val oldV = e.getVertex(Direction.OUT)
    if (oldV.getProperty[String]("kind") != "class") { 0L } else {
      val newLOC = newV.getProperty[Double]("metric--lines-of-code")
      val oldLOC = oldV.getProperty[Double]("metric--lines-of-code")
      Math.abs(newLOC - oldLOC).toLong
    }
  }
}

