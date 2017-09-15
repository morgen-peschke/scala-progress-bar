package peschke.console.progressbar

import java.io.{ByteArrayOutputStream, PrintStream}

import org.scalatest.concurrent.ScalaFutures
import peschke.UnitSpec
import peschke.console.progressbar.Command.{IncrementCount, IncrementTotal, Refresh, Terminate}

import scala.concurrent.ExecutionContext

class ProgressBarTest extends UnitSpec with ScalaFutures {
  private implicit val ec: ExecutionContext =  scala.concurrent.ExecutionContext.global

  private val ignoreOutput: PrintStream = new PrintStream(new ByteArrayOutputStream())

  Seq[(String, ProgressBar => Unit, Command)](
    ("incrementCount(3)", _.incrementCount(3), IncrementCount(3)),
    ("incrementTotal(6)", _.incrementTotal(6), IncrementTotal(6)),
    ("redraw()", _.redraw(), Refresh),
    ("terminate()", _.terminate(), Terminate)
  ).foreach {
    case (methodName, invocation, expected) =>
      s"ProgressBar.$methodName" should {
        "throw exceptions caused by the worker" in {
          val progressBar = new ProgressBar(
            ProgressBarState(count = 0, total = 1),
            commandBufferSize = 1,
            output = null)

          progressBar.future.failed.futureValue mustBe a[NullPointerException]

          intercept[NullPointerException] {
            invocation(progressBar)
          }
        }

        s"queue up $expected" in {
          val progressBar = new ProgressBar(
            // isTerminated is set to true to prevent the background worker from
            // emptying the queue before it can be checked.
            ProgressBarState(count = 0, total = 10, isFinished = true),
            commandBufferSize = 2,
            output = ignoreOutput)

          invocation(progressBar)
          progressBar.commandQueue.element mustBe expected

          progressBar.terminate()
        }
      }
  }
}
