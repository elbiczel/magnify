package magnify.services

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import com.google.inject.{AbstractModule, TypeLiteral}
import com.google.inject.multibindings.{MapBinder, Multibinder}
import com.google.inject.name.Names
import magnify.common.reflect.constructor
import magnify.features._
import magnify.model.graph._
import magnify.services.metrics.{LoggedContributionMetric, LoggedExperienceMetric}

/**
 * @author Cezary Bartoszuk (cezarybartoszuk@gmail.com)
 */
final class Services extends AbstractModule {
  def configure() {
    val metricsBinder = Multibinder.newSetBinder(binder(), classOf[Metric])
    metricsBinder.addBinding().toConstructor(constructor[LoggedExperienceMetric])
    metricsBinder.addBinding().toConstructor(constructor[LoggedContributionMetric])
    val graphViewFactories = MapBinder.newMapBinder(
      binder(),
      new TypeLiteral[Class[_ <: GraphView]]() {},
      new TypeLiteral[GraphViewFactory]() {})
    graphViewFactories
        .addBinding(classOf[ClassImportsGraphView])
        .toConstructor(constructor[ClassImportsGraphViewFactory])
    graphViewFactories
        .addBinding(classOf[CustomGraphView])
        .toConstructor(constructor[CustomGraphViewFactory])
    graphViewFactories
        .addBinding(classOf[PackageImportsGraphView])
        .toConstructor(constructor[PackageImportsGraphViewFactory])
    graphViewFactories
        .addBinding(classOf[PackagesGraphView])
        .toConstructor(constructor[PackagesGraphViewFactory])
    bind(classOf[ExecutionContext])
        .annotatedWith(Names.named("ServicesPool"))
        .toInstance(ExecutionContext.fromExecutorService(Executors.newCachedThreadPool()))
    bind(classOf[Parser]).toConstructor(constructor[ClassAndImportsParser])
    bind(classOf[Imports]).toConstructor(constructor[ProjectImports])
    bind(classOf[FullGraphFactory]).toConstructor(constructor[FullGraphFactoryImpl])
    bind(classOf[RevisionGraphFactory]).toConstructor(constructor[RevisionGraphFactoryImpl])
  }
}
