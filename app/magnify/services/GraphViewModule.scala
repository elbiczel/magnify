package magnify.services

import com.google.inject.{AbstractModule, TypeLiteral}
import com.google.inject.multibindings.MapBinder
import magnify.common.reflect._
import magnify.model.graph._

private[services] class GraphViewModule extends AbstractModule {
  override def configure(): Unit = {
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
  }
}
