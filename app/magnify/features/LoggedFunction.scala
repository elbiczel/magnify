package magnify.features

import play.api.Logger

trait LoggedFunction[A, B] extends (A => B) {
  protected val logger: Logger

  abstract override def apply(a: A): B = {
    logger.info { "starting: " + System.nanoTime() }
    val result = super.apply(a)
    logger.info { "finished: " + System.nanoTime() }
    result
  }
}
