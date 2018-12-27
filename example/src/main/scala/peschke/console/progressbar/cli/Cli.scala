package peschke.console.progressbar.cli

import peschke.console.progressbar.ProgressBar

import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}
import scala.concurrent.duration.Duration

object Cli extends App {
  Settings.optionParser.parse(args, Settings()).foreach { settings =>
    implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global
    val progressBar = ProgressBar(
      initialCount = settings.initial,
      totalCount = settings.total
    )
    Await.result(Server.bind(settings.port, progressBar), Duration.Inf)
  }
}
