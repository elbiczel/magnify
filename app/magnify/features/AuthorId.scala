package magnify.features

import com.tinkerpop.blueprints.Vertex

trait AuthorId {

  final protected def getName(authorWithTime: String): String = {
    val endEmailIndex = authorWithTime.lastIndexOf('<')
    authorWithTime.substring(0, endEmailIndex - 1)
  }

  final protected def getName(v: Vertex): String = getName(v.getProperty[String]("author"))

}
