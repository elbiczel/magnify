package magnify.services

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import com.google.inject.{AbstractModule, TypeLiteral}
import com.google.inject.name.Names
import japa.parser.ast.body.TypeDeclaration
import magnify.common.reflect.constructor
import magnify.features._
import magnify.model.graph.{FullGraph, Graph}
import magnify.services.imports.AstBuilder
import magnify.services.metrics._
import com.tinkerpop.blueprints.Vertex

/**
 * @author Cezary Bartoszuk (cezarybartoszuk@gmail.com)
 */
final class Services extends AbstractModule {
  def configure() {
    install(new MetricsModule)
    install(new GraphViewModule)
    bind(classOf[ExecutionContext])
        .annotatedWith(Names.named("ServicesPool"))
        .toInstance(ExecutionContext.fromExecutorService(Executors.newCachedThreadPool()))
    bind(classOf[AstBuilder]).toConstructor(constructor[AstBuilder])
    bind(classOf[Parser]).toConstructor(constructor[ClassAndImportsParser])
    bind(classOf[Imports]).toConstructor(constructor[ProjectImports])
    bind(classOf[FullGraphFactory]).toConstructor(constructor[FullGraphFactoryImpl])
    bind(classOf[RevisionGraphFactory]).toConstructor(constructor[RevisionGraphFactoryImpl])
    bind(new TypeLiteral[MetricsProvider[FullGraph, FullGraph, FullGraphMetric]]() {})
        .toConstructor(constructor[FullGraphDependenciesResolvedMetricsProvider])
    bind(new TypeLiteral[MetricsProvider[(Graph, Vertex), Graph, RevisionMetric]]() {})
        .toConstructor(constructor[RevisionDependenciesResolvedMetricsProvider])
    bind(new TypeLiteral[MetricsProvider[TypeDeclaration, AnyRef, AstMetric]]() {})
        .toConstructor(constructor[AstDependenciesResolvedMetricsProvider])
  }
}
