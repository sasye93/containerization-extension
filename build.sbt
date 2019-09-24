name := "scala-loci-containerize-extension"

version := "0.1"

scalaVersion := "2.12.8"

resolvers += Resolver.bintrayRepo("stg-tud", "maven")

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.scalatest" %% "scalatest" % "3.0.8" % "test",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
  "de.tuda.stg" %% "scala-loci-lang" % "0.3.0",
  "de.tuda.stg" %% "scala-loci-serializer-upickle" % "0.3.0",
  "de.tuda.stg" %% "scala-loci-communicator-ws-akka" % "0.3.0",
  "de.tuda.stg" %% "scala-loci-communicator-tcp" % "0.3.0",
  "de.tuda.stg" %% "scala-loci-lang-transmitter-rescala" % "0.3.0")

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch)

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
