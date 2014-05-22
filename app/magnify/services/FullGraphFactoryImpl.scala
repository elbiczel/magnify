package magnify.services

import java.io.{BufferedInputStream, FileInputStream}

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}

import com.google.inject.name.Named
import com.tinkerpop.blueprints.impls.tg.TinkerGraph
import com.tinkerpop.blueprints.util.io.graphml.GraphMLReader
import com.tinkerpop.gremlin.java.GremlinPipeline
import magnify.features.{FullGraphFactory, RevisionGraphFactory}
import magnify.model.VersionedArchive
import magnify.model.graph.{FullGraph, HeadCommitFilter}

class FullGraphFactoryImpl(
    @Named("ServicesPool") implicit val pool: ExecutionContext,
    revisionGraphFactory: RevisionGraphFactory) extends FullGraphFactory {
  implicit def gremlinPipelineAsScalaIterable[S, E](pipe: GremlinPipeline[S, E]): Iterable[E] =
    collectionAsScalaIterable(pipe.toList)

  def tinker(archive: VersionedArchive): FullGraph = new FullGraph(new TinkerGraph, archive)

  def load(fileName: String): FullGraph = {
    val tinker = new TinkerGraph
    val is = new BufferedInputStream(new FileInputStream(fileName + ".gml"))
    GraphMLReader.inputGraph(tinker, is)
    val archive = VersionedArchive.load(fileName + ".archive")
    val headCommitV = new GremlinPipeline(tinker.getVertices, true)
        .filter(HeadCommitFilter)
        .toList
        .head
    val graph = new FullGraph(tinker, archive, headCommitV)
    Future {
      revisionGraphFactory(graph)
    }
    graph
  }
}
