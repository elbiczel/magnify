package magnify.features

import magnify.model.VersionedArchive
import magnify.model.graph.FullGraph

trait FullGraphFactory {
  def tinker(archive: VersionedArchive): FullGraph

  def load(fileName: String): FullGraph
}
