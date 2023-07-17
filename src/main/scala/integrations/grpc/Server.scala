package integrations.grpc

/**
 * Created by Ilya Volynin on 02.12.2020 at 9:12.
 */

import cats.effect.{Deferred, ExitCode, IO, IOApp, Resource}
import com.typesafe.config.{Config, ConfigFactory}
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
      .evalMap(server =>
        for {
          s <- IO(server.start())
          _ <- IO(logger.warn("server is running..."))
          d <- Deferred[IO, cats.effect.ExitCode]
        } yield (s,d)
      )
      .useForever

  def run(args: scala.List[String]): cats.effect.IO[cats.effect.ExitCode] = {
    val config: Config = ConfigFactory.load()
    val port = config.getString("port").toInt
    val rs = for {
          addr <- addressService()
          hello <- helloService(port)
        } yield (addr, hello)
        rs.use(s => runS(port, s._1, s._2))
    }

}
