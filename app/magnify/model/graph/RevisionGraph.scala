package magnify.model.graph

import scala.collection.mutable
import scala.collection.JavaConversions._

import com.tinkerpop.blueprints.{Direction, Edge, Graph => BlueprintsGraph, Vertex}
import com.tinkerpop.blueprints.impls.tg.TinkerGraph
import com.tinkerpop.blueprints.oupls.jung.GraphJung
import com.tinkerpop.gremlin.java.GremlinPipeline
import com.tinkerpop.pipes.PipeFunction
import edu.uci.ics.jung.algorithms.scoring.PageRank
import play.api.Logger

final class RevisionGraph(override val graph: BlueprintsGraph) extends Graph {

  private def addPackages(): Unit = {
    val classVertices = vertices.has("kind", "class").toList.toSeq.asInstanceOf[Seq[Vertex]]
    val classNames = classVertices.map(_.getProperty[String]("name"))
    val classesByNames = classNames.zip(classVertices).toMap
    val packageNames = allPackageNames(classNames).toSeq
    val packageVertices = packageNames.map(addPackageVertex)
    val packageByName = packageNames.zip(packageVertices).toMap
    addPackageEdges(packageByName)
    for (clsName <- classNames) {
      val classVertex = classesByNames(clsName)
      val parentPackageName = parentPkgName(clsName)
      val parentPackageVertex = packageByName(parentPackageName)
      addEdge(classVertex, "in-package", parentPackageVertex)
    }
  }

  private def allPackageNames(classNames: Seq[String]): Seq[String] = {
    (for {
      clsName <- classNames
      pkgName <- clsName.split('.').inits.toList.tail.map(_.mkString("."))
    } yield (pkgName)).toSet.toSeq
  }

  private def addPackageVertex(pkgName: String): Vertex = addVertex("package", pkgName)

  private def addPackageEdges(packageByName: Map[String, Vertex]) {
    for ((name, pkg) <- packageByName; if name.nonEmpty) {
      val outer = packageByName(parentPkgName(name))
      addEdge(pkg, "in-package", outer)
    }
  }

  private def parentPkgName(name: String): String =
    if (name.contains('.')) {
      name.substring(0, name.lastIndexOf('.'))
    } else {
      ""
    }

  private def addPageRank(): Unit = {
    val pageRank = new PageRank[Vertex, Edge](new GraphJung(graph), 0.15)
    pageRank.evaluate()
    for (vertex <- graph.getVertices) {
      vertex.setProperty("page-rank", pageRank.getVertexScore(vertex).toString)
    }
  }

  private def addPackageImports(): Unit = {
    for {
      pkg <- vertices
          .has("kind", "class")
          .out("in-package")
          .toList.toSet[Vertex]
      importsPkg <- new GremlinPipeline()
          .start(pkg)
          .in("in-package")
          .out("imports")
          .out("in-package")
          .toList.toSet[Vertex]
    } {
      addEdge(pkg, "package-imports", importsPkg)
    }
  }

  private def computePackageLOC(): Unit = {
    vertices.has("kind", "package").toList.foreach { case pkg: Vertex =>
      val elems = new GremlinPipeline()
          .start(pkg)
          .in("in-package")
          .has("kind", "class")
          .property("metric--lines-of-code")
          .toList.toSeq.asInstanceOf[mutable.Seq[Double]]
      val avg = Option(elems).filter(_.size > 0).map(_.sum / elems.size)
      pkg.setProperty("metric--lines-of-code", avg.getOrElse(0.0))
    }
  }
}

object RevisionGraph {

  private val logger = Logger(classOf[RevisionGraph].getSimpleName)

  val LABELS: Seq[String] = Seq("imports", "calls")

  def apply(full: FullGraph, revVertex: Vertex): Graph = {
    logger.info("Subgraph creation starts: " + System.nanoTime())
    val tinker = new TinkerGraph
    revVertices(revVertex).sideEffect(new PipeFunction[Vertex, Vertex] {
      override def compute(argument: Vertex): Vertex = {
        val copy = tinker.addVertex(argument.getId)
        argument.getPropertyKeys.foreach((key) => copy.setProperty(key, argument.getProperty(key)))
        argument
      }
    }).iterate()
    revVertices(revVertex).sideEffect(new PipeFunction[Vertex, Vertex] {
      override def compute(argument: Vertex): Vertex = {
        val copy = tinker.getVertex(argument.getId)
        argument.getEdges(Direction.OUT, LABELS :_*).foreach { (outEdge) =>
          val inVertexId = outEdge.getVertex(Direction.IN).getId
          val oOutCopy = Option(tinker.getVertex(inVertexId))
          oOutCopy match {
            case Some(outCopy) => {
              val copyEdge = copy.addEdge(outEdge.getLabel, outCopy)
              outEdge.getPropertyKeys.foreach((key) => copyEdge.setProperty(key, outEdge.getProperty(key)))
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
    logger.info("Compute LOC starts: " + System.nanoTime())
    revGraph.computePackageLOC()
    logger.info("Compute LOC finished: " + System.nanoTime())
    revGraph
  }

  private def revVertices(revVertex: Vertex): GremlinPipeline[Vertex, Vertex] = {
    new GremlinPipeline().start(revVertex).in("in-revision")
  }
}
