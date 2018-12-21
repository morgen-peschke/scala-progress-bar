package peschke.console.progressbar

import java.io.PrintStream
import java.util.concurrent.{ConcurrentHashMap, LinkedBlockingQueue}

import peschke.{Complete => CompletedUpdate}
import peschke.console.progressbar.Command._

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.collection.JavaConverters._
import scala.util.Failure

/**
 * Create, display, control, and terminate a console progress bar.
 *
 * @param initialState Initial state of the progress bar
 * @param commandBufferSize Maximum of changes to the bar (increments, etc) per display
 * @param output This is a [[java.io.PrintStream]] to match [[java.lang.System.out]]
 * @param ec Execution context where the worker thread will run
 */
class ProgressBar(initialState: ProgressBarState, commandBufferSize: Int, output: PrintStream)
                 (implicit ec: ExecutionContext) {

  private [progressbar] val commandQueue: LinkedBlockingQueue[Command] = new LinkedBlockingQueue[Command](commandBufferSize)
  private [progressbar] val updateObservers = ConcurrentHashMap.newKeySet[Promise[CompletedUpdate]]()
  private [progressbar] val future = Future {
    @tailrec
    def loop(state: ProgressBarState): ProgressBarState = {
      val willNotify = updateObservers.iterator.asScala.toList
      updateObservers.removeAll(willNotify.asJava)

      state.draw(output)

      if (state.isFinished) {
        output.println()
        willNotify.foreach(_.trySuccess(CompletedUpdate))
        // We want to make sure that, if we're finished, we notify everyone.
        updateObservers.iterator.asScala.foreach(_.trySuccess(CompletedUpdate))
        state
      }
      else {
        val nextState =
          Command.takeFrom(commandQueue).foldLeft(state) {
            case (prevState, _) if prevState.isFinished => prevState
            case (prevState, Terminate)                 => prevState.terminated
            case (prevState, Complete)                  => prevState.completed
            case (prevState, Refresh)                   => prevState
            case (prevState, IncrementCount(delta))     => prevState.incrementCount(delta)
            case (prevState, IncrementTotal(delta))     => prevState.incrementTotal(delta)
          }
        // Any added during the update can be notified next time around.
        willNotify.foreach(_.trySuccess(peschke.Complete))
        loop(nextState)
      }
    }
    loop(initialState)
  }

  private def maybeThrowExceptionFromWorker(): Unit = {
    future.value match {
      case Some(Failure(ex)) => throw ex
      case _ => ()
    }
  }

  private def queueCommandWithCompletion(command: Command): Future[CompletedUpdate] = {
    val completionPromise = Promise[CompletedUpdate]()
    updateObservers.add(completionPromise)
    commandQueue.put(command)
    completionPromise.future
  }

  /**
   * Increment (or decrement) the progress of the bar
   * @param delta can be positive or negative
   */
  def incrementCount(delta: Long = 1): Unit = {
    maybeThrowExceptionFromWorker()
    commandQueue.put(IncrementCount(delta))
  }

  /**
   * Set the progress of the bar
   * @param count must be between 0 and the total value of the bar
   */
  def setCount(count: Long): Unit = {
    maybeThrowExceptionFromWorker()
    commandQueue.put(SetCount(count))
  }

  /**
   * Increment (or decrement) the total value of the bar
   *
   * This won't modify the count, but will modify the percent completion.
   *
   * @param delta can be positive or negative
   */
  def incrementTotal(delta: Long = 1): Unit = {
    maybeThrowExceptionFromWorker()
    commandQueue.put(IncrementTotal(delta))
  }

  /**
   * Set the total value of the bar
   *
   * This won't modify the count, but will modify the percent completion.
   *
   * @param total must be greater than or equal to the current count
   */
  def setTotal(total: Long): Unit = {
    maybeThrowExceptionFromWorker()
    commandQueue.put(SetTotal(total))
  }

  /**
   * Force a redraw
   */
  def redraw(): Future[CompletedUpdate] = {
    maybeThrowExceptionFromWorker()
    queueCommandWithCompletion(Refresh)
  }

  /**
   * Terminate the progress bar.
   *
   * Prints a final bar update, and drops a newline so printing to the output stream can continue without issue.
   */
  def terminate(): Future[CompletedUpdate] = {
    maybeThrowExceptionFromWorker()
    queueCommandWithCompletion(Terminate)
  }

  /**
   * Complete the progress bar.
   *
   * Sets the bar value to the total, prints a final bar update, and drops a newline so printing to the output stream
   * can continue without issue.
   */
  def complete(): Future[CompletedUpdate] = {
    maybeThrowExceptionFromWorker()
    queueCommandWithCompletion(Complete)
  }
}

object ProgressBar {

  /**
   * Create a new [[ProgressBar]]
   *
   * @param initialCount Initial counter value, must be less than or equal to totalCount
   * @param totalCount Maximum count value, must be greater than or equal to initialCount
   * @param width Display width of the bar, manually set to avoid depending on JLine
   * @param commandBufferSize Maximum of changes to the bar (increments, etc) per display
   * @param output This is a [[java.io.PrintStream]] to match [[java.lang.System.out]]
   * @param ec Execution context where the worker thread will run
   */
  def apply(initialCount: Long = 0L,
            totalCount: Long = 100L,
            width: Int = 80,
            commandBufferSize: Int = 50,
            output: PrintStream = System.out)
           (implicit ec: ExecutionContext): ProgressBar = {
    new ProgressBar(
      ProgressBarState(count = initialCount, total = totalCount, width = width),
      commandBufferSize,
      output)
  }
}