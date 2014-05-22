package magnify.services

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import com.google.inject.{AbstractModule, TypeLiteral}
import com.google.inject.multibindings.{MapBinder, Multibinder}
import com.google.inject.name.Names
import magnify.common.reflect.constructor
import magnify.features._
import magnify.model.graph._
import magnify.services.metrics._

/**
 * @author Cezary Bartoszuk (cezarybartoszuk@gmail.com)
 */
final class Services extends AbstractModule {
  def configure() {
    // TODO(biczel): Add dependencies between metrics
    // TODO(biczel): Add CC metric:
    // http://gmetrics.sourceforge.net/gmetrics-CyclomaticComplexityMetric.html
    // http://stackoverflow.com/questions/12355783/tools-to-automate-calculation-of-cyclomatic-complexity-in-java
    // http://stackoverflow.com/questions/13571635/how-to-calculate-cyclomatic-complexity-of-a-project-not-a-class-function
    val metricsBinder = Multibinder.newSetBinder(binder(), classOf[Metric])
    metricsBinder.addBinding().toConstructor(constructor[LoggedExperienceMetric])
    metricsBinder.addBinding().toConstructor(constructor[LoggedContributionMetric])
    val pkgMetricsBinder = Multibinder.newSetBinder(binder(), classOf[RevisionMetric])
    pkgMetricsBinder.addBinding().toConstructor(constructor[LoggedRevisionLocMetric])
    pkgMetricsBinder.addBinding().toConstructor(constructor[LoggedRevisionAvgLocMetric])
    pkgMetricsBinder.addBinding().toConstructor(constructor[LoggedRevisionContributionMetric])
    pkgMetricsBinder.addBinding().toConstructor(constructor[LoggedRevisionExperienceMetric])
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
