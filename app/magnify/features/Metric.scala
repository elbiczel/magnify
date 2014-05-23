package magnify.features


trait Metric[A, B] extends (A => B) {
  def name: String

  def dependencies: Set[String] = Set()
}
