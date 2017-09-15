package peschke.console.progressbar

import java.io.{ByteArrayOutputStream, PrintStream}

import org.scalatest.matchers.{MatchResult, Matcher}
import peschke.UnitSpec

class ProgressBarStateTest extends UnitSpec {
  "ProgressBarState.incrementTotal" should {
    "respect the lower bound of the current value of count" in {
      val initial = ProgressBarState(4, 5)
      initial.incrementTotal(1) mustBe ProgressBarState(4, 6)
      initial.incrementTotal(0) mustBe ProgressBarState(4, 5)
      initial.incrementTotal(-1) mustBe ProgressBarState(4, 4)
      initial.incrementTotal(-2) mustBe ProgressBarState(4, 4)
    }
  }

  "ProgressBarState.incrementCount" should {
    "respect the lower bound of 0" in {
      val initial = ProgressBarState(2, 5)
      initial.incrementCount(1) mustBe ProgressBarState(3, 5)
      initial.incrementCount(0) mustBe ProgressBarState(2, 5)
      initial.incrementCount(-1) mustBe ProgressBarState(1, 5)
      initial.incrementCount(-2) mustBe ProgressBarState(0, 5)
      initial.incrementCount(-3) mustBe ProgressBarState(0, 5)
    }

    "respect the upper bound of the current value of total" in {
      val initial = ProgressBarState(4, 5)
      initial.incrementCount(1) mustBe ProgressBarState(5, 5)
      initial.incrementCount(2) mustBe ProgressBarState(5, 5)
    }
  }

  "ProgressBarState.terminated" should {
    "finish the progress bar without changing the count" in {
      ProgressBarState(2, 4).terminated mustBe ProgressBarState(2, 4, isFinished = true)
    }
  }

  "ProgressBarState.completed" should {
    "finish the progress bar with setting the count to total" in {
      ProgressBarState(2, 4).completed mustBe ProgressBarState(4, 4, isFinished = true)
    }
  }

  "ProgressBarState.draw" should {
    def draw(expected: String): Matcher[ProgressBarState] = new Matcher[ProgressBarState] {
      private val underlyingMatcher = be(expected)
      override def apply(pbs: ProgressBarState): MatchResult = {
        val underlyingOutputStream = new ByteArrayOutputStream()
        val output = new PrintStream(underlyingOutputStream)
        pbs.draw(output)
        output.close()
        underlyingOutputStream.close()
        underlyingMatcher.apply(underlyingOutputStream.toString)
      }
    }

    "produce the expected bar when empty" in {
      ProgressBarState(0, 80) must draw {
        "\r 0 / 80 [                                                              ]   0.00%"
      }
    }

    "produce the expected bar when not terminated, and the count is full" in {
      ProgressBarState(80, 80) must draw {
        "\r80 / 80 [==============================================================] 100.00%"
      }
    }

    "produce the expected bar when terminated with a partial count" in {
      ProgressBarState(40, 80, isFinished = true) must draw {
        "\r40 / 80 [==============================|                               ]  50.00%"
      }
    }

    "produce the expected bar when terminated with a full count" in {
      ProgressBarState(80, 80, isFinished = true) must draw {
        "\r80 / 80 [==============================================================] 100.00%"
      }
    }

    Seq(
      ProgressBarState(0, 5, 20) -> "\r0 / 5 [    ]   0.00%",
      ProgressBarState(1, 5, 20) -> "\r1 / 5 [>   ]  20.00%",
      ProgressBarState(2, 5, 20) -> "\r2 / 5 [=>  ]  40.00%",
      ProgressBarState(3, 5, 20) -> "\r3 / 5 [==> ]  60.00%",
      ProgressBarState(4, 5, 20) -> "\r4 / 5 [===>]  80.00%",
      ProgressBarState(5, 5, 20) -> "\r5 / 5 [====] 100.00%",
      ProgressBarState(0, 5, 20, isFinished = true) -> "\r0 / 5 [    ]   0.00%",
      ProgressBarState(1, 5, 20, isFinished = true) -> "\r1 / 5 [|   ]  20.00%",
      ProgressBarState(2, 5, 20, isFinished = true) -> "\r2 / 5 [=|  ]  40.00%",
      ProgressBarState(3, 5, 20, isFinished = true) -> "\r3 / 5 [==| ]  60.00%",
      ProgressBarState(4, 5, 20, isFinished = true) -> "\r4 / 5 [===|]  80.00%",
      ProgressBarState(5, 5, 20, isFinished = true) -> "\r5 / 5 [====] 100.00%"
    ).foreach {
      case (state, bar) =>
        s"produce the expected bar for $state" in {
          state must draw (bar)
        }
    }
  }
}
