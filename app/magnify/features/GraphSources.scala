package magnify.features

import java.io._

import scala.collection.mutable
import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.matching.Regex

import com.google.inject.name.Named
import com.tinkerpop.blueprints.{Direction, Vertex}
import com.tinkerpop.gremlin.java.GremlinPipeline
import magnify.model._
import magnify.model.graph.{Actions, FullGraph, HasInFilter, NotFilter}
import play.api.Logger

/**
 * @author Cezary Bartoszuk (cezarybartoszuk@gmail.com)
 */
private[features] final class GraphSources(
    parse: Parser, imports: Imports, metrics: MetricsProvider[FullGraph, FullGraph, FullGraphMetric],
    graphFactory: FullGraphFactory, revisionGraphFactory: RevisionGraphFactory,
    @Named("ServicesPool") implicit val pool: ExecutionContext) extends Sources with Actions {

  private val logger = Logger(classOf[GraphSources].getSimpleName)
  private val graphsDir = "graphs/"

  private val graphs = mutable.Map[String, FullGraph]()
  private val importedGraphs = mutable.Map[String, Json]()

  Future {
    logger.info("Loading graphs...")
    val dir = new File(graphsDir)
    val filesSet: Set[String] = dir.list().toSet
    val projects = filesSet.map(_.split("\\.").head).filter(_.trim.nonEmpty)
    projects.foreach { (project) =>
      logger.info("Loading: " + project)
      val graph = graphFactory.load(graphsDir + project)
      graphs += project -> graph
    }
  }.recover {
    case t: Throwable => {
      logger.error("Error loading graphs", t)
      throw t
    }
  }

  class ClassExtractor {
    var currentClasses = Map[String, Set[String]]() // file name -> class names
    var changedFiles = Set[String]()

    def newCommit(diff: ChangeDescription): Unit = {
      currentClasses = currentClasses.filterKeys((fileName) => !diff.removedFiles.contains(fileName))
      changedFiles = diff.changedFiles
    }

    def shouldParse(fileName: String): Boolean =
      changedFiles.contains(fileName) || !currentClasses.containsKey(fileName)

    def parsedFile(fileName: String, classes: Seq[ParsedFile]): Unit =
      currentClasses = currentClasses.updated(fileName, classes.map(_.ast.className).toSet)

    def classes: Set[String] = currentClasses.values.toSet.flatten
  }

  override def add(name: String, vArchive: VersionedArchive, prefixes: Set[String]) {
    val graph = graphFactory.tinker(vArchive)
    val classExtractor = new ClassExtractor()
    logger.info("Revision analysis starts: " + name + " : " + System.nanoTime())
    vArchive.extract { (archive, diff) =>
      classExtractor.newCommit(diff)
      val classes = classesFrom(archive, classExtractor, prefixes)
      if (classes.nonEmpty) {
        processRevision(graph, diff, classes, classExtractor.classes)
        graph.commitVersion(diff, classExtractor.classes)
      }
      Seq() // for monoid to work
    }
    logger.info("Revision analysis finished: " + name + " : " + System.nanoTime())
    logger.info("Metrics analysis starts: " + name + " : " + System.nanoTime())
    val graphWithMetrics = metrics().foldLeft(graph) { case (graph, metric) =>
      metric(graph)
    }
    logger.info("Metrics analysis finished: " + name + " : " + System.nanoTime())
    Future {
      // generate head graph
      revisionGraphFactory(graphWithMetrics)
      graphWithMetrics.save(graphsDir + name)
    }.recover {
      case t: Throwable => {
        logger.error("Error saving repo: " + name, t)
        throw t
      }
    }
    graphs += name -> graphWithMetrics
  }

  override def add(name: String, graph: Json) {
    importedGraphs += name -> graph
  }

  private def classesFrom(file: Archive, classExtractor: ClassExtractor, prefixes: Set[String]): Seq[ParsedFile] =
    file.extract { (fileName, oFileId, content) =>
      if (isJavaFile(fileName) && classExtractor.shouldParse(fileName) && !isTestFile(fileName)) {
        val stringContent = inputStreamToString(content())
        val parsedFiles = for (
          ast <- parse(fileName, new ByteArrayInputStream(stringContent.getBytes("UTF-8")))
        ) yield (ParsedFile(ast, stringContent, fileName, oFileId))
        classExtractor.parsedFile(fileName, parsedFiles)
        parsedFiles.filter { (parsedFile) =>
          // TODO(biczel): Find some better way to handle multimodule projects
          val firstCatalog = parsedFile.ast.className.split("\\.").headOption
              .map(_ + "/").getOrElse(parsedFile.ast.className)
          val baseNames = Set("src/main/java/" + firstCatalog, "src/" + firstCatalog, firstCatalog)
          val allPrefixes = prefixes + ""
          val startsWith = for (pref <- allPrefixes; baseName <- baseNames) yield {
            if (pref == "") baseName else pref + "/" + baseName
          }
          startsWith.foldLeft(false) { (acc, pref) =>
            acc || parsedFile.fileName.startsWith(pref)
          }
        }
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
    name.endsWith(".java")

  private def processRevision(
      graph: FullGraph,
      changeDescription: ChangeDescription,
      classes: Iterable[ParsedFile],
      allClasses: Set[String]): Unit = {
    val changedClassNames = classes.map(_.ast.className).toSet
    val classesImportingChanged = classesImporting(graph, changedClassNames, allClasses)
    val newClassesByOldName = classesImportingChanged.map { (v) =>
      val allImported = v.getVertices(Direction.OUT, "cls-imports-cls").toSet
      val allImportedNames = allImported.map(_.getProperty[String]("name"))
      val changedImported = allImportedNames.filter(changedClassNames)
      (v -> changedImported)
    }.toMap
    val newVertex = classes.map(addClasses(graph, changeDescription))
    val newClassesByName = newVertex.map((v) => (v.getProperty[String]("name") -> v)).toMap
    addImportsFromNewClasses(graph, classes.map(_.ast), allClasses)
    newClassesByOldName.foreach { case (v, names) =>
      names.foreach { (clsName) =>
        graph.addEdge(v, "cls-imports-cls", newClassesByName(clsName))
      }
    }
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
      parsedFile.ast.metrics.foreach { case (name, value) =>
        setMetricValue(name, cls, value)
      }
      val linesOfCode = parsedFile.content.count(_ == '\n')
      setMetricValue(MetricNames.linesOfCode, cls, linesOfCode.toDouble)
      cls
  }

  private def classesImporting(graph: FullGraph, changedClasses: Set[String], allClasses: Set[String]): Set[Vertex] = {
    val hasInFilter = HasInFilter[Vertex]("name", changedClasses)
    graph.currentVertices
        .filter(hasInFilter)
        .has("kind", "class")
        .in("cls-imports-cls")
        .filter(HasInFilter("name", allClasses))
        .filter(NotFilter(hasInFilter))
        .dedup()
        .toList.toSet
  }

  private def addImportsFromNewClasses(graph: FullGraph, classes: Iterable[Ast], allClasses: Set[String]) {
    for {
      (outCls, imported) <- imports.resolve(classes, allClasses: Set[String])
      inCls <- imported
    } for {
      inVertex <- classesNamed(graph, inCls)
      outVertex <- classesNamed(graph, outCls)
    } {
      graph.addEdge(outVertex, "cls-imports-cls", inVertex)
    }
  }

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
    graphs.get(name)

  override def getJson(name: String) =
    importedGraphs.get(name)

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
        from <- graph.currentVertices.has("kind", "package").has("name", fromPackage).toList
        to <- graph.currentVertices.has("kind", "package").has("name", toPackage).toList
      } {
        val e = graph.addEdge(from.asInstanceOf[Vertex], "calls", to.asInstanceOf[Vertex])
        e.setProperty("count", count.toString)
      }
    }
  }
}

private case class ParsedFile(ast: Ast, content: String, fileName: String, oFileId: Option[String])
