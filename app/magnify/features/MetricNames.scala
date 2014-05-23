package magnify.features

object MetricNames {

  val linesOfCode: String = "loc"
  val averageLinesOfCode: String = "avg-loc"
  val contribution: String = "contr"
  val aggregatedContribution: String = "aggr-cont"
  val experience: String = "exp"
  val mcCabeCyclomaticComplexity: String = "mcCabeCC"
  val pageRank: String = "pr"

  def propertyName(metricName: String) = "metric--" + metricName
}
