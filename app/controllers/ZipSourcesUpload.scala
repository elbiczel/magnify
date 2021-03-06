package controllers

import java.io.File
import java.lang.String

import scala.concurrent.{ExecutionContext, Future}

import magnify.features.Sources
import magnify.model.{Git, Json, Zip}
import magnify.modules.inject
import play.api.Logger
import play.api.libs.Files
import play.api.mvc._

/**
 * @author Cezary Bartoszuk (cezary@codilime.com)
 */
object ZipSourcesUpload extends ZipSourcesUpload(inject[Sources], inject[ExecutionContext]("UiPool"))

sealed class ZipSourcesUpload (protected override val sources: Sources, implicit val pool: ExecutionContext)
    extends Controller with ProjectList {

  private val logger = Logger(classOf[ZipSourcesUpload].getSimpleName)

  private type MultipartRequest = Request[MultipartFormData[Files.TemporaryFile]]

  private val allowedFormats = Set("application/zip", "application/x-java-archive",
    "application/json", "application/x-javascript", "text/javascript",
    "text/x-javascript", "text/x-json", "application/octet-stream", "application/java-archive")

  private val progress = "success" -> "Project uploaded. Interpreting in background."

  def form = Action { implicit request =>
    Ok(views.html.newProject())
  }

  def upload = Action(parse.multipartFormData) { implicit request =>
    for (file <- projectSources(request)) Future {
      for (name <- projectName) sources.add(name, new Zip(file))
    }
    Redirect(routes.ZipSourcesUpload.form()).flashing(progress)
  }

  def uploadJson = Action(parse.multipartFormData) { implicit request =>
    for (file <- projectSources(request)) Future {
      for (name <- projectName) {
        sources.add(name, new Json(file))
      }
    }
    Redirect(routes.ZipSourcesUpload.form()).flashing(progress)
  }

  def uploadGit = Action(parse.multipartFormData) { implicit request =>
    for (path <- gitPath; name <- projectName) Future {
      sources.add(name, Git(path, gitBranch), gitPrefixes)
    }.recover {
      case t: Throwable => {
        logger.error("Error processing project: " + name, t)
        throw t
      }
    }
    Redirect(routes.ZipSourcesUpload.form()).flashing(progress)
  }

  private def projectName(implicit request: MultipartRequest): Option[String] =
    getForm("project-name", request)

  private def gitPath(implicit request: MultipartRequest): Option[String] =
    getForm("project-git-path", request)

  private def gitBranch(implicit request: MultipartRequest): Option[String] =
    getForm("project-git-branch", request)

  private def gitPrefixes(implicit request: MultipartRequest): Set[String] =
    getForm("project-git-prefixes", request).getOrElse("").split(",").toSet.map((s: String) => s.trim)

  private def getForm(name: String, request: MultipartRequest) =
    request.body.dataParts.get(name).flatMap {
      case Seq(onlyName) => Some(onlyName).filter(_.trim.nonEmpty)
      case _ => None
    }

  private def projectSources(request: MultipartRequest): Option[File] =
    for {
      filePart <- request.body.file("project-sources")
      if filePart.contentType.map(allowedFormats) getOrElse false
    } yield {
      val newFile = File.createTempFile(filePart.filename, ".tmp.zip")
      filePart.ref.moveTo(newFile, replace=true)
      newFile
    }
}
