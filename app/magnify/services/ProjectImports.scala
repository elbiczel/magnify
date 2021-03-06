package magnify.services

import magnify.features.Imports
import magnify.model.Ast
import play.api.Logger

private[services] final class ProjectImports extends Imports {
  val logger = Logger(classOf[ProjectImports].getSimpleName)

  /**
   * Resolves only explicit imports as:
   *
   * {{{
   *   import magnify.model.Ast
   * }}}
   *
   * Does not resolve implicit "same package imports", asterisk imports and static imports.
   */
  override def resolve(classes: Iterable[Ast], allClasses: Set[String]): Map[String, Seq[String]] = {
    val imports = for {
      Ast(name, imports, asteriskPackages, unresolvedClasses, _) <- classes
    } yield {
      val possibleImports = for (
        packageName <- asteriskPackages;
        className <- unresolvedClasses
      ) yield (packageName + "." + className)
      val implicitImports = (possibleImports ++ unresolvedClasses).filter(allClasses)
      logger.debug("implicitImports in " + name + " : " + implicitImports.mkString(", "))
      val allImports = (imports.filter(allClasses) ++ implicitImports) - name
      (name -> allImports.toSeq)
    }
    imports.toMap
  }
}
