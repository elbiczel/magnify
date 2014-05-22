package controllers

import scala.collection.JavaConversions._

import com.tinkerpop.blueprints.{Graph => _, _}
import com.tinkerpop.blueprints.Direction._
import magnify.features.Sources
import magnify.model.graph._
import magnify.modules.inject
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.libs.json.Writes._
import play.api.mvc._
import com.google.inject.TypeLiteral

object ShowGraph extends ShowGraph(
    inject[Sources],
    inject[java.util.Map[Class[_ <: GraphView], GraphViewFactory]](
      new TypeLiteral[java.util.Map[Class[_ <: GraphView], GraphViewFactory]]() {}).toMap)

sealed class ShowGraph (
    protected override val sources: Sources,
    graphViewFactories: Map[Class[_ <: GraphView], GraphViewFactory])
  extends Controller with ProjectList {

  def show[A](name: String) = Action { implicit request =>
    if (sources.list.contains(name)) {
      showHtml(name)
    } else {
      projectNotFound(name)
    }
  }

  private def showHtml(projectName: String)(implicit request: Request[AnyContent]): Result =
    sources.get(projectName) match {
      case Some(_) => Ok(views.html.show(projectName))
      case None => Ok(views.html.showJson(projectName))
    }

  private def projectNotFound(projectName: String)(implicit request: Request[AnyContent]): Result = {
    val message = "Project \"%s\" was not found. Why not create new one?" format projectName
    Redirect(routes.ZipSourcesUpload.form()).flashing("warning" -> message)
  }

  def showCustomJson(name: String) = Action { implicit request =>
    withGraph(name) { graph =>
        val factory = graphViewFactories(classOf[CustomGraphView])
        Ok(json(factory(graph, request.getQueryString("rev").filter(_.trim.nonEmpty))))
    }
  }

  def showPackagesJson(name: String) = Action { implicit request =>
    withGraph(name) { graph =>
      val factory = graphViewFactories(classOf[PackagesGraphView])
      Ok(json(factory(graph, request.getQueryString("rev").filter(_.trim.nonEmpty))))
    }
  }

  def showPkgImportsJson(name: String) = Action { implicit request =>
    withGraph(name) { graph =>
      val factory = graphViewFactories(classOf[PackageImportsGraphView])
      Ok(json(factory(graph, request.getQueryString("rev").filter(_.trim.nonEmpty))))
    }
  }

  def showClsImportsJson(name: String) = Action { implicit request =>
    withGraph(name) { graph =>
      val factory = graphViewFactories(classOf[ClassImportsGraphView])
      Ok(json(factory(graph, request.getQueryString("rev").filter(_.trim.nonEmpty))))
    }
  }

  def committers(name: String) = Action { implicit request =>
    withGraph(name) { graph =>
      Ok(toJson(Committers(request.getQueryString("rev").filter(_.trim.nonEmpty), graph)))
    }
  }

  def revisions(name: String) = Action { implicit request =>
    withGraph(name) { graph =>
      Ok(toJson(Revisions(request.getQueryString("rev").filter(_.trim.nonEmpty), graph)))
    }
  }

  private def withGraph(name: String)(action: FullGraph => Result)(implicit request: Request[AnyContent]): Result =
    sources.get(name) match {
      case Some(graph) => action(graph)
      case None =>
        sources.getJson(name) match {
          case Some(json) => Ok(json.getContents)
          case None => projectNotFound
        }
    }

  private def projectNotFound(implicit request: Request[AnyContent]): Result =
    NotFound(toJson(Map("warning" -> "Project was not found.")))


  private def json(graphView: GraphView): JsValue = {
    val vertices = toMap(graphView.vertices)
    val idByVertexName = (for {
      (vertex, index) <- vertices.zipWithIndex
    } yield vertex("name") -> index).toMap
    val edges = toMap(graphView.edges, idByVertexName)
    JsObject(Seq("nodes" -> toJson(vertices), "edges" -> toJson(edges)))
  }

  private def toMap(vertices: Iterable[Vertex]): Seq[Map[String, String]] =
    for (vertex <- vertices.toSeq) yield {
      val name = vertex.getProperty("name").toString
      val kind = vertex.getProperty("kind").toString
      val pageRank = Option(vertex.getProperty[String]("page-rank")).getOrElse("")
      Map("name" -> name, "kind" -> kind, "page-rank" -> pageRank) ++ property(vertex, "metric--avg-lines-of-code")
    }

  private def property(v: Element, name: String): Map[String, String] =
    Option(v.getProperty(name).asInstanceOf[Object]).map(value => Map(name -> value.toString)).getOrElse(Map())

  private def toMap(edges: Iterable[Edge], idByVertexName: Map[String, Int]): Seq[Map[String, JsValue]] =
    for {
      edge <- edges.toSeq
      source <- idByVertexName.get(name(edge, OUT)).toSeq
      target <- idByVertexName.get(name(edge, IN)).toSeq
    } yield Map(
      "source" -> toJson(source),
      "target" -> toJson(target),
      "kind" -> toJson(edge.getLabel)) ++ property(edge, "count").mapValues(s => toJson(s))

  private def name(edge: Edge, direction: Direction): String =
    edge.getVertex(direction).getProperty("name").toString
}
