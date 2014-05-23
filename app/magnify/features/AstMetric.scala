package magnify.features

import japa.parser.ast.body.TypeDeclaration

// TODO(biczel): Types are hard
trait AstMetric extends (TypeDeclaration => AnyRef) {

  def name: String
}
