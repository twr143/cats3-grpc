

ThisBuild / useCoursier := false
organization := "com.iv"
scalaVersion := "2.12.17"
version := "0.1.0-SNAPSHOT"
name := "cats3-grpc"

//scalacOptions ++= List(
//  "-Yrangepos", // required by SemanticDB compiler plugin
//  "-deprecation",
//  "-unchecked",
//  "-language:implicitConversions",
//  "-language:higherKinds",
//  "-language:existentials",
//  "-language:postfixOps",
//  "-Wunused"
//    "-Xsource:3",
//    "-P:kind-projector:underscore-placeholders"
//)
val scalaCacheVersion = "0.28.0"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4"
javacOptions ++= Seq("-encoding", "UTF-8")
enablePlugins(Fs2Grpc)
scalapbCodeGeneratorOptions += CodeGeneratorOption.Fs2Grpc
libraryDependencies += "io.grpc" % "grpc-netty-shaded" % scalapb.compiler.Version.grpcJavaVersion
