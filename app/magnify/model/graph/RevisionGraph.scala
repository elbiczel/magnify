package magnify.model.graph

import scala.collection.JavaConversions._
import scala.collection.mutable

import com.tinkerpop.blueprints.{Direction, Edge, Graph => BlueprintsGraph, Vertex}
import com.tinkerpop.blueprints.impls.tg.TinkerGraph
import com.tinkerpop.gremlin.java.GremlinPipeline
import com.tinkerpop.pipes.PipeFunction

class RevisionGraph(val blueprintsGraph: BlueprintsGraph) extends Graph {
  override def vertices: GremlinPipeline[Vertex, Vertex] =
    new GremlinPipeline(blueprintsGraph.getVertices, true)

  override def edges: GremlinPipeline[Edge, Edge] =
    new GremlinPipeline(blueprintsGraph.getEdges, true)
}

object RevisionGraph {

  val LABELS: Seq[String] = Seq("imports", "in-package", "package-imports", "calls")

  def apply(full: FullGraph, revVertex: Vertex): Graph = {
    val tinker = new TinkerGraph
    revVertices(revVertex).sideEffect(new PipeFunction[Vertex, Vertex] {
      override def compute(argument: Vertex): Vertex = {
        val copy = tinker.addVertex(argument.getId)
        argument.getPropertyKeys.foreach((key) => copy.setProperty(key, argument.getProperty(key)))
        argument
      }
    }).iterate()
    revVertices(revVertex).sideEffect(new PipeFunction[Vertex, Vertex] {
      override def compute(argument: Vertex): Vertex = {
        val copy = tinker.getVertex(argument.getId)
        argument.getEdges(Direction.OUT, LABELS :_*).foreach { (outEdge) =>
          val inVertexId = outEdge.getVertex(Direction.IN).getId
          val copyEdge = copy.addEdge(outEdge.getLabel, tinker.getVertex(inVertexId))
          outEdge.getPropertyKeys.foreach((key) => copyEdge.setProperty(key, outEdge.getProperty(key)))
        }
        argument
      }
    }).iterate()
    new RevisionGraph(tinker)
  }

  private def revVertices(revVertex: Vertex): GremlinPipeline[Vertex, Vertex] = {
    new GremlinPipeline().start(revVertex).in("in-revision")
  }
}
