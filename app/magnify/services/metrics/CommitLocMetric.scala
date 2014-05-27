package magnify.services.metrics

import scala.collection.JavaConversions._

import com.tinkerpop.blueprints.{Direction, Vertex}
import com.tinkerpop.gremlin.java.GremlinPipeline
import magnify.features.{FullGraphMetric, MetricNames}
import magnify.model.graph.{AsVertex, FullGraph}

class CommitLocMetric extends FullGraphMetric {
  override def name: String = MetricNames.linesOfCode

  override def apply(graph: FullGraph): FullGraph = {
    // Class loc is already counted as AST step
    graph.vertices.has("kind", "commit").transform(new AsVertex)
        .sideEffect(new AggregatingMetricTransformation[Double](
      Direction.IN, "in-revision", "class", MetricNames.linesOfCode) {
          override def metricValue(pipe: GremlinPipeline[Vertex, Vertex]): Double = {
            val nodes = pipe.toList.toSeq
            val values = nodes.map(getMetricValue[Double](metricName, _))
            val sum = values.sum
            sum
          }
        }).sideEffect(new AggregatingMetricTransformation[Double](
      Direction.IN, "in-revision", "class", MetricNames.averageLinesOfCode) {
      override def metricValue(pipe: GremlinPipeline[Vertex, Vertex]): Double = {
        val nodes = pipe.toList.toSeq
        val values = nodes.map(getMetricValue[Double](MetricNames.linesOfCode, _))
        Option(values).filter(_.size > 0).map(_.sum / values.size).getOrElse(0.0)
      }
    }).iterate()
    graph
  }
}
