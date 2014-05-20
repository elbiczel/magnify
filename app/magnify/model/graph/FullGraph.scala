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

  def tinker: FullGraph =
    new FullGraph(new TinkerGraph)

  def load(fileName: String, pool: ExecutionContext): FullGraph = {
    val tinker = new TinkerGraph
    val is = new BufferedInputStream(new FileInputStream(fileName))
    GraphMLReader.inputGraph(tinker, is)
    val graph = new FullGraph(tinker)
    graph.headVertex = new GremlinPipeline(tinker.getVertices, true)
        .filter(HeadVertexFilter)
        .toList.head
    Future {
      graph.forRevision()
    }(pool)
    graph
  }

  private object HeadVertexFilter extends PipeFunction[Vertex, lang.Boolean] {
    override def compute(commit: Vertex): lang.Boolean =
      if (commit.getProperty[String]("kind") != "commit") { false } else {
        !commit.getEdges(Direction.OUT, "commit").iterator().hasNext
      }
  }
}

final class FullGraph(override val graph: BlueprintsGraph) extends Graph {

  private var headVertex: Vertex = _
  private var parentRevVertex: Option[Vertex] = None

  private val versions = mutable.Map[Option[String], Graph]()

  def save(fileName: String): Unit = {
    val os = new BufferedOutputStream(new FileOutputStream(fileName))
    GraphMLWriter.outputGraph(graph, os)
    os.flush()
    os.close()
  }

  def forRevision(rev: Option[String] = None): Graph = {
    versions.getOrElse(rev, {
      val revGraph = RevisionGraph(this, revVertex(rev))
      versions.put(rev, revGraph)
      revGraph
    })
  }

  def getSource(v: Vertex, vArchive: VersionedArchive): String = {
    if (v.getPropertyKeys.contains("source-code")) {
      v.getProperty("source-code")
    } else {
      vArchive.getContent(v.getProperty("object-id"))
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

  def currentVertices: GremlinPipeline[Vertex, Vertex] =
    parentRevVertex.map { revVertex =>
      new GremlinPipeline().start(revVertex).in("in-revision")
      .transform(NewVertex)
    }.getOrElse(vertices)

  private def getPrevCommitVertex(kind: String, name: String, props: Map[String, String]): Option[Vertex] = {
    var pipeline = currentVertices.has("kind", kind).has("name", name)
    props.keys.foreach((prop) => pipeline = pipeline.has(prop, props(prop)))
    val vertex = pipeline.transform(new AsVertex).dedup().toList.toSet
    require(vertex.size <= 1, parentRevVertex.map(_.getProperty("rev")).getOrElse("parent") + " : " +
        vertex.map((v) => "V[" + Seq(v.getProperty("name"), v.getProperty("kind"), v.getId.toString).mkString(", ") + "]").mkString(", "))
    vertex.headOption
  }

  def addVertex(kind: String, name: String, props: Map[String, String] = Map()): (Vertex, Option[Edge]) = {
    val oldVertex: Option[Vertex] = getPrevCommitVertex(kind, name, props)
    val newVertex = addVertex(kind, name)
    props.keys.foreach((prop) => newVertex.setProperty(prop, props(prop)))
    (newVertex, oldVertex.map(addEdge(newVertex, "commit", _)))
  }

  def commitVersion(changeDescription: ChangeDescription, classes: Set[String]): Unit = {
    val revVertex = addVertex("commit", changeDescription.revision)
    changeDescription.setProperties(revVertex)
    headVertex = parentRevVertex.map { parentRev =>
      this.addEdge(revVertex, "commit", parentRev)
      headVertex
    }.getOrElse(revVertex)

    val currentClasses = currentVertices
        .has("kind", "class")
        .filter(HasInFilter("name", classes))
        .filter(NotFilter(HasInFilter("file-name", changeDescription.removedFiles)))
        .transform(new AsVertex())
    val classVertices = currentClasses.toList
    val currentPackages =
      new GremlinPipeline(classVertices, true).out("in-package").loop(1, TrueFilter, TrueFilter).dedup()
    val pkgVertices = currentPackages.toList
    for (inRevVertex <- classVertices ++ pkgVertices) {
      this.addEdge(inRevVertex, "in-revision", revVertex)
    }
    parentRevVertex = Some(revVertex)
  }

  private object TrueFilter extends PipeFunction[LoopBundle[Vertex], lang.Boolean] {
    override def compute(argument: LoopBundle[Vertex]): lang.Boolean = true
  }

  private object NewVertex extends PipeFunction[Vertex, Vertex] {
    override def compute(v: Vertex): Vertex = {
      val it = v.getVertices(Direction.IN, "commit").iterator()
      if (it.hasNext) { it.next() } else { v }
    }
  }

  def currentRevisionFilter: PipeFunction[Vertex, lang.Boolean] = new PipeFunction[Vertex, lang.Boolean] {
    override def compute(v: Vertex): lang.Boolean = parentRevVertex match {
      case None => true
      case Some(revV) => {
        v.getVertices(Direction.OUT, "in-revision").toSet.contains(revV)
      }
    }
  }
}

class AsVertex[T <: Element] extends PipeFunction[T, Vertex] {
  override def compute(argument: T): Vertex = argument.asInstanceOf[Vertex]
}

case class HasInFilter[T <: Element](property: String, values: Set[String])
    extends PipeFunction[T, lang.Boolean] {
  override def compute(element: T): lang.Boolean = values.contains(element.getProperty(property))
}

case class NotFilter[T <: Element](filter: PipeFunction[T, lang.Boolean])
    extends PipeFunction[T, lang.Boolean] {
  override def compute(argument: T): lang.Boolean = !filter.compute(argument)
}
