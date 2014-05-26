package magnify.model.graph

import scala.collection.JavaConversions._

import com.tinkerpop.blueprints.{Edge, Vertex}
import com.tinkerpop.gremlin.pipes.filter.{LabelFilterPipe, PropertyFilterPipe}
import com.tinkerpop.pipes.filter.FilterPipe.Filter
import magnify.features.RevisionGraphFactory

final class ClassImportsGraphView(graph: Graph) extends GraphView {

  override def vertices: Iterable[Vertex] =
    graph.vertices
        .add(classes)
        .toList

  private val classes =
    new PropertyFilterPipe[Vertex, String]("kind", "class", Filter.EQUAL)

  override def edges: Iterable[Edge] =
    graph.edges
        .add(imports)
        .toList

  private val imports =
    new LabelFilterPipe("cls-imports-cls", Filter.EQUAL)
}

final class ClassImportsGraphViewFactory(revisionGraphFactory: RevisionGraphFactory)
    extends GraphViewFactory {

  def apply(graph: FullGraph, revision: Option[String]): ClassImportsGraphView = {
    new ClassImportsGraphView(revisionGraphFactory(graph, revision))
  }
}
