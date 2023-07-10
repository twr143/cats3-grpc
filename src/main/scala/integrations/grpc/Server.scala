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
import protomodels.person.{Person, PersonAddressServiceFs2Grpc, PersonResponse}

object Server extends IOApp with StrictLogging {
  class ExampleImplementation(port: Int) extends GreeterFs2Grpc[IO, Metadata] {
    override def sayHello(request: HelloRequest, clientHeaders: Metadata): IO[HelloResponse] = {
      IO(HelloResponse(s"Serial is: ${request.serial}, port: $port, ${request.name}!"))
    }

    override def sayHelloStream(request: Stream[IO, HelloRequest], clientHeaders: Metadata): Stream[IO, HelloResponse] = {
      request.evalMap(req => sayHello(req, clientHeaders))
    }
  }

  class AddressBookServiceImpl extends PersonAddressServiceFs2Grpc[IO, Metadata] {
    override def saveAddress(request: Person, ctx: Metadata): IO[PersonResponse] = IO {
      PersonResponse(code = 0, message = request.name)
    }
  }

  def helloService(port: Int): Resource[IO, ServerServiceDefinition] =
    GreeterFs2Grpc.bindServiceResource(new ExampleImplementation(port))

  def addressService(): Resource[IO, ServerServiceDefinition] =
    PersonAddressServiceFs2Grpc.bindServiceResource(new AddressBookServiceImpl())

  def runS(port: Int, helloService: ServerServiceDefinition, addressService: ServerServiceDefinition) =
    NettyServerBuilder
      .forPort(port)
      .addService(helloService)
      .addService(addressService)
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
        val rs = for {
          addr <- addressService()
          hello <- helloService(args.head.toInt)
        } yield (addr, hello)
        rs.use(s => runS(args.head.toInt, s._1, s._2))
      case _ =>
        IO {
          logger.warn("Please provide a port as an argument. Exiting")
          ExitCode.Success
        }
    }
  }
}
