package magnify.services

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import magnify.common.reflect.constructor
import magnify.features._
import magnify.services.metrics._

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
    bind(classOf[Parser]).toConstructor(constructor[ClassAndImportsParser])
    bind(classOf[Imports]).toConstructor(constructor[ProjectImports])
    bind(classOf[FullGraphFactory]).toConstructor(constructor[FullGraphFactoryImpl])
    bind(classOf[RevisionGraphFactory]).toConstructor(constructor[RevisionGraphFactoryImpl])
  }
}
