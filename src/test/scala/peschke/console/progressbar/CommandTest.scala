package peschke.console.progressbar

import java.util.concurrent.LinkedBlockingQueue

import peschke.UnitSpec
import peschke.console.progressbar.Command.{IncrementCount, IncrementTotal, Refresh, Terminate}

class CommandTest extends UnitSpec {

  private def queueOf(command: Command): LinkedBlockingQueue[Command] = queueOf(List(command))
  private def queueOf(command: Command, commands: Command*): LinkedBlockingQueue[Command] = queueOf(command +: commands)
  private def queueOf(commands: Seq[Command]): LinkedBlockingQueue[Command] = {
    val queue = new LinkedBlockingQueue[Command](commands.length)
    commands.foreach { cmd =>
      queue.add(cmd) mustBe true
    }
    queue
  }

  "Commands.takeFrom" when {
    "the queue contains a single element" should {
      Seq(
        Terminate,
        Refresh,
        IncrementCount(),
        IncrementTotal()).foreach { command =>
        s"return $command immediately" in {
          Command.takeFrom(queueOf(command)) mustBe List(command)
        }
      }
    }

    "the queue contains multiple elements" should {
      val atomicCommands = List(Refresh, Terminate)
      val collapsibleCommands = List(IncrementCount(), IncrementTotal())

      atomicCommands.foreach { command =>
        s"return $command immediately" in {
          Command.takeFrom(queueOf(command, IncrementCount())) mustBe List(command)
        }
      }

      collapsibleCommands.foreach { collapsibleCommand =>
        atomicCommands.foreach { atomicCommand =>
          s"return $collapsibleCommand and subsequent elements until the first $atomicCommand" in {
            val expected = collapsibleCommand :: collapsibleCommands ::: List(atomicCommand)
            val input = expected ::: collapsibleCommands
            Command.takeFrom(queueOf(input)) mustBe expected
          }
        }

        s"return $collapsibleCommand and the rest of the queue, if the queue does not contain an atomic command" in {
          val expected = collapsibleCommand :: collapsibleCommands
          Command.takeFrom(queueOf(expected)) mustBe expected
        }
      }
    }
  }
}
