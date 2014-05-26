package magnify.model.graph

import scala.collection.JavaConversions._

import com.tinkerpop.blueprints.{Edge, Vertex}
import com.tinkerpop.gremlin.pipes.filter.{LabelFilterPipe, PropertyFilterPipe}
import com.tinkerpop.pipes.filter.OrFilterPipe
import com.tinkerpop.pipes.filter.FilterPipe.Filter
import magnify.features.RevisionGraphFactory

final class FullGraphView(graph: Graph) extends GraphView {
  override def edges: Iterable[Edge] =
    graph.edges
        .add(imports)
        .toList

  override def vertices: Iterable[Vertex] =
    graph.vertices
        .add(packagesOrClasses)
        .toList

  private val packagesOrClasses =
    new OrFilterPipe[Vertex](
      new PropertyFilterPipe[Vertex, String]("kind", "package", Filter.EQUAL),
      new PropertyFilterPipe[Vertex, String]("kind", "class", Filter.EQUAL))

  private val imports =
    new OrFilterPipe[Edge](
      new LabelFilterPipe("cls-imports-cls", Filter.EQUAL),
      new LabelFilterPipe("pkg-imports-pkg", Filter.EQUAL),
      new LabelFilterPipe("cls-imports-pkg", Filter.EQUAL),
      new LabelFilterPipe("pkg-imports-cls", Filter.EQUAL),
      new LabelFilterPipe("in-package", Filter.EQUAL),
      new LabelFilterPipe("cls-in-pkg", Filter.EQUAL),
      new LabelFilterPipe("calls", Filter.EQUAL))
}

final class FullGraphViewFactory(revisionGraphFactory: RevisionGraphFactory) extends GraphViewFactory {
  override def apply(graph: FullGraph, revision: Option[String]): FullGraphView = {
    new FullGraphView(revisionGraphFactory(graph, revision))
  }
}
