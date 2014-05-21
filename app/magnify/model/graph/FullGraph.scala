package magnify.model.graph

import java.lang
import java.io.{BufferedInputStream, BufferedOutputStream, FileInputStream, FileOutputStream}

import scala.collection.mutable
import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}

import com.tinkerpop.blueprints.{Graph => BlueprintsGraph, _}
import com.tinkerpop.blueprints.impls.tg.TinkerGraph
import com.tinkerpop.blueprints.util.io.graphml.{GraphMLReader, GraphMLWriter}
import com.tinkerpop.gremlin.java.GremlinPipeline
import com.tinkerpop.pipes.PipeFunction
import com.tinkerpop.pipes.branch.LoopPipe.LoopBundle
import magnify.model.{ChangeDescription, VersionedArchive}

/**
 * @author Cezary Bartoszuk (cezarybartoszuk@gmail.com)
 */
object FullGraph {
  implicit def gremlinPipelineAsScalaIterable[S, E](pipe: GremlinPipeline[S, E]): Iterable[E] =
    collectionAsScalaIterable(pipe.toList)

  def tinker(archive: VersionedArchive): FullGraph =
    new FullGraph(new TinkerGraph, archive)

  def load(fileName: String, pool: ExecutionContext): FullGraph = {
    val tinker = new TinkerGraph
    val is = new BufferedInputStream(new FileInputStream(fileName + ".gml"))
    GraphMLReader.inputGraph(tinker, is)
    val archive = VersionedArchive.load(fileName + ".archive")
    val graph = new FullGraph(tinker, archive)
    graph.headVertex = graph.getHeadCommitVertex.toList.head
    Future {
      graph.forRevision()
    }(pool)
    graph
  }
}

final class FullGraph(override val graph: BlueprintsGraph, archive: VersionedArchive) extends Graph {

  def getHeadCommitVertex: GremlinPipeline[Vertex, Vertex] =
    vertices
    .filter(HeadCommitFilter)

  def getTailCommitVertex: GremlinPipeline[Vertex, Vertex] =
    vertices
    .filter(TailCommitFilter)

  private var headVertex: Vertex = _
  private var parentRevVertex: Option[Vertex] = None

  private val versions = mutable.Map[Option[String], Graph]()

  def save(fileName: String): Unit = {
    val os = new BufferedOutputStream(new FileOutputStream(fileName + ".gml"))
    GraphMLWriter.outputGraph(graph, os)
    os.flush()
    os.close()
    archive.save(fileName + ".archive")
  }

  def forRevision(rev: Option[String] = None): Graph = {
    versions.getOrElse(rev, {
      val revGraph = RevisionGraph(this, revVertex(rev))
      versions.put(rev, revGraph)
      revGraph
    })
  }

  def getSource(v: Vertex): String = {
    if (v.getPropertyKeys.contains("source-code")) {
      v.getProperty("source-code")
    } else {
      archive.getContent(v.getProperty("object-id"))
    }
  }

  private def revVertex(rev: Option[String]): Vertex = {
    rev.flatMap { (revId) =>
      val commitVertices = vertices.has("kind", "commit").has("name", revId).toList
      if (commitVertices.size() == 1) { Some(commitVertices.get(0).asInstanceOf[Vertex]) } else { None }
    }.getOrElse(headVertex)
  }

  def commitsOlderThan(rev: Option[String] = None): Seq[Vertex] = {
    val head = revVertex(rev)
    val older = new GremlinPipeline().start(head).in("commit").loop(1, TrueFilter, TrueFilter).dedup().toList.toSeq
    Seq(head) ++ older
  }

  def currentVertices: GremlinPipeline[Vertex, Vertex] = vertices.filter(NoNewVertexFilter)

  private def getPrevCommitVertex(kind: String, name: String, props: Map[String, String]): Option[Vertex] = {
    val vertex = props
        .foldLeft[GremlinPipeline[Vertex, Element]](currentVertices.has("kind", kind).has("name", name)
            .asInstanceOf[GremlinPipeline[Vertex, Element]]) { case (pipe, (prop, propValue)) =>
              pipe.has(prop, propValue).asInstanceOf[GremlinPipeline[Vertex, Element]]
            }
        .transform(new AsVertex)
        .dedup
        .toList
        .toSet
    require(vertex.size <= 1, parentRevVertex.map(_.getProperty("rev")).getOrElse("parent") + " : " +
        vertex.map((v) => "V[" + Seq(v.getProperty("name"), v.getProperty("kind"), v.getId.toString).mkString(", ") + "]").mkString(", "))
    vertex.headOption
  }

  def addVertex(kind: String, name: String, props: Map[String, String] = Map()): (Vertex, Option[Edge]) = {
    val oldVertex: Option[Vertex] = getPrevCommitVertex(kind, name, props)
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
    parentRevVertex = Some(revVertex)
    headVertex = revVertex
  }
}
