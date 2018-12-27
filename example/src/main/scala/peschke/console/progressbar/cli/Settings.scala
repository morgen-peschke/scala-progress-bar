package peschke.console.progressbar.cli

import scopt.OptionParser

case class Settings(initial: Int = 0, total: Int = 100, port: Int = 8091)

object Settings {
  implicit val optionParser: OptionParser[Settings] = new OptionParser[Settings]("progress-bar") {
   note("Starts a progress bar and listens for commands on the specified port.")

    opt[Int]('i', "initial")
      .text("Initial value for progress bar (defaults to 0)")
      .action((i, s) => s.copy(initial = i))

    opt[Int]('t', "total")
      .text("Maximum value for the progress bar (defaults to 100)")
      .action((t, s) => s.copy(total = t))

    opt[Int]('p', "port")
      .text("Port used to listen for updates (defaults to 8091)")
      .action((p, s) => s.copy(port = p))

    help("help")

    note {
      s"""|
          |Commands
          |========
          |
          |Any integer (positive or negative) will update the current count by that amount.
          |The string "close" closes the connection without closing the progress bar.
          |The string "quit" exits the progress bar.
          |The string "finish" completes the progress bar, then exits.""".stripMargin
    }
  }
}
