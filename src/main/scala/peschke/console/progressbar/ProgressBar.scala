package peschke.console.progressbar

import java.io.PrintStream
import java.util.concurrent.LinkedBlockingQueue

import peschke.console.progressbar.Command.{IncrementCount, IncrementTotal, Refresh, Terminate}

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

/**
 * Create, display, control, and terminate a console progress bar.
 *
 * @param initialCount Initial counter value, must be less than or equal to [[totalCount]]
 * @param totalCount Maximum count value, must be greater than or equal to [[initialCount]]
 * @param width Display width of the bar, manually set to avoid depending on JLine
 * @param commandBufferSize Maximum of changes to the bar (increments, etc) per display
 * @param output This is a [[java.io.PrintStream]] to match [[java.lang.System.out]]
 * @param ec Execution context where the worker thread will run
 */
class ProgressBar(initialCount: Long = 0,
                  totalCount: Long = 100,
                  width: Int = 80,
                  commandBufferSize: Int = 50,
                  output: PrintStream = System.out)(implicit ec: ExecutionContext) {

  private val commandQueue = new LinkedBlockingQueue[Command](commandBufferSize)
  private val future = Future {
    @tailrec
    def loop(state: ProgressBarState): ProgressBarState = {
      state.draw(output)
      if (state.isTerminated) {
        output.println()
        state
      }
      else {
        val nextState =
          Command.takeFrom(commandQueue).foldLeft(state) {
            case (prevState, _) if prevState.isTerminated => prevState
            case (prevState, Terminate) => prevState.terminated
            case (prevState, Refresh) => prevState
            case (prevState, IncrementCount(delta)) => prevState.incrementCount(delta)
            case (prevState, IncrementTotal(delta)) => prevState.incrementTotal(delta)
          }
        loop(nextState)
      }
    }
    loop(ProgressBarState(initialCount, totalCount, width))
  }

  private def maybeThrowExceptionFromWorker(): Unit = {
    future.value match {
      case Some(Failure(ex)) => throw ex
      case _ => ()
    }
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
   * Increment (or decrement) the total value of the bar
   *
   * This won't modify the count, but will implicitly modify the percent completion.
   *
   * @param delta can be positive or negative
   */
  def incrementTotal(delta: Long = 1): Unit = {
    maybeThrowExceptionFromWorker()
    commandQueue.put(IncrementTotal(delta))
  }

  /**
   * Force a redraw
   */
  def redraw(): Unit = {
    maybeThrowExceptionFromWorker()
    commandQueue.put(Refresh)
    Thread.`yield`()
  }

  /**
   * Terminate the progress bar.
   *
   * Sets the bar value to the total, prints a final bar update, and drops a newline so printing to the output stream
   * can continue without issue.
   */
  def terminate(): Unit = {
    maybeThrowExceptionFromWorker()
    commandQueue.put(Terminate)
    Thread.`yield`()
  }
}
