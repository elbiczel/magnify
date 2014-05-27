package magnify.services.metrics

import scala.collection.mutable

import japa.parser.ast.body.{ConstructorDeclaration, MethodDeclaration, TypeDeclaration}
import japa.parser.ast.expr.{BinaryExpr, ConditionalExpr}
import japa.parser.ast.stmt._
import japa.parser.ast.visitor.VoidVisitorAdapter
import magnify.features.{AstMetric, MetricNames}

// Implemented as defined: http://docs.codehaus.org/display/SONAR/Metrics+-+Complexity
private[this] class McCabeCcVisitor extends VoidVisitorAdapter[Object] {

  private var methodValue: Int = 0
  private val methodsCC = mutable.Buffer[Int]()

  override def visit(n: MethodDeclaration, arg: Object): Unit = {
    nextMethod()
    super.visit(n, arg)
  }

  override def visit(n: ConstructorDeclaration, arg: Object): Unit = {
    nextMethod()
    super.visit(n, arg)
  }

  override def visit(n: WhileStmt, arg: Object): Unit = {
    methodValue += 1
    super.visit(n, arg)
  }

  override def visit(n: ThrowStmt, arg: Object): Unit = {
    methodValue += 1
    super.visit(n, arg)
  }

  override def visit(n: SwitchEntryStmt, arg: Object): Unit = {
    methodValue += 1
    super.visit(n, arg)
  }

  override def visit(n: ReturnStmt, arg: Object): Unit = {
    methodValue += 1
    super.visit(n, arg)
  }

  override def visit(n: ForStmt, arg: Object): Unit = {
    methodValue += 1
    super.visit(n, arg)
  }

  override def visit(n: ForeachStmt, arg: Object): Unit = {
    methodValue += 1
    super.visit(n, arg)
  }

  override def visit(n: DoStmt, arg: Object): Unit = {
    methodValue += 1
    super.visit(n, arg)
  }

  override def visit(n: ConditionalExpr, arg: Object): Unit = {
    methodValue += 1
    super.visit(n, arg)
  }

  override def visit(n: CatchClause, arg: Object): Unit = {
    methodValue += 1
    super.visit(n, arg)
  }

  override def visit(n: BinaryExpr, arg: Object): Unit = {
    methodValue += 1
    super.visit(n, arg)
  }

  // http://stackoverflow.com/questions/13571635
  def getComplexity: Double = {
    nextMethod()
    if (methodsCC.isEmpty) { 0.0 } else {
      methodsCC.filter(_ >= 6).length * 100 / Math.sqrt(methodsCC.length)
    }
  }

  private def nextMethod(): Unit = if (methodValue > 0) {
    methodsCC += methodValue
    methodValue = 0
  }
}

class ClassMcCabeCyclomaticComplexityMetric extends AstMetric {
  override def apply(typeUnit: TypeDeclaration): AnyRef = {
    val visitor = new McCabeCcVisitor
    typeUnit.accept(visitor, null)
    visitor.getComplexity.asInstanceOf[AnyRef]
  }

  override final val name: String = MetricNames.mcCabeCyclomaticComplexity
}
