package magnify.services


import scala.collection.JavaConversions._

import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import com.tinkerpop.blueprints.{Direction, Vertex}
import com.tinkerpop.blueprints.impls.tg.TinkerGraph
import com.tinkerpop.pipes.PipeFunction
import magnify.features.{MetricsProvider, RevisionGraphFactory, RevisionMetric}
import magnify.model.graph.{Actions, FullGraph, Graph, RevisionGraph}
import play.api.Logger

class RevisionGraphFactoryImpl(metrics: MetricsProvider[(Graph, Vertex), Graph, RevisionMetric])
    extends RevisionGraphFactory with Actions {

  private val logger = Logger(classOf[RevisionGraphFactoryImpl].getSimpleName)

  private val LABELS: Seq[String] = Seq("cls-imports-cls", "calls")

  private val versions: LoadingCache[(FullGraph, Option[String]), Graph] = CacheBuilder.newBuilder()
      .maximumSize(100)
      .softValues()
      .build(new CacheLoader[(FullGraph, Option[String]), Graph] {
    override def load(key: (FullGraph, Option[String])): Graph = {
      val graph = key._1
      val revVertex = graph.revVertex(key._2)
      logger.info("Subgraph creation starts: " + System.nanoTime())
      val tinker = new TinkerGraph
      getRevisionVertices(revVertex).sideEffect(new PipeFunction[Vertex, Vertex] {
        override def compute(argument: Vertex): Vertex = {
          val copy = tinker.addVertex(argument.getId)
          copyProperties(argument, copy)
          argument
        }
      }).iterate()
      getRevisionVertices(revVertex).sideEffect(new PipeFunction[Vertex, Vertex] {
        override def compute(argument: Vertex): Vertex = {
          val copy = tinker.getVertex(argument.getId)
          argument.getEdges(Direction.OUT, LABELS :_*).foreach { (outEdge) =>
            val inVertexId = outEdge.getVertex(Direction.IN).getId
            val oOutCopy = Option(tinker.getVertex(inVertexId))
            oOutCopy match {
              case Some(outCopy) => {
                val copyEdge = copy.addEdge(outEdge.getLabel, outCopy)
                copyProperties(outEdge, copyEdge)
              }
              case None => ()
            }
          }
          argument
        }
      }).iterate()
      val revGraph = new RevisionGraph(tinker)
      logger.info("Subgraph creation finished: " + System.nanoTime())
      logger.info("Adding packages starts: " + System.nanoTime())
      revGraph.addPackages()
      logger.info("Adding packages finished: " + System.nanoTime())
      logger.info("PageRank starts: " + System.nanoTime())
      revGraph.addPageRank()
      logger.info("PageRank finished: " + System.nanoTime())
      logger.info("Add package Imports starts: " + System.nanoTime())
      revGraph.addPackageImports()
      logger.info("Add package Imports finished: " + System.nanoTime())
      metrics().foldLeft(revGraph.asInstanceOf[Graph]) { case (graph, metric) =>
        metric(graph, revVertex)
      }
    }
  })

  override def apply(graph: FullGraph, revision: Option[String]): Graph = versions.get((graph, revision))
}
