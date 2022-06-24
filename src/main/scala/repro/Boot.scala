package repro

import cats.effect._
import cats.effect.std.Console
import cats.syntax.all._
import fs2._
import fs2.io.{readOutputStream, writeOutputStream}

object Boot extends IOApp.Simple {
  private def stream[F[_] : Console : Async](size: Long): Stream[F, Nothing] =
    Stream
      .repeatEval(Array.fill(10.megabytes)(Byte.MinValue).pure[F])
      .map(Chunk.array(_))
      .zipWithIndex
      .evalMap { case (c, idx) =>
        Console[F].println(s"👀 processing chunk #$idx of ${c.size} bytes")
          .as(c)
      }
      .flatMap(Stream.chunk)
      .take(size)
      .through { in =>
        // This is a simplification of what fs2-pgp does to pipe the stream through BouncyCastle's various OutputStreams
        readOutputStream(1.megabyte) { out =>
          in
            .through(writeOutputStream(out.pure[F], closeAfterUse = true))
            .compile
            .drain
            .flatTap(_ => Console[F].println("✍️ writeOutputStream complete"))
        }
      }
      .through {
        _.drop(1) >>
          Stream.raiseError(new RuntimeException("boom"))
      }
      .drain ++ Stream.eval(Console[F].println("😇 completed stream")).drain

  private def runF[F[_] : Async : Console]: Stream[F, Unit] =
    stream(1.gigabyte) ++ Stream.emit(())

  override def run: IO[Unit] =
    runF[IO].compile.drain

  implicit class MBOps(val i: Int) extends AnyVal {
    def megabyte: Int = i * 1024 * 1024
    def megabytes: Int = i * 1024 * 1024
    def gigabyte: Long = i.megabytes * 1024L
    def gigabytes: Long = i.megabytes * 1024L
  }
}
