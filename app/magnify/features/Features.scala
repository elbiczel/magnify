package magnify.features

import scala.concurrent.ExecutionContext

import com.google.inject.{AbstractModule, Scopes}
import magnify.common.reflect.constructor
import com.google.inject.multibindings.Multibinder

/**
 * @author Cezary Bartoszuk (cezarybartoszuk@gmail.com)
 */
final class Features extends AbstractModule {
  def configure() {
    Multibinder.newSetBinder(binder(), classOf[Metrics])
    requireBinding(classOf[Imports])
    requireBinding(classOf[Parser])
    requireBinding(classOf[ExecutionContext])
    bind(classOf[Sources]).toConstructor(constructor[GraphSources]).in(Scopes.SINGLETON)
  }
}
