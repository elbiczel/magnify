package magnify.services.metrics

import java.lang

import scala.concurrent.ExecutionContext

import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.pipes.PipeFunction
import com.tinkerpop.pipes.branch.LoopPipe.LoopBundle
import magnify.features.Metric
import magnify.model.graph.{Actions, FullGraph, TailCommitFilter}

abstract class AbstractRevWalkMetric[A] extends Metric {

  implicit val pool: ExecutionContext

  override def apply(g: FullGraph): FullGraph = {
    g.vertices.filter(TailCommitFilter).transform(transformation).loop(1, HasNextFilter).iterate()
    g
  }

  val transformation: RevisionTransformation[A]
}

trait RevisionTransformation[A] extends PipeFunction[Vertex, Vertex] with Actions {

  override def compute(v: Vertex): Vertex = {
    getRevisionClasses(v).sideEffect(calculateClassMetric(v)).iterate()
    getNextRevision(v).getOrElse(null)
  }

  private def calculateClassMetric(revision: Vertex) = new PipeFunction[Vertex, Unit] {
    override def compute(v: Vertex): Unit = {
      val value = metric(revision, v, getPrevRevision(v))
      v.setProperty("metric--" + metricName, value)
    }
  }

  def metric(revision: Vertex, current: Vertex, oParent: Option[Vertex]): A

  def metricName: String
}

private[this] object HasNextFilter extends PipeFunction[LoopBundle[Vertex], lang.Boolean] {
  override def compute(loop: LoopBundle[Vertex]): lang.Boolean = Option(loop.getObject).isDefined
}
