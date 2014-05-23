package magnify.features

import japa.parser.ast.body.TypeDeclaration

// TODO(biczel): Types are hard
trait AstMetric extends Metric[TypeDeclaration, AnyRef]
