package magnify.model.graph

import scala.collection.JavaConversions._

import com.tinkerpop.blueprints.{Edge, Vertex}
import com.tinkerpop.gremlin.pipes.filter.{LabelFilterPipe, PropertyFilterPipe}
import com.tinkerpop.pipes.filter.FilterPipe.Filter
import magnify.features.RevisionGraphFactory

/**
 * @author Cezary Bartoszuk (cezary@codilime.com)
 */
final class PackagesGraphView(graph: Graph) extends GraphView {

  override def vertices: Iterable[Vertex] =
    graph.vertices
        .add(packages)
        .toList

  private val packages =
    new PropertyFilterPipe[Vertex, String]("kind", "package", Filter.EQUAL)

  override def edges: Iterable[Edge] =
    graph.edges
        .add(inPackage)
        .toList

  private val inPackage =
    new LabelFilterPipe("in-package", Filter.EQUAL)
}

final class PackagesGraphViewFactory(revisionGraphFactory: RevisionGraphFactory) extends GraphViewFactory {

  def apply(graph: FullGraph, revision: Option[String]): PackagesGraphView = {
    new PackagesGraphView(revisionGraphFactory(graph, revision))
  }
}
