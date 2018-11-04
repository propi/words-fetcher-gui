enablePlugins(ScalaJSPlugin)

name := "words-fetcher-gui"

version := "1.0"

scalaVersion := "2.12.2"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")
scalacOptions += "-P:scalajs:sjsDefinedByDefault"

scalaJSUseMainModuleInitializer := true

jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv()

libraryDependencies += "com.thoughtworks.binding" %%% "dom" % "11.0.1"

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)