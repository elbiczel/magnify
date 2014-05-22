package magnify.services.metrics

import java.lang

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext

import com.tinkerpop.blueprints.{Direction, Vertex}
import com.tinkerpop.pipes.PipeFunction
import com.tinkerpop.pipes.branch.LoopPipe.LoopBundle
import magnify.features.Metric
import magnify.model.graph.{FullGraph, TailCommitFilter}

abstract class AbstractRevWalkMetric[A] extends Metric {

  implicit val pool: ExecutionContext

  override def apply(g: FullGraph): FullGraph = {
    g.vertices.filter(TailCommitFilter).transform(transformation).loop(1, HasNextFilter).iterate()
    g
  }

  val transformation: RevisionTransformation[A]
}

trait RevisionTransformation[A] extends PipeFunction[Vertex, Vertex] with NextCommit {

  override def compute(v: Vertex): Vertex = {
    val classes = v.getVertices(Direction.IN, "in-revision").toSeq
        .filter(_.getProperty[String]("kind") eq "class")
    classes.foreach(calculateClassMetric(v))
    nextCommit(v)
  }

  private def calculateClassMetric(revision: Vertex)(v: Vertex): Unit = {
    val prevIterator = v.getVertices(Direction.IN, "commit").iterator()
    val prevOption = if (prevIterator.hasNext) Option(prevIterator.next()) else None
    val value = metric(revision, v, prevOption)
    v.setProperty("metric--" + metricName, value)
    v
  }

  def getName(authorWithTime: String): String = {
    val endEmailIndex = authorWithTime.lastIndexOf('<')
    authorWithTime.substring(0, endEmailIndex - 1)
  }

  def metric(revision: Vertex, current: Vertex, oParent: Option[Vertex]): A

  def metricName: String
}

private[this] object HasNextFilter extends PipeFunction[LoopBundle[Vertex], lang.Boolean] with NextCommit {
  override def compute(loop: LoopBundle[Vertex]): lang.Boolean = hasNextCommit(loop.getObject)
}

trait NextCommit {
  def hasNextCommit(v: Vertex): Boolean = Option(v).isDefined

  def nextCommit(v: Vertex): Vertex = Option(v).map(nextIterator(_).next()).getOrElse(null)

  private def nextIterator(v: Vertex): java.util.Iterator[Vertex] = v.getVertices(Direction.OUT, "commit").iterator()
}
