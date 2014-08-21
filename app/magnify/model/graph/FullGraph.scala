package magnify.model.graph


import java.io.{BufferedOutputStream, FileOutputStream}

import scala.collection.JavaConversions._

import com.tinkerpop.blueprints.{Graph => BlueprintsGraph, _}
import com.tinkerpop.blueprints.util.io.graphml.GraphMLWriter
import com.tinkerpop.gremlin.java.GremlinPipeline
import com.tinkerpop.pipes.PipeFunction
import magnify.features.AuthorId
import magnify.model.{ChangeDescription, VersionedArchive}

/**
 * @author Cezary Bartoszuk (cezarybartoszuk@gmail.com)
 */
final class FullGraph(
    override val graph: BlueprintsGraph,
    archive: VersionedArchive,
    private[this] var headVertex: Vertex)
  extends Graph
  with Actions
  with AuthorId {

  def this(graph: BlueprintsGraph, archive: VersionedArchive) = this(graph, archive, null)

  def getHeadCommitVertex: GremlinPipeline[Vertex, Vertex] =
    vertices
    .filter(HeadCommitFilter)

  def getTailCommitVertex: GremlinPipeline[Vertex, Vertex] =
    vertices
    .filter(TailCommitFilter)

  private var parentRevVertex: Option[Vertex] = None

  def save(fileName: String): Unit = {
    val os = new BufferedOutputStream(new FileOutputStream(fileName + ".gml"))
    GraphMLWriter.outputGraph(graph, os)
    os.flush()
    os.close()
    archive.save(fileName + ".archive")
  }

  def getSource(v: Vertex): String = {
    if (v.getPropertyKeys.contains("source-code")) {
      v.getProperty("source-code")
    } else {
      archive.getContent(v.getProperty("object-id"))
    }
  }

  def revVertex(rev: Option[String]): Vertex = {
    rev.flatMap { (revId) =>
      vertices.has("kind", "commit").has("name", revId).toList.headOption.asInstanceOf[Option[Vertex]]
    }.getOrElse(headVertex)
  }

  def commitsOlderThan(rev: Option[String] = None): Seq[Vertex] = {
    val head = revVertex(rev)
    val older = new GremlinPipeline().start(head).in("commit").loop(1, TrueFilter, TrueFilter).dedup().toList.toSeq
    Seq(head) ++ older
  }

  def currentVertices: GremlinPipeline[Vertex, Vertex] = vertices.filter(NoNewVertexFilter)

  private def getPrevCommitVertex(kind: String, name: String): Option[Vertex] = {
    val vertex = currentVertices
        .has("kind", kind)
        .has("name", name)
        .transform(new AsVertex)
        .dedup
        .toList
        .toSet
    require(vertex.size <= 1, parentRevVertex.map(_.getProperty("rev")).getOrElse("parent") + " : " +
        vertex.map((v) => "V[" + Seq(v.getProperty("name"), v.getProperty("kind"), v.getId.toString).mkString(", ") + "]").mkString(", "))
    vertex.headOption
  }

  def addVertex(kind: String, name: String, props: Map[String, String] = Map()): (Vertex, Option[Edge]) = {
    val oldVertex: Option[Vertex] = getPrevCommitVertex(kind, name)
    val newVertex = addVertex(kind, name)
    props.keys.foreach((prop) => newVertex.setProperty(prop, props(prop)))
    (newVertex, oldVertex.map(addEdge(_, "commit", newVertex)))
  }

  def commitVersion(changeDescription: ChangeDescription, classes: Set[String]): Unit = {
    val revVertex = addVertex("commit", changeDescription.revision)
    changeDescription.setProperties(revVertex)
    parentRevVertex.map { parentRev =>
      this.addEdge(parentRev, "commit", revVertex)
    }

    currentVertices
        .has("kind", "class")
        .filter(HasInFilter("name", classes))
        .filter(NotFilter(HasInFilter("file-name", changeDescription.removedFiles)))
        .transform(new AsVertex())
        .sideEffect(new PipeFunction[Vertex, Vertex] {
          override def compute(v: Vertex): Vertex = {
            addEdge(v, "in-revision", revVertex)
            v
          }
        }).iterate()
    val authorId = getName(revVertex)
    val authorVertex = vertices
        .has("kind", "author")
        .has("name", authorId)
        .transform(new AsVertex)
        .toList.headOption.getOrElse {
          addVertex("author", authorId)
        }
    addEdge(authorVertex, "committed", revVertex)
    parentRevVertex = Some(revVertex)
    headVertex = revVertex
  }
}
