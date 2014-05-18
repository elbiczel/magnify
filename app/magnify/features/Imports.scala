package magnify.features

import magnify.model.Ast

/**
 * @author Cezary Bartoszuk (cezarybartoszuk@gmail.com)
 */
trait Imports {
  /**
   * Resolves imports from given `Ast` `Seq` yielding a who imports who `Map` containing classes
   * FQNs.
   */
  def resolve(classes: Iterable[Ast], allClasses: Set[String]): Map[String, Seq[String]]
}
