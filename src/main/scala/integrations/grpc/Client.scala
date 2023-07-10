package integrations.grpc

import cats.implicits._
import cats.effect.{ExitCode, IO, IOApp, Resource}
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import fs2.grpc.syntax.all._
import io.grpc.ManagedChannel
import grpc.model.hello._
import io.grpc._
import protomodels.person.Person.PhoneNumber
import protomodels.person.{Person, PersonAddressServiceFs2Grpc}

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by Ilya Volynin on 02.12.2020 at 12:15.
 */
object Client extends IOApp {
  val address = "127.0.0.1:1234"
  val managedChannelResource: Resource[IO, ManagedChannel] =
    NettyChannelBuilder
      .forTarget(address)
      .defaultLoadBalancingPolicy("round_robin")
      .usePlaintext()
      .resource[IO]

  def runProgram(helloStub: GreeterFs2Grpc[IO, Metadata], addrBookServ: PersonAddressServiceFs2Grpc[IO, Metadata]): IO[Unit] = {
    for {
      response <- helloStub.sayHello(HelloRequest("Ilyusha V"), new Metadata())
      _ <- IO(println(response.greeting))
      _ <- helloStub
        .sayHelloStream(
          fs2.Stream
            .iterate(0)(_ + 1)
            .map(a => HelloRequest("blah", a))
            .take(10),
          new Metadata()
        )
        .map(r => println(r.greeting))
        .compile
        .drain
      addResp <- addrBookServ.saveAddress(Person("me", 1, Some("1@1.com"), Seq(PhoneNumber(number = "7777777777"))), new Metadata())
      _ <- IO(println(addResp.code + " " + addResp.message))
    } yield ()
  }

  def run(args: scala.List[String]): cats.effect.IO[ExitCode] =
    managedChannelResource.use { mcr =>
      val rs = for {
        greeter <- GreeterFs2Grpc.stubResource[IO](mcr)
        addrBookServ <- PersonAddressServiceFs2Grpc.stubResource[IO](mcr)
      } yield (greeter, addrBookServ)

      rs.use(r => runProgram(r._1, r._2))
    }.map(_ => ExitCode.Success)

}
