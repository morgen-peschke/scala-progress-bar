package peschke.console.progressbar

import java.io.{ByteArrayOutputStream, PrintStream}

import org.scalatest.concurrent.ScalaFutures
import peschke.{Complete, UnitSpec}
import peschke.console.progressbar.Command.{IncrementCount, IncrementTotal, Refresh, Terminate}

import scala.concurrent.ExecutionContext

class ProgressBarTest extends UnitSpec with ScalaFutures {
  private implicit val ec: ExecutionContext =  scala.concurrent.ExecutionContext.global

  private def createBuffer: ByteArrayOutputStream = new ByteArrayOutputStream()
  private def writeTo(byteArrayOutputStream: ByteArrayOutputStream): PrintStream = new PrintStream(byteArrayOutputStream)
  private val ignoreOutput: PrintStream = writeTo(createBuffer)

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

  "ProgressBar.redraw" should {
    "return a future which completes after the next update" in {
      val buffer = createBuffer
      val progressBar = new ProgressBar(
        ProgressBarState(count = 0, total = 5, width = 20),
        commandBufferSize = 10,
        output = writeTo(buffer))

      progressBar.setCount(2)
      val redrawFuture = progressBar.redraw()
      redrawFuture.futureValue mustBe Complete

      buffer.toString mustBe "\r2 / 5 [=>  ]  40.00%"
    }
  }

  "ProgressBar.terminate" should {
    "return a future which completes after the next update" in {
      val buffer = createBuffer
      val progressBar = new ProgressBar(
        ProgressBarState(count = 0, total = 5, width = 20),
        commandBufferSize = 10,
        output = writeTo(buffer))

      progressBar.setCount(2)
      val redrawFuture = progressBar.terminate()
      redrawFuture.futureValue mustBe Complete

      buffer.toString mustBe "\r2 / 5 [=>  ]  40.00%\n"
    }
  }

  "ProgressBar.complete" should {
    "return a future which completes after the next update" in {
      val buffer = createBuffer
      val progressBar = new ProgressBar(
        ProgressBarState(count = 0, total = 5, width = 20),
        commandBufferSize = 10,
        output = writeTo(buffer))

      progressBar.setCount(2)
      val redrawFuture = progressBar.complete()
      redrawFuture.futureValue mustBe Complete

      buffer.toString mustBe "\r5 / 5 [====] 100.00%\n"
    }
  }
}
