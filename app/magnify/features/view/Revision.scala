package magnify.features.view

import java.text.SimpleDateFormat
import java.util.Date

import com.tinkerpop.blueprints.Vertex
import magnify.features.MetricNames
import magnify.model.graph.FullGraph

object Revision extends ((Vertex, Boolean) => Map[String, String]) {
  val format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss")

  def apply(revision: Option[String], graph: FullGraph): Map[String, String] = {
    apply(graph.revVertex(revision), true)
  }

  override def apply(vrtx: Vertex, fullDetails: Boolean): Map[String, String] = {
    val rev = vrtx.getProperty[String]("rev")
    val desc = vrtx.getProperty[String]("desc")
    val author = Committers.getName(vrtx.getProperty[String]("author"))
    val committer = Committers.getName(vrtx.getProperty[String]("committer"))
    val time = format.format(new Date(vrtx.getProperty[Integer]("time") * 1000L))
    val additionalData = if (fullDetails) { additionalRevData(vrtx) } else { Map() }
    Map("id" -> rev, "description" -> desc, "author" -> author, "committer" -> committer, "time" -> time) ++
        additionalData
  }

  private def additionalRevData(vrtx: Vertex): Map[String, String] = {
    val aggregatedContribution = vrtx.getProperty[Map[String, Double]](
      MetricNames.propertyName(MetricNames.aggregatedContribution)).map { case (author, contribution) =>
      (MetricNames.propertyName(MetricNames.aggregatedContribution) + "---" + author -> contribution.toString)
    }
    val experience = vrtx.getProperty[Map[String, Double]](MetricNames.propertyName(MetricNames.experience))
      .map { case (author, exp) =>
      (MetricNames.propertyName(MetricNames.experience) + "---" + author -> exp.toString)
    }
    val contribution = vrtx.getProperty[Double](MetricNames.propertyName(MetricNames.contribution))
    val complexity = vrtx.getProperty[Double](MetricNames.propertyName(MetricNames.mcCabeCyclomaticComplexity))
    val loc = vrtx.getProperty[Double](MetricNames.propertyName(MetricNames.linesOfCode))
    val avgLoc = vrtx.getProperty[Double](MetricNames.propertyName(MetricNames.averageLinesOfCode))
    aggregatedContribution ++ experience ++ Map(
      MetricNames.propertyName(MetricNames.contribution) -> contribution.toString,
      MetricNames.propertyName(MetricNames.mcCabeCyclomaticComplexity) -> complexity.toString,
      MetricNames.propertyName(MetricNames.linesOfCode) -> loc.toString,
      MetricNames.propertyName(MetricNames.averageLinesOfCode) -> avgLoc.toString)
  }
}
