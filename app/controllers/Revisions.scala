package controllers

import java.text.SimpleDateFormat
import java.util.Date

import magnify.model.graph.FullGraph

object Revisions {

  val format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss")

  def apply(revision: Option[String], graph: FullGraph, fullDetails: Boolean): Seq[Map[String, String]] = {
    val commits = graph.commitsOlderThan(revision)
    commits.map(Revision(_, fullDetails))
  }
}
