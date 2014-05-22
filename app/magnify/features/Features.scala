package magnify.features

import scala.concurrent.ExecutionContext

import com.google.inject.{AbstractModule, Scopes}
import magnify.common.reflect.constructor
import com.google.inject.multibindings.Multibinder
import java.util.concurrent.Executors
import com.google.inject.name.Names

/**
 * @author Cezary Bartoszuk (cezarybartoszuk@gmail.com)
 */
final class Features extends AbstractModule {
  def configure() {
    Multibinder.newSetBinder(binder(), classOf[Metric])
    requireBinding(classOf[Imports])
    requireBinding(classOf[Parser])
    requireBinding(classOf[FullGraphFactory])
    requireBinding(classOf[RevisionGraphFactory])
    bind(classOf[ExecutionContext])
        .annotatedWith(Names.named("UiPool"))
        .toInstance(ExecutionContext.fromExecutorService(Executors.newCachedThreadPool()))
    bind(classOf[Sources]).toConstructor(constructor[GraphSources]).in(Scopes.SINGLETON)
  }
}
