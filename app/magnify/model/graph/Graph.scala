package magnify.model.graph

import com.tinkerpop.gremlin.java.GremlinPipeline
import com.tinkerpop.blueprints.{Edge, Graph => BlueprintsGraph, Vertex}

trait Graph {
  def vertices: GremlinPipeline[Vertex, Vertex] = new GremlinPipeline(graph.getVertices, true)

  def edges: GremlinPipeline[Edge, Edge] = new GremlinPipeline(graph.getEdges, true)

  def addEdge(from: Vertex, label: String, to: Vertex): Edge = graph.addEdge(null, from, to, label)

  def addVertex(kind: String, name: String): Vertex = {
    val newVertex = graph.addVertex(null)
    newVertex.setProperty("kind", kind)
    newVertex.setProperty("name", name)
    newVertex
  }

  protected val graph: BlueprintsGraph
}
