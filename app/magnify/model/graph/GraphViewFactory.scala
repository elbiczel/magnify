package magnify.model.graph

trait GraphViewFactory extends ((FullGraph, Option[String]) => GraphView)
