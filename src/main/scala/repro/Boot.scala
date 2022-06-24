package repro

import cats.effect._
import cats.effect.std.Console
import cats.syntax.all._
import cats.effect.syntax.all._
import fs2._
import fs2.io.{readOutputStream, writeOutputStream}

object Boot extends IOApp.Simple {
  private def stream[F[_] : Console : Async](size: Long): Stream[F, Nothing] =
    Stream
      .emit(Byte.MinValue)
      .repeat
      .chunkN(4.megabytes, allowFewer = false)
      .zipWithIndex
      .evalMap { case (c, idx) =>
        Console[F].println(s"👀 processing chunk #$idx of ${c.size} bytes")
          .as(c)
      }
      .flatMap(Stream.chunk)

      /*
       * Source stream chunk size | readOutputStream Chunk Size | Take at least…
       * -------------------------|-----------------------------|--------------
       *             1            |               1             |       4
       *             1            |               2             |       5
       *             1            |               3             |       7
       *            4MB           |              1MB            |      3MB
       */

      .take(3.megabytes)
      .through { in =>
        // This is a simplification of what fs2-pgp does to pipe the stream through BouncyCastle's various OutputStreams
        readOutputStream(1.megabyte) { out =>
          in
            .through(writeOutputStream(out.pure[F], closeAfterUse = true))
            .compile
            .drain
            .guarantee(Console[F].println("✍️ writeOutputStream complete"))
        }
      }
      .through {
        _.drop(1) >> // without this drop (or maybe just something that reads from the input stream), the hang condition doesn't seem to be triggered
          Stream.raiseError(new RuntimeException("boom"))
      } ++
      Stream.eval(Console[F].println("😇 completed stream")).drain

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
