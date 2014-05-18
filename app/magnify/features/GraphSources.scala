package magnify.features

import java.io._

import scala.collection.mutable
import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.matching.Regex

import com.tinkerpop.blueprints.{Edge, Vertex}
import com.tinkerpop.blueprints.oupls.jung.GraphJung
import com.tinkerpop.gremlin.java.GremlinPipeline
import edu.uci.ics.jung.algorithms.scoring.PageRank
import magnify.model._
import magnify.model.graph.FullGraph
import play.api.Logger

/**
 * @author Cezary Bartoszuk (cezarybartoszuk@gmail.com)
 */
private[features] final class GraphSources (parse: Parser, imports: Imports, implicit val pool: ExecutionContext)
    extends Sources {

  private val logger = Logger(classOf[GraphSources].getSimpleName)
  private val graphsDir = "graphs/"

  private val graphs = mutable.Map[String, (FullGraph, VersionedArchive)]()
  private val importedGraphs = mutable.Map[String, Json]()

  Future {
    logger.info("Loading graphs...")
    val dir = new File(graphsDir)
    val filesSet: Set[String] = dir.list().toSet
    val projects = filesSet.map(_.split("\\.").head).filter(_.trim.nonEmpty)
    projects.foreach { (project) =>
      logger.info("Loading: " + project)
      val graph = FullGraph.load(graphsDir + project + ".gml", pool)
      val archive = VersionedArchive.load(graphsDir + project + ".archive")
      graphs += project -> (graph, archive)
    }
  }.recover {
    case t: Throwable => {
      logger.error("Error loading graphs", t)
      throw t
    }
  }

  class ClassExtractor {
    var currentClasses = Map[String, Set[String]]() // file name -> class name
    var changedFiles = Set[String]()

    def newCommit(diff: ChangeDescription): Unit = {
      currentClasses = currentClasses.filterKeys((fileName) => !diff.removedFiles.contains(fileName))
      changedFiles = diff.changedFiles
    }

    def shouldParse(fileName: String): Boolean =
      changedFiles.contains(fileName) || !currentClasses.containsKey(fileName)

    def parsedFile(fileName: String, classes: Seq[ParsedFile]) =
      currentClasses = currentClasses.updated(fileName, classes.map(_.ast.className).toSet)

    def classes: Set[String] = currentClasses.values.toSet.flatten
  }

  override def add(name: String, vArchive: VersionedArchive) {
    val graph = FullGraph.tinker
    val classExtractor = new ClassExtractor()
    logger.info("Revision analysis starts: " + name + " : " + System.nanoTime())
    vArchive.extract { (archive, diff) =>
      classExtractor.newCommit(diff)
      val start = System.nanoTime()
      val classes = classesFrom(archive, classExtractor)
      val parseTime = System.nanoTime() - start
      val (clsTime, importTime, packageTime) = processRevision(graph, diff, classes, classExtractor.classes)
      graph.commitVersion(diff, classExtractor.classes)
      System.err.print("%s,%s,%s,%s,%s,%s,%s\n".format(
        diff.revision, diff.changedFiles.size, diff.removedFiles.size, parseTime, clsTime, importTime, packageTime))
      Seq() // for monoid to work
    }
    logger.info("Revision analysis finished: " + name + " : " + System.nanoTime())
    logger.info("PageRank starts: " + name + " : " + System.nanoTime())
    // addPageRank(graph) // TODO(biczel): Make it work on single revision layer.
    logger.info("PageRank finished: " + name + " : " + System.nanoTime())
    logger.info("Add package Imports starts: " + name + " : " + System.nanoTime())
    addPackageImports(graph)
    logger.info("Add package Imports finished: " + name + " : " + System.nanoTime())
    logger.info("Compute LOC starts: " + name + " : " + System.nanoTime())
    computeLinesOfCode(graph, vArchive)
    logger.info("Compute LOC finished: " + name + " : " + System.nanoTime())
    Future {
      // generate head graph
      graph.forRevision()
      graph.save(graphsDir + name + ".gml")
      vArchive.save(graphsDir + name + ".archive")
    }.recover {
      case t: Throwable => {
        logger.error("Error saving repo: " + name, t)
        throw t
      }
    }
    graphs += name -> (graph, vArchive)
  }

  override def add(name: String, graph: Json) {
    importedGraphs += name -> graph
  }

  private def classesFrom(file: Archive, classExtractor: ClassExtractor): Seq[ParsedFile] =
    file.extract { (fileName, oFileId, content) =>
      if (isJavaFile(fileName) && classExtractor.shouldParse(fileName) && !isTestFile(fileName)) {
        val stringContent = inputStreamToString(content())
        val parsedFiles = for (
          ast <- parse(fileName, new ByteArrayInputStream(stringContent.getBytes("UTF-8")))
        ) yield (ParsedFile(ast, stringContent, fileName, oFileId))
        classExtractor.parsedFile(fileName, parsedFiles)
        parsedFiles
      } else {
        Seq()
      }
  }

  private def isTestFile(fileName: String): Boolean = {
    fileName.contains("/test/") || fileName.startsWith("test/") || fileName.contains("/tests/") ||
        fileName.startsWith("tests/")
  }

  private def inputStreamToString(is: InputStream) = {
    val rd: BufferedReader = new BufferedReader(new InputStreamReader(is, "UTF-8"))
    val builder = new StringBuilder()
    try {
      var line = rd.readLine
      while (line != null) {
        builder.append(line + "\n")
        line = rd.readLine
      }
    } finally {
      rd.close()
    }
    builder.toString()
  }

  private def isJavaFile(name: String): Boolean =
    name.endsWith(".java") && !name.endsWith("Test.java")

  private def processRevision(
      graph: FullGraph,
      changeDescription: ChangeDescription,
      classes: Iterable[ParsedFile],
      allClasses: Set[String]): (Long, Long, Long) = {
    val clsStart = System.nanoTime()
    val clsVertices = classes.map(addClasses(graph, changeDescription))
    val clsTime = System.nanoTime() - clsStart
    val importsStart = System.nanoTime()
    addImports(graph, classes.map(_.ast), allClasses)
    val importsTime = System.nanoTime() - importsStart
    val packageStart = System.nanoTime()
    addPackages(graph, changeDescription, clsVertices)
    val packageTime = System.nanoTime() - packageStart
    (clsTime, importsTime, packageTime)
  }

  private def addClasses(graph: FullGraph, changeDescription: ChangeDescription): (ParsedFile => Vertex) = {
    parsedFile =>
      val (cls, commitEdge) = graph.addVertex(
        "class", parsedFile.ast.className, Map("file-name" -> parsedFile.fileName))
      commitEdge.map(changeDescription.setProperties(_))
      if (parsedFile.oFileId.isDefined) {
        cls.setProperty("object-id", parsedFile.oFileId.get)
      } else {
        cls.setProperty("source-code", parsedFile.content)
      }
      val linesOfCode = parsedFile.content.count(_ == '\n')
      cls.setProperty("metric--lines-of-code", linesOfCode.toDouble)
      cls
  }

  private def addImports(graph: FullGraph, classes: Iterable[Ast], allClasses: Set[String]) {
    for {
      (outCls, imported) <- imports.resolve(classes, allClasses: Set[String])
      inCls <- imported
    } for {
      inVertex <- classesNamed(graph, inCls)
      outVertex <- classesNamed(graph, outCls)
    } {
      graph.addEdge(outVertex, "imports", inVertex)
    }
  }

  private def addPackages(graph: FullGraph, changeDescription: ChangeDescription, classes: Iterable[Vertex]) {
    val packageNames = packagesFrom(classes)
    val packageByName = addPackageVertices(graph, changeDescription, packageNames)
    addPackageEdges(graph, packageByName)
    addClassPackageEdges(graph, classes, packageByName)
  }

  private def packagesFrom(classes: Iterable[Vertex]): Set[String] =
    (for {
      cls <- classes
      clsName = name(cls)
      pkgName <- clsName.split('.').inits.toList.tail.map(_.mkString("."))
    } yield pkgName).toSet

  private def addPackageVertices(
      graph: FullGraph, changeDescription: ChangeDescription, packageNames: Set[String]): Map[String, Vertex] =
    (for (pkgName <- packageNames) yield {
      val (pkg, commitEdge) = graph.addVertex("package", pkgName)
      commitEdge.map(changeDescription.setProperties(_))
      pkgName -> pkg
    }).toMap

  private def addPackageEdges(graph: FullGraph, packageByName: Map[String, Vertex]) {
    for ((name, pkg) <- packageByName; if name.nonEmpty) {
      val outer = packageByName(parentPkgName(name))
      graph.addEdge(pkg, "in-package", outer)
    }
  }

  private def addClassPackageEdges(graph: FullGraph, classes: Iterable[Vertex], packageByName: Map[String, Vertex]) {
    for (cls <- classes) {
      val pkg = packageByName(parentPkgName(name(cls)))
      graph.addEdge(cls, "in-package", pkg)
    }
  }

  private def addPackageImports(graph: FullGraph) {
    for {
      pkg <- graph.vertices
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
      graph.addEdge(pkg, "package-imports", importsPkg)
    }
  }

  private def addPageRank(graph: FullGraph) {
    val pageRank = new PageRank[Vertex, Edge](new GraphJung(graph.blueprintsGraph), 0.15)
    pageRank.evaluate()
    for (vertex <- graph.blueprintsGraph.getVertices) {
      vertex.setProperty("page-rank", pageRank.getVertexScore(vertex).toString)
    }
  }

  private def parentPkgName(name: String): String =
    if (name.contains('.')) {
      name.substring(0, name.lastIndexOf('.'))
    } else {
      ""
    }

  private def name(cls: Vertex): String =
    cls.getProperty("name").toString

  private def classesNamed(graph: FullGraph, name: String): Iterable[Vertex] =
    graph
      .currentVertices
      .has("kind", "class")
      .has("name", name)
      .asInstanceOf[GremlinPipeline[Vertex, Vertex]]
      .toList

  override def list: Seq[String] =
    graphs.keys.toSeq ++ importedGraphs.keys.toSeq

  override def get(name: String): Option[FullGraph] =
    graphs.get(name).map(_._1)

  override def getJson(name: String) =
    importedGraphs.get(name)

  private def computeLinesOfCode(graph: FullGraph, vArchive: VersionedArchive) {
    graph
      .vertices
      .has("kind", "package").toList foreach { case pkg: Vertex =>
      val elems = new GremlinPipeline()
        .start(pkg)
        .in("in-package")
        .has("kind", "class")
        .property("metric--lines-of-code")
        .toList.toSeq.asInstanceOf[mutable.Seq[Double]]
      val avg = Option(elems).filter(_.size > 0).map(_.sum / elems.size.toDouble)
      pkg.setProperty("metric--lines-of-code", avg.getOrElse(0.0))
    }
  }

  private object CsvCall extends Regex("""([^;]+);([^;]+);(\d+)""", "from", "to", "count")

  private object PackageFromCall extends Regex("""(.* |^)([^ ]*)\.[^.]+\.[^.(]+\(.*""")

  def addRuntime(name: String, file: File) {
    for (graph <- get(name)) {
      val runtime = for {
        CsvCall(from, to, count) <- Source.fromFile(file).getLines().toSeq
        PackageFromCall(_, fromPackage) = from
        PackageFromCall(_, toPackage) = to
      } yield (fromPackage, toPackage, count.toInt)
      val calls = runtime.groupBy {case (a, b, _) => (a, b)}.mapValues(s => s.map(_._3).sum)
      for {
        ((fromPackage, toPackage), count) <- calls
        from <- graph.forRevision().vertices.has("kind", "package").has("name", fromPackage).toList
        to <- graph.forRevision().vertices.has("kind", "package").has("name", toPackage).toList
      } {
        val e = graph.addEdge(from.asInstanceOf[Vertex], "calls", to.asInstanceOf[Vertex])
        e.setProperty("count", count.toString)
      }
    }
  }
}

private case class ParsedFile(ast: Ast, content: String, fileName: String, oFileId: Option[String])
