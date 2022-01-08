package integrations.grpc

/**
  * Created by Ilya Volynin on 02.12.2020 at 9:12.
  */
import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.typesafe.scalalogging.StrictLogging
import fs2.Stream
import fs2.grpc.syntax.all._
import io.grpc._
import grpc.model.hello._
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object Server extends IOApp with StrictLogging {
  class ExampleImplementation(port: Int) extends GreeterFs2Grpc[IO, Metadata] {
    override def sayHello(request: HelloRequest, clientHeaders: Metadata): IO[HelloResponse] = {
      IO(HelloResponse(s"Serial is: ${request.serial}, port: $port, ${request.name}!"))
    }

    override def sayHelloStream(request: Stream[IO, HelloRequest], clientHeaders: Metadata): Stream[IO, HelloResponse] = {
      request.evalMap(req => sayHello(req, clientHeaders))
    }
  }
  def helloService(port: Int)  =
     GreeterFs2Grpc.bindServiceResource(new ExampleImplementation(port))

  def runS(port: Int, service: ServerServiceDefinition)=
    NettyServerBuilder
      .forPort(port)
      .addService(service)
      .resource[IO]
      .evalMap(server => IO(server.start()))
      .use(_ => IO {
                    logger.warn(s"${System.getProperty("os.name")} Press Ctrl+Z to exit...")
                    while (System.in.read() != -1) {}
                    logger.warn("Received end-of-file on stdin. Exiting")
                    ExitCode.Success
      })

  def run(args: scala.List[String]): cats.effect.IO[cats.effect.ExitCode] = {
    args.size match {
      case 1 =>
        helloService(args(0).toInt).use(s => runS(args(0).toInt, s))
      case _ =>
        IO {
          logger.warn("Please provide a port as an argument. Exiting")
          ExitCode.Success
        }
    }
  }
}
