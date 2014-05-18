package magnify.features

import scala.concurrent.ExecutionContext

import com.google.inject.{AbstractModule, Scopes}
import magnify.common.reflect.constructor

/**
 * @author Cezary Bartoszuk (cezarybartoszuk@gmail.com)
 */
final class Features extends AbstractModule {
  def configure() {
    requireBinding(classOf[Imports])
    requireBinding(classOf[Parser])
    requireBinding(classOf[ExecutionContext])
    bind(classOf[Sources]).toConstructor(constructor[GraphSources]).in(Scopes.SINGLETON)
  }
}
