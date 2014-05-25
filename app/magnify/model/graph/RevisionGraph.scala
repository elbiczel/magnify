package magnify.model.graph

import scala.collection.JavaConversions._

import com.tinkerpop.blueprints.{Edge, Graph => BlueprintsGraph, Vertex}
import com.tinkerpop.blueprints.oupls.jung.GraphJung
import com.tinkerpop.gremlin.java.GremlinPipeline
import edu.uci.ics.jung.algorithms.scoring.PageRank
import magnify.features.MetricNames

final class RevisionGraph(override val graph: BlueprintsGraph) extends Graph with Actions {

  def addPackages(): Unit = {
    val classVertices = vertices.has("kind", "class").toList.toSeq.asInstanceOf[Seq[Vertex]]
    classVertices.foreach((v) => v.setProperty("parent-pkg-name", parentPkgName(v.getProperty[String]("name"))))
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
      addEdge(classVertex, "cls-in-pkg", parentPackageVertex)
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

  def addPageRank(): Unit = {
    val pageRank = new PageRank[Vertex, Edge](new GraphJung(graph), 0.15)
    pageRank.evaluate()
    for (vertex <- graph.getVertices) {
      vertex.setProperty(MetricNames.propertyName(MetricNames.pageRank), pageRank.getVertexScore(vertex).toString)
    }
  }

  def addPackageImports(): Unit = {
    for {
      pkg <- vertices
          .has("kind", "package")
          .transform(new AsVertex)
          .toList.toSet[Vertex]
      importsPkg <- getPackageClasses(pkg)
          .out("imports")
          .out("cls-in-pkg")
          .toList.toSeq.groupBy((v) => v).mapValues((seq) => seq.length)
    } {
      val edge = addEdge(pkg, "package-imports", importsPkg._1)
      edge.setProperty("weight", importsPkg._2)
    }
    for {
      cls <- vertices
        .has("kind", "class")
        .transform(new AsVertex)
        .toList.toSet[Vertex]
      importsPkg <- new GremlinPipeline()
          .start(cls)
          .out("imports")
          .out("cls-in-pkg")
          .toList.toSeq.groupBy((v) => v).mapValues((seq) => seq.length)
    } {
      val edge = addEdge(cls, "cls-imports-pkg", importsPkg._1)
      edge.setProperty("weight", importsPkg._2)
    }
  }
}
