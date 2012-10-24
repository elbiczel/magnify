package magnify.services

import java.io.InputStream
import magnify.model.java.Ast
import japa.parser.ast.CompilationUnit
import japa.parser.JavaParser
import japa.parser.ast.expr.{QualifiedNameExpr, NameExpr}

import java.util.Collections
import scala.annotation.tailrec
import scala.collection.JavaConversions._

/**
 * @author Cezary Bartoszuk (cezarybartoszuk@gmail.com)
 */
final class JavaParse extends (InputStream => Seq[Ast]) {
  def apply(input: InputStream): Seq[Ast] = {
    val unit = JavaParser.parse(input)
    val imports = getImports(unit)
    val prefix = packagePrefix(unit)
    for {
      className <- getClassNames(unit)
    } yield Ast(imports, (prefix ++ List(className)).mkString("."))
  }

  private def getImports(unit: CompilationUnit): Seq[String] =
    for {
      anyImport <- orEmpty(unit.getImports).toSeq
      if !anyImport.isStatic && !anyImport.isAsterisk
    } yield extractName(anyImport.getName).mkString(".")

  private def orEmpty[A](value: java.util.List[A]): java.util.List[A] =
    Option(value).getOrElse(Collections.emptyList[A])

  private def getClassNames(unit: CompilationUnit): Seq[String] =
    for {
      typ <- orEmpty(unit.getTypes).toSeq
      if typ.getName ne null
    } yield typ.getName

  private def packagePrefix(unit: CompilationUnit): Seq[String] =
    if (unit.getPackage ne null) {
      extractName(unit.getPackage.getName)
    } else {
      Seq()
    }

  @tailrec
  private def extractName(expr: NameExpr, prefix: List[String] = List.empty[String]): Seq[String] =
    expr match {
      case e: QualifiedNameExpr => extractName(e.getQualifier, e.getName :: prefix)
      case e: NameExpr => e.getName :: prefix
    }
}
