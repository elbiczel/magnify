package magnify.model.graph

import scala.collection.JavaConversions._

import com.tinkerpop.blueprints.{Edge, Vertex}
import com.tinkerpop.gremlin.pipes.filter.{LabelFilterPipe, PropertyFilterPipe}
import com.tinkerpop.pipes.filter.FilterPipe.Filter
import magnify.features.RevisionGraphFactory

/**
 * @author Cezary Bartoszuk (cezary@codilime.com)
 */
final class PackageImportsGraphView(graph: Graph) extends GraphView {

  override def vertices: Iterable[Vertex] =
    graph.vertices
        .add(packages)
        .toList

  private val packages =
    new PropertyFilterPipe[Vertex, String]("kind", "package", Filter.EQUAL)

  override def edges: Iterable[Edge] =
    graph.edges
        .add(imports)
        .toList

  private val imports =
    new LabelFilterPipe("pkg-imports-pkg", Filter.EQUAL)
}


final class PackageImportsGraphViewFactory(revisionGraphFactory: RevisionGraphFactory)
    extends GraphViewFactory {

  def apply(graph: FullGraph, revision: Option[String]): PackageImportsGraphView = {
    new PackageImportsGraphView(revisionGraphFactory(graph, revision))
  }
}
