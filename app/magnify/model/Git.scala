package magnify.model

import java.io.{ByteArrayInputStream, File, FileWriter, InputStream}

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.JavaConversions._

import org.eclipse.jgit.diff.{DiffFormatter, RawTextComparator}
import org.eclipse.jgit.lib.{Constants, MutableObjectId, Repository}
import org.eclipse.jgit.revwalk.{RevWalk, RevCommit}
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.util.io.DisabledOutputStream
import play.api.Logger
import scalaz.Monoid
import org.eclipse.jgit.revwalk.filter.RevFilter

/**
 * @author Tomasz Biczel (tomasz@biczel.com)
 */
private[this] final class Git(repo: Repository, branch: Option[String]) extends GitArchive {

  private val logger = Logger(classOf[Git].getSimpleName)

  logger.info("Having repo: " + repo.getDirectory)
  logger.debug("Getting head of branch: " + branch)
  val reader = repo.newObjectReader()
  val df = new DiffFormatter(DisabledOutputStream.INSTANCE)
  df.setRepository(repo)
  df.setDiffComparator(RawTextComparator.DEFAULT)
  df.setDetectRenames(true)

  override def extract[A: Monoid](f: (Archive, ChangeDescription) => A): A = {
    val walk = new RevWalk(repo)
    val startCommitId = repo.resolve(branch.map("refs/heads/" + _).getOrElse(Constants.HEAD))
    val startCommit = walk.lookupCommit(startCommitId)
    walk.markStart(startCommit)
    walk.setRevFilter(RevFilter.NO_MERGES)
    val monoid = implicitly[Monoid[A]]
    fold(monoid.zero, walk.toSeq.reverseIterator, (acc: A, archive: Archive, changeDesc: ChangeDescription) =>
      monoid.append(acc, f(archive, changeDesc)))
  }

  @tailrec
  private def fold[A](
      acc: A, revWalk: Iterator[RevCommit],
      transform: (A, Archive, ChangeDescription) => A): A = {
    val oRevCommit = if (revWalk.hasNext) { Option(revWalk.next()) } else { None }
    oRevCommit match {
      case Some(revCommit) => {
        logger.debug("Processing commit: " + revCommit)
        val (added, changed, removed) = if (revCommit.getParentCount > 0) {
          val diffs = df.scan(revCommit.getParent(0).getTree, revCommit.getTree).toSeq
          (diffs.filter(_.getOldPath eq "/dev/null").map(_.getNewPath).toSet,
           diffs.filter(_.getOldPath ne "/dev/null").map(_.getNewPath).filter(_ ne "/dev/null").toSet,
           diffs.filter(_.getNewPath eq "/dev/null").map(_.getOldPath).toSet)
        } else {
          val tree = revCommit.getTree()
          val treeWalk = new TreeWalk(repo)
          treeWalk.addTree(tree)
          treeWalk.setRecursive(true)
          val files = mutable.Set[String]()
          while (treeWalk.next()) {
            files += treeWalk.getPathString
          }
          (files.toSet, Set[String](), Set[String]())
        }
        val changeDesc = ChangeDescription(
          reader.abbreviate(revCommit.getId).name,
          revCommit.getFullMessage,
          revCommit.getAuthorIdent.toExternalString,
          revCommit.getCommitterIdent.toExternalString,
          revCommit.getCommitTime,
          added,
          changed,
          removed)
        fold(transform(acc, new GitCommit(repo, revCommit), changeDesc), revWalk, transform)
      }
      case None => acc
    }
  }

  private val mutableObjectId = new MutableObjectId()

  override def getContent(objectId: String): String = {
    mutableObjectId.fromString(objectId)
    val loader = repo.open(mutableObjectId)
    new String(loader.getBytes)
  }

  override def save(fileName: String): Unit = {
    val writer = new FileWriter(fileName)
    writer
        .append(archiveTypeName)
        .append("\n")
        .append(repo.getDirectory.getAbsolutePath)
        .append("\n")
        .append(branch.getOrElse(""))
        .append("\n")
    writer.flush()
    writer.close()
  }

  override val archiveTypeName: String = classOf[GitArchive].getSimpleName
}

private[this] final class GitCommit(
    repo: Repository,
    commit: RevCommit)
  extends Archive {

  private val logger = Logger(classOf[GitCommit].getSimpleName)

  private val mutableObjectId = new MutableObjectId()
  
  override def extract[A: Monoid](f: (String, Option[String], () => InputStream) => A): A = {
    val tree = commit.getTree()
    logger.debug("Having tree: " + tree)
    val treeWalk = new TreeWalk(repo)
    treeWalk.addTree(tree)
    treeWalk.setRecursive(true)
    val monoid = implicitly[Monoid[A]]
    try {
      fold(monoid.zero, treeWalk, (acc: A, name: String, oObjectId: Option[String], content: () => InputStream) =>
        monoid.append(acc, f(name, oObjectId, content)))
    } finally {
      repo.close()
    }
  }

  @tailrec
  private def fold[A](acc: A, walk: TreeWalk, transform: (A, String, Option[String], () => InputStream) => A): A = {
    if (walk.next()) {
      val contentFn: () => InputStream = () => {
        walk.getObjectId(mutableObjectId, 0)
        val loader = repo.open(mutableObjectId)
        new ByteArrayInputStream(loader.getCachedBytes)
      }
      fold(
        transform(
          acc, walk.getPathString,
          Some(mutableObjectId.name),
          contentFn),
        walk, transform)
    } else {
      acc
    }
  }
}

object Git {

  private val logger = Logger(classOf[Git].getSimpleName)

  def apply(path: String, branch: Option[String] = None): VersionedArchive = new Git(
    createRepo(path), branch)

  private def createRepo(path: String): Repository = if (isRemote(path)) {
    cloneRemoteRepo(path)
  } else {
    getLocalRepo(if (path.endsWith(".git")) new File(path) else new File(path, ".git"))
  }

  def cloneRemoteRepo(url: String): Repository = {
    val localPath = File.createTempFile("TmpGitRepo", "")
    logger.info("Cloning from " + url + " to " + localPath)
    localPath.delete()
    org.eclipse.jgit.api.Git.cloneRepository()
        .setURI(url)
        .setDirectory(localPath)
        .call()
    getLocalRepo(new File(localPath, ".git"))
  }

  def getLocalRepo(gitFile: File): Repository = {
    new FileRepositoryBuilder()
        .setGitDir(gitFile)
        .readEnvironment() // scan environment GIT_* variables
        .findGitDir() // scan up the file system tree
        .build()
  }

  private def isRemote(path: String): Boolean = path.startsWith("git@") || path.startsWith("http")
}
