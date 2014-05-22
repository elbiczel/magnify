package magnify.model.graph

import scala.collection.JavaConversions._

import java.lang

import com.tinkerpop.blueprints.{Direction, Edge, Element, Vertex}
import com.tinkerpop.pipes.PipeFunction
import com.tinkerpop.pipes.branch.LoopPipe.LoopBundle

trait ExtremeVertexFilter extends PipeFunction[Vertex, lang.Boolean] {

  object Extreme extends Enumeration {
    type Extreme = Value
    val Head = Value("OUT")
    val Tail = Value("IN")
  }

  override final def compute(v: Vertex): lang.Boolean =
    if (v.getProperty[String]("kind") != kind) { false } else {
      !v.getEdges(Direction.valueOf(extreme.toString), "commit").iterator().hasNext
    }

  def kind: String

  def extreme: Extreme.Extreme
}

object HeadCommitFilter extends ExtremeVertexFilter {

  override def extreme: Extreme.Extreme = Extreme.Head

  override def kind: String = "commit"
}

object TailCommitFilter extends ExtremeVertexFilter {

  override def extreme: Extreme.Extreme = Extreme.Tail

  override def kind: String = "commit"
}

object TrueFilter extends PipeFunction[LoopBundle[Vertex], lang.Boolean] {
  override def compute(argument: LoopBundle[Vertex]): lang.Boolean = true
}

object NoNewVertexFilter extends PipeFunction[Vertex, lang.Boolean] {
  override def compute(v: Vertex): lang.Boolean = {
    !v.getVertices(Direction.OUT, "commit").iterator().hasNext
  }
}

case class HasInFilter[T <: Element](property: String, values: Set[String])
    extends PipeFunction[T, lang.Boolean] {
  override def compute(element: T): lang.Boolean = values.contains(element.getProperty(property))
}

case class NotFilter[T <: Element](filter: PipeFunction[T, lang.Boolean])
    extends PipeFunction[T, lang.Boolean] {
  override def compute(argument: T): lang.Boolean = !filter.compute(argument)
}

class AsVertex[T <: Element] extends PipeFunction[T, Vertex] {
  override def compute(argument: T): Vertex = argument.asInstanceOf[Vertex]
}

class AsEdge[T <: Element] extends PipeFunction[T, Edge] {
  override def compute(argument: T): Edge = argument.asInstanceOf[Edge]
}

class HasVertexToFilter(dir: Direction, label: String, vertices: Set[Vertex])
    extends PipeFunction[Vertex, lang.Boolean] {
  override def compute(v: Vertex): lang.Boolean = v.getVertices(dir, label).iterator().toSet.exists(vertices)
}
