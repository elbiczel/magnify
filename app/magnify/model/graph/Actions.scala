package magnify.model.graph

import scala.collection.JavaConversions._

import com.tinkerpop.blueprints.{Direction, Element, Vertex}
import com.tinkerpop.gremlin.java.GremlinPipeline

trait Actions {

  def getName(authorWithTime: String): String = {
    val endEmailIndex = authorWithTime.lastIndexOf('<')
    authorWithTime.substring(0, endEmailIndex - 1)
  }

  def getName(v: Vertex): String = getName(v.getProperty[String]("author"))

  def getRevisionClasses(rev: Vertex): GremlinPipeline[Vertex, Vertex] =
    getRevisionVertices(rev)
        .has("kind", "class")
        .transform(new AsVertex)

  def getRevisionVertices(revVertex: Vertex): GremlinPipeline[Vertex, Vertex] =
    new GremlinPipeline()
        .start(revVertex)
        .in("in-revision")

  def getPackageClasses(pkg: Vertex): GremlinPipeline[Vertex, Vertex] =
    new GremlinPipeline()
        .start(pkg)
        .in("in-package")
        .has("kind", "class")
        .transform(new AsVertex)

  def getPrevRevision(v: Vertex): Option[Vertex] = v.getVertices(Direction.IN, "commit").iterator().toSeq.headOption

  def getNextRevision(v: Vertex): Option[Vertex] = v.getVertices(Direction.OUT, "commit").iterator().toSeq.headOption

  def copyProperties(from: Element, to: Element): Unit = {
    from.getPropertyKeys.foreach((key) => to.setProperty(key, from.getProperty(key)))
  }
}
