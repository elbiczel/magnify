package magnify.services

import scala.concurrent.{ExecutionContext, Future}

import com.google.inject.name.Named
import magnify.features.RevisionGraphFactory
import magnify.model.graph.FullGraph
import play.api.libs.iteratee.Enumerator

class BulkExporter(
    revisionGraphFactory: RevisionGraphFactory,
    @Named("ServicesPool") implicit val pool: ExecutionContext)
  extends (FullGraph => Enumerator[String]) {

  override def apply(v1: FullGraph): Enumerator[String] = {
    var x = 0
    Enumerator.generateM(Future[Option[String]] {
      if (x == 5) None
      else {
        x += 1
        Thread.sleep(1000)
        Some(x.toString + "\n")
      }
    })
  }
}
