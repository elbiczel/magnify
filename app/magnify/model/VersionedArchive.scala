package magnify.model

import java.io.File

import scala.io.Source

import scalaz.Monoid

sealed trait VersionedArchive  {

  def extract[A : Monoid](f: (Archive, ChangeDescription) => A): A

  def getContent(objectId: String): String = ???

  def save(fileName: String): Unit

  def archiveTypeName: String
}

object VersionedArchive {

  val GitArchiveName = classOf[GitArchive].getSimpleName
  val SingleVersionName = classOf[SingleVersion].getSimpleName

  def load(fileName: String): VersionedArchive = {
    val lines = Source.fromFile(fileName).getLines().toSeq
    lines.headOption match {
      case Some(GitArchiveName) => {
        val repoPath = lines(1)
        val branch = Option(lines(2)).filter(_.trim.nonEmpty)
        Git(repoPath, branch)
      }
      case Some(SingleVersionName) => {
        val zipPath = lines(1)
        new SingleVersionArchive(new Zip(new File(zipPath)))
      }
    }
  }
}

trait GitArchive extends VersionedArchive
trait SingleVersion extends VersionedArchive
