package magnify.services

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import com.google.inject.AbstractModule
import magnify.common.reflect.constructor
import magnify.features.{LoggedMetric, Metric, Imports, Parser}
import com.google.inject.multibindings.Multibinder
import magnify.services.metrics.ExperienceMetric

/**
 * @author Cezary Bartoszuk (cezarybartoszuk@gmail.com)
 */
final class Services extends AbstractModule {
  def configure() {
    val metricsBinder = Multibinder.newSetBinder(binder(), classOf[Metric])
    metricsBinder.addBinding().toInstance(new ExperienceMetric with LoggedMetric)
    bind(classOf[Parser]).toConstructor(constructor[ClassAndImportsParser])
    bind(classOf[Imports]).toConstructor(constructor[ProjectImports])
    bind(classOf[ExecutionContext]).toInstance(ExecutionContext.fromExecutorService(Executors.newCachedThreadPool()))
  }
}
