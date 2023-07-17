

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
Global / onChangedBuildSource := ReloadOnSourceChanges
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4"
libraryDependencies += "com.typesafe" % "config" % "1.4.2"
javacOptions ++= Seq("-encoding", "UTF-8")
enablePlugins(DockerPlugin)
enablePlugins(Fs2Grpc)
scalapbCodeGeneratorOptions += CodeGeneratorOption.Fs2Grpc
libraryDependencies += "io.grpc" % "grpc-netty-shaded" % scalapb.compiler.Version.grpcJavaVersion
Compile / mainClass := Some("integrations.grpc.Server")
docker / dockerfile := {
  val jarFile: File = (Compile / packageBin / sbt.Keys.`package`).value
  val classpath = (Compile / managedClasspath).value
  val mainclass = (Compile / packageBin / mainClass).value.getOrElse(sys.error("Expected exactly one main class"))
  val jarTarget = s"/app/${jarFile.getName}"
  // Make a colon separated classpath with the JAR file
  val classpathString = classpath.files.map("/app/" + _.getName)
    .mkString(":") + ":" + jarTarget
  val vmArgs = "-XX:+UnlockExperimentalVMOptions -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -XX:+UseContainerSupport -XX:MinRAMPercentage=50.0 -XX:MaxRAMPercentage=95.0".split(" ")
  new Dockerfile {
    // Base image
    from("openjdk:8-jre-slim")
    // Add all files on the classpath
    add(classpath.files, "/app/")
    // Add the JAR file
    add(jarFile, jarTarget)

    env("PORT","8080")
    // On launch run Java with the classpath and the main class
    entryPoint("java", vmArgs(0), vmArgs(1), vmArgs(2), vmArgs(3), "-cp", classpathString, mainclass)
  }
}