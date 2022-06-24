package repro

import blobstore.s3.S3Store
import blobstore.url.Url
import blobstore.url.exception.Throwables
import cats.data.OptionT
import cats.effect._
import cats.effect.std._
import cats.effect.syntax.all._
import cats.syntax.all._
import fs2._
import fs2.io.{readOutputStream, writeOutputStream}
import org.typelevel.log4cats.{Logger, LoggerFactory}
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.core.retry.RetryPolicy
import software.amazon.awssdk.services.s3.S3AsyncClient

object Boot extends IOApp {
  private val config =
    ClientOverrideConfiguration
      .builder()
      .retryPolicy(RetryPolicy.builder().numRetries(0).build()) // not retrying makes it more likely to hang
      .build()

  private def resource[F[_] : Async : LoggerFactory](s3Path: String): Resource[F, Pipe[F, Byte, Unit]] =
    for {
      s3Client <- Resource.fromAutoCloseable(Sync[F].blocking(S3AsyncClient.builder().overrideConfiguration(config).build()))
      s3 <- S3Store.builder[F](s3Client).build.leftMap(_.reduce(Throwables.collapsingSemigroup)).liftTo[F].toResource
      url <- Url.parseF(s3Path).toResource
      s3Pipe = s3.put(url)(_: Stream[F, Byte]).handleErrorWith { t =>
        Stream.eval(LoggerFactory[F].create.flatMap(_.error(t)("‼️ an error occurred during s3.put")) >> t.raiseError)
      }
    } yield s3Pipe

  private def stream[F[_] : Logger : Async](size: Long)
                                           (s3: Pipe[F, Byte, Unit]): Stream[F, Nothing] =
    Stream
      .repeatEval(Array.fill(10.megabytes)(Byte.MinValue).pure[F])
      .map(Chunk.array(_))
      .zipWithIndex
      .evalMap { case (c, idx) =>
        Logger[F].info(s"👀 processing chunk #$idx of ${c.size} bytes")
          .as(c)
      }
      .flatMap(Stream.chunk)
      .take(size)
      .through { in =>
        // This is a simplification of what fs2-pgp does to pipe the stream through BouncyCastle's various OutputStreams
        readOutputStream(10.megabytes) { out =>
          in
            .through(writeOutputStream(out.pure[F], closeAfterUse = true))
            .compile
            .drain
            .flatTap(_ => Logger[F].info("✍️ writeOutputStream complete"))
        }
      }
//      .through { _ =>
//        // notably, just raising an error here using Stream.raiseError does not seem to cause a hang
//        Stream.raiseError(new RuntimeException("boom"))
//      }
      .through(s3)
      .drain ++ Stream.eval(Logger[F].info("😇 completed upload")).drain

  private def runF[F[_] : Async : LoggerFactory](s3Path: String): Stream[F, Unit] =
    for {
      s3 <- Stream.resource(resource[F](s3Path))
      implicit0(logger: Logger[F]) <- Stream.eval(LoggerFactory[F].create)
      _ <- stream(1.gigabyte)(s3) ++ Stream.emit(())
    } yield ()

  override def run(args: List[String]): IO[ExitCode] =
    OptionT
      .fromOption[IO](args.headOption)
      // a 404 from S3 seems to trigger the problem right away, so feel free not to specify a working S3 path
      .getOrElseF {
        UUIDGen[IO]
          .randomUUID
          .map(uuid => s"s3://${uuid.toString}/random.gpg")
          .flatTap { s3Path =>
            Console[IO].errorln(s"🙋 using a randomly generated S3 path ($s3Path), so expect a 404 from S3. This should still trigger the hang.")
          }
      }
      .flatMap {
        import org.typelevel.log4cats.slf4j._

        runF[IO](_).compile.drain.as(ExitCode.Success)
      }

  implicit class MBOps(val i: Int) extends AnyVal {
    def megabyte: Int = i * 1024 * 1024
    def megabytes: Int = i * 1024 * 1024
    def gigabyte: Long = i.megabytes * 1024L
    def gigabytes: Long = i.megabytes * 1024L
  }
}
