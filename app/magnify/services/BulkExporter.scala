package magnify.services

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}

import com.google.inject.name.Named
import com.tinkerpop.blueprints.Vertex
import magnify.features.RevisionGraphFactory
import magnify.features.view.ElementView
import magnify.model.graph.{Actions, FullGraph}
import org.apache.commons.lang3.StringEscapeUtils
import play.api.libs.iteratee.Enumerator

class BulkExporter(
    revisionGraphFactory: RevisionGraphFactory,
    @Named("ServicesPool") implicit val pool: ExecutionContext)
  extends (FullGraph => Enumerator[String])
  with Actions
  with ElementView {

  override def apply(graph: FullGraph): Enumerator[String] = {
    val revProvider = RevProvider(graph)
    val artefactRowsFn = ArtefactRows(graph)
    Enumerator.generateM(Future[Option[String]] {
      val rev = revProvider.take()
      val oRevRow = rev.map(RevRow)
      val oArtefactRows = rev.map(artefactRowsFn)
      val oRows = for (revRow <- oRevRow; artefactRows <- oArtefactRows) yield {
        val allKeys = artefactRows.map(_.keySet).foldLeft(revRow.keySet)(_ ++ _).toSeq.sorted
        val header = allKeys.map(StringEscapeUtils.escapeCsv(_)).mkString(",")
        val revRowValues = allKeys.map(revRow.getOrElse(_, "")).map(StringEscapeUtils.escapeCsv(_))
        val revRowString = revRowValues.mkString(",")
        val artefactRowStrings = artefactRows.map { artefactRow =>
          val artefactRowValues = allKeys.map(artefactRow.getOrElse(_, "")).map(StringEscapeUtils.escapeCsv(_))
          val artefactRowString = artefactRowValues.mkString(",")
          artefactRowString
        }
        (header +: (revRowString +: artefactRowStrings))
      }
      oRows.map(_.mkString("\n") + "\n")
    })
  }

  private case class RevProvider(graph: FullGraph) {
    private var current: Option[Vertex] = graph.getHeadCommitVertex.iterator().toSeq.headOption

    def take(): Option[Vertex] = this.synchronized {
      val result = current
      current = current.flatMap(getPrevRevision(_))
      result
    }
  }

  private object RevRow extends (Vertex => Map[String, String]) {
    override def apply(rev: Vertex) = revisionValues(rev) ++ additionalRevData(rev)
  }

  private case class ArtefactRows(graph: FullGraph) extends (Vertex => Seq[Map[String, String]]) {
    override def apply(rev: Vertex) = {
      val revisionGraph = revisionGraphFactory(graph, Some(rev.getProperty[String]("rev")))
      val revVals = revisionValues(rev)
      val revisionArtefacts = revisionGraph.vertices.iterator()
      revisionArtefacts.toSeq.map { artefact =>
        revVals ++ artefactValues(artefact)
      }
    }
  }

  def revisionValues(rev: Vertex): Map[String, String] = {
    Map(
      "revision-timestamp" -> timestamp(rev),
      "revision-author" -> getName(rev),
      "revision-id" -> rev.getProperty("rev"),
      "revision-message" -> rev.getProperty("desc")
    ) // TODO: Add parent revision IDs
  }

  def artefactValues(artefact: Vertex): Map[String, String] = Map(
    "artefact-kind" -> artefact.getProperty("kind"),
    "artefact-name" -> artefact.getProperty("name")
  ) ++ additionalRevData(artefact) ++ pageRankMap(artefact)
}
