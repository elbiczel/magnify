package magnify.features

object MetricNames {

  val linesOfCode: String = "loc"
  val averageLinesOfCode: String = "avg-loc"
  val contribution: String = "contr"
  val aggregatedContribution: String = "aggr-cont"
  val experience: String = "exp"
  val mcCabeCyclomaticComplexity: String = "mccabe-cc"
  val pageRank: String = "pr"
  val distinctAuthors: String = "dist-auth"
  def owner: String = "owner"

  def propertyName(metricName: String) = "metric--" + metricName
}
