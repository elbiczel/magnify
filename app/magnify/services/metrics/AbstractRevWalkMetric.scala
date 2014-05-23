package magnify.services.metrics

import java.lang

import scala.concurrent.ExecutionContext

import com.tinkerpop.blueprints.{Element, Vertex}
import com.tinkerpop.pipes.PipeFunction
import com.tinkerpop.pipes.branch.LoopPipe.LoopBundle
import magnify.features.Metric
import magnify.model.graph.{Actions, FullGraph, TailCommitFilter}

abstract class AbstractRevWalkMetric[A](transformation: RevisionTransformation[A]) extends Metric {

  implicit val pool: ExecutionContext

  override def apply(g: FullGraph): FullGraph = {
    g.vertices.filter(TailCommitFilter).transform(transformation).loop(1, HasNextFilter).iterate()
    aggregateMetric(g)
  }

  def aggregateMetric(g: FullGraph): FullGraph = g
}

trait RevisionTransformation[A] extends PipeFunction[Vertex, Vertex] with Actions {

  override final def compute(v: Vertex): Vertex = {
    getRevisionClasses(v).sideEffect(calculateClassMetric(v)).iterate()
    getNextRevision(v).getOrElse(null)
  }

  private def calculateClassMetric(revision: Vertex) = new PipeFunction[Vertex, Unit] {

    val revSha = revision.getProperty[String]("rev")

    override def compute(v: Vertex): Unit = {
      val wasUpdated = getPrevCommit(v).map(_.getProperty[String]("rev") == revSha).getOrElse(true)
      val value = metric(revision, v, getPrevRevision(v), wasUpdated)
      setMetricValue(metricName, v, value)
    }
  }

  def metric(revision: Vertex, current: Vertex, oParent: Option[Vertex], wasUpdated: Boolean): A =
    if (!wasUpdated) { getMetricValue(metricName, oParent.get) } else {
      metric(revision, current, oParent)
    }

  def metric(revision: Vertex, current: Vertex, oParent: Option[Vertex]): A

  def metricName: String
}

private[this] object HasNextFilter extends PipeFunction[LoopBundle[Vertex], lang.Boolean] {
  override def compute(loop: LoopBundle[Vertex]): lang.Boolean = Option(loop.getObject).isDefined
}
