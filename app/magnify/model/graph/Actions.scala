package magnify.model.graph

import scala.collection.JavaConversions._

import com.tinkerpop.blueprints.{Direction, Edge, Element, Vertex}
import com.tinkerpop.gremlin.java.GremlinPipeline
import magnify.features.MetricNames

trait Actions {

  final def getName(authorWithTime: String): String = {
    val endEmailIndex = authorWithTime.lastIndexOf('<')
    authorWithTime.substring(0, endEmailIndex - 1)
  }

  final def getName(v: Vertex): String = getName(v.getProperty[String]("author"))

  final def getRevisionVertices(pipe: GremlinPipeline[Vertex, Vertex]): GremlinPipeline[Vertex, Vertex] =
    pipe.in("in-revision")

  final def getRevisionClasses(pipe: GremlinPipeline[Vertex, Vertex]): GremlinPipeline[Vertex, Vertex] =
    pipe.has("kind", "class").transform(new AsVertex)

  final def getRevisionClasses(rev: Vertex): GremlinPipeline[Vertex, Vertex] =
    getRevisionClasses(getRevisionVertices(rev))

  final def getRevisionVertices(revVertex: Vertex): GremlinPipeline[Vertex, Vertex] =
    getRevisionVertices(new GremlinPipeline().start(revVertex))

  final def getPackageClasses(pkg: Vertex): GremlinPipeline[Vertex, Vertex] =
    new GremlinPipeline()
        .start(pkg)
        .in("cls-in-pkg")
        .has("kind", "class")
        .transform(new AsVertex)

  final def getPrevRevision(v: Vertex): Option[Vertex] =
    v.getVertices(Direction.IN, "commit").iterator().toSeq.headOption

  final def getPrevCommit(v: Vertex): Option[Edge] = v.getEdges(Direction.IN, "commit").iterator().toSeq.headOption

  final def getNextRevision(v: Vertex): Option[Vertex] =
    v.getVertices(Direction.OUT, "commit").iterator().toSeq.headOption

  final def copyProperties(from: Element, to: Element): Unit = {
    from.getPropertyKeys.foreach((key) => to.setProperty(key, from.getProperty(key)))
  }

  final def getMetricValue[A](metricName: String, e: Element): A = e.getProperty[A]("metric--" + metricName)

  final def setMetricValue[A](metricName: String, e: Element, value: A): Unit =
    e.setProperty(MetricNames.propertyName(metricName), value)
}
