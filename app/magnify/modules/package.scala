package magnify

import com.google.inject.{TypeLiteral, Guice, Key}
import com.google.inject.name.Names
import magnify.features.Features
import magnify.services.Services

/**
 * @author Cezary Bartoszuk (cezarybartoszuk@gmail.com)
 */
package object modules {
  private val injector = Guice.createInjector(new Features(), new Services())

  def inject[A](implicit manifest: Manifest[A]): A =
    injector.getInstance(manifest.runtimeClass.asInstanceOf[Class[A]])

  def inject[A](name: String)(implicit manifest: Manifest[A]): A =
    injector.getInstance(Key.get(manifest.runtimeClass.asInstanceOf[Class[A]], Names.named(name)))

  def inject[A](typeLiteral: TypeLiteral[A]): A =
    injector.getInstance(Key.get(typeLiteral))
}
