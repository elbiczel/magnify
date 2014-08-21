package magnify.features.view

import magnify.model.graph.FullGraph

object Revisions {

  def apply(revision: Option[String], graph: FullGraph, fullDetails: Boolean): Seq[Map[String, String]] = {
    val commits = graph.commitsOlderThan(revision)
    commits.map(Revision(_, fullDetails))
  }
}
