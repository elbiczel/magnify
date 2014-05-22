package magnify.services.metrics

import com.tinkerpop.blueprints.{Direction, Vertex}
import com.tinkerpop.gremlin.java.GremlinPipeline
import com.tinkerpop.pipes.PipeFunction
import magnify.model.graph.AsVertex

abstract class AggregatingMetricTransformation[A](dir: Direction, label: String, kind: String, metricName: String)
    extends PipeFunction[Vertex, A] {

  override final def compute(v: Vertex): A = {
    val pipe = individuals(new GremlinPipeline().start(v))
        .has("kind", kind)
        .transform(new AsVertex)
    val value = metricValue(pipe)
    v.setProperty("metric--" + metricName, value)
    value
  }

  def metricValue(pipe: GremlinPipeline[Vertex, Vertex]): A

  private def individuals(pipe: GremlinPipeline[Vertex, Vertex]): GremlinPipeline[Vertex, Vertex] = dir match {
    case Direction.IN => pipe.in(label)
    case Direction.OUT => pipe.out(label)
  }
}
