package peschke.console.progressbar

import java.util.concurrent.BlockingQueue

import peschke.collections.TakeUntil.syntax._

/**
 * Enum values encoding queued operations on a progress bar.
 */
sealed trait Command
object Command extends {
  case object Terminate extends Command
  case object Refresh extends Command
  case class IncrementCount(delta: Long = 1L) extends Command
  case class IncrementTotal(delta: Long = 1L) extends Command

  /**
   * Pull [[peschke.console.progressbar.Command]] from a [[java.util.concurrent.BlockingQueue]].
   *
   * Stopping criteria is the queue is empty, or an command is reached which requires immediate action.
   *
   * This method blocks on the first element, but only pulls subsequent elements which can be retrieved without
   * blocking further.
   */
  def takeFrom(queue: BlockingQueue[Command]): List[Command] =
    queue.take match {
      case command @ (Refresh | Terminate) => List(command)
      case command =>
        command :: Stream
          .continually(queue.poll())
          .map(Option(_))
          .takeUntil {
            case None | Some(Terminate | Refresh) => true
            case Some(_) => false
          }
          .toList
          .flatten
    }
}
