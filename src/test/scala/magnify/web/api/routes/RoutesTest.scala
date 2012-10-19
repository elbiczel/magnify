package magnify.web.api.routes

import magnify.testing.{GuiceTestModules, ActorsSuite}

import org.scalatest.matchers.ShouldMatchers

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.TestKit
import cc.spray.Route
import com.google.inject._
import com.google.inject.name.Names
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
 * @author Cezary Bartoszuk (cezarybartoszuk@gmail.com)
 */
@RunWith(classOf[JUnitRunner])
final class RoutesTest extends TestKit(ActorSystem()) with FunSuite with ActorsSuite
    with ShouldMatchers with GuiceTestModules {

  val injector = Guice.createInjector(new ActorsModule(), new Routes)

  test("should inject root service actor") {
    val key = Key.get(classOf[ActorRef], Names.named("root-service"))
    injector.getInstance(key)
  }

  test("should not expose routes") {
    intercept[ConfigurationException] {
      val key = Key.get(new TypeLiteral[Set[Route]]() {})
      injector.getInstance(key)
    }
  }
}
