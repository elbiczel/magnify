package magnify.services.metrics

import scala.collection.JavaConversions._

import com.tinkerpop.blueprints.{Direction, Vertex}
import magnify.features.{LoggedFunction, RevisionMetric}
import magnify.model.graph.{AsVertex, Actions, Graph}
import play.api.Logger
import com.tinkerpop.gremlin.java.GremlinPipeline

class RevisionLocMetric extends RevisionMetric with Actions {

  val logger = Logger(classOf[RevisionLocMetric].getSimpleName)

  override def apply(g: Graph): Graph = {
    g.vertices.has("kind", "package").transform(new AsVertex).sideEffect(
      new AggregatingMetricTransformation[Double](Direction.IN, "in-package", "class", "lines-of-code") {
        override def metricValue(pipe: GremlinPipeline[Vertex, Vertex]): Double = {
          pipe.property("metric--lines-of-code").toList.toSeq.asInstanceOf[Seq[Double]].sum
        }
      }).iterate()
    g
  }
}

class LoggedRevisionLocMetric extends RevisionLocMetric with LoggedFunction[Graph, Graph]
