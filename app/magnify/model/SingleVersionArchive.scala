package magnify.model

import java.io.FileWriter

import scalaz.Monoid

final class SingleVersionArchive(archive: Zip) extends SingleVersion {

  val changedFiles = archive.extract((fileName, oObjectId, content) => Set(fileName))
  val diff = ChangeDescription("", "", "", "", 0, changedFiles, Set[String]())

  override def extract[A: Monoid](f: (Archive, ChangeDescription) => A): A = f(archive, diff)

  override def save(fileName: String): Unit = {
    val writer = new FileWriter(fileName)
    writer
        .append(archiveTypeName)
        .append("\n")
        .append(archive.file.getAbsolutePath)
        .append("\n")
    writer.flush()
    writer.close()
  }
  
  override val archiveTypeName: String = classOf[SingleVersion].getSimpleName
}
