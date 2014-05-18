package magnify.model.graph

import com.tinkerpop.gremlin.java.GremlinPipeline
import com.tinkerpop.blueprints.{Edge, Vertex}

trait Graph {
  def vertices: GremlinPipeline[Vertex, Vertex]
  def edges: GremlinPipeline[Edge, Edge]
}
