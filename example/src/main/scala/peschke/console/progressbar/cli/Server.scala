package peschke.console.progressbar.cli

import java.io._
import java.net.ServerSocket

import peschke.Complete
import peschke.console.progressbar.ProgressBar

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object Server {
  sealed abstract class SocketResult
  case object SocketClosed extends SocketResult
  case object TerminationInitiated extends SocketResult

  def serve(input: BufferedReader, output: PrintWriter, progressBar: ProgressBar)(implicit EC: ExecutionContext): Future[SocketResult] = {
    def loop(): Future[SocketResult] =
      Future(Option(input.readLine()))
        .flatMap {
          case None         => Future.successful(SocketClosed)
          case Some("close") => Future.successful(SocketClosed)
          case Some("quit") => progressBar.terminate().map(_ => TerminationInitiated)
          case Some("finish") => progressBar.complete().map(_ => TerminationInitiated)
          case Some(rawValue) =>
            Future {
              val delta = rawValue.toLong
              progressBar.incrementCount(delta)
              delta
            }.transformWith {
              case Success(delta) =>
                Future {
                  output.println(s"Incremented by $delta")
                  output.flush()
                }.flatMap(_ => loop())
              case Failure(_: NumberFormatException) =>
                Future {
                  output.println("Bad input: expected 'close', 'quit', 'finish', or an integer")
                  output.flush()
                }.flatMap(_ => loop())
              case Failure(_) =>
                Future.successful(SocketClosed)
            }
        }
    loop()
  }

  def bind(port: Int, progressBar: ProgressBar)(implicit EC: ExecutionContext): Future[Complete] = {
    val acceptor = new ServerSocket(port)

    def listen(): Future[Complete] = {
      val socket = acceptor.accept
      val input = new BufferedReader(new InputStreamReader(socket.getInputStream))
      val output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream))
      serve(input, output, progressBar)
        .transformWith { result =>
          input.close()
          output.flush()
          output.close()
          socket.close()
          result match {
            case Failure(ex)                   => Future.failed(ex)
            case Success(SocketClosed)         => listen()
            case Success(TerminationInitiated) => Future.successful(Complete)
          }
        }
    }

    listen().transformWith { result =>
      Future(acceptor.close()).flatMap(_ => Future.fromTry(result))
    }
  }
}