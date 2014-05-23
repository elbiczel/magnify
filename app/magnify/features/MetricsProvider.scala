package magnify.features

trait MetricsProvider[A, B, C <: Metric[A, B]] extends (() => Seq[C])
