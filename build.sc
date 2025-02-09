import $ivy.`com.goyeau::mill-git:0.2.2`
import $ivy.`com.goyeau::mill-scalafix:0.2.2`
import $ivy.`com.lihaoyi::mill-contrib-buildinfo:$MILL_VERSION`
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest_mill0.9:0.4.0`
import $ivy.`io.github.davidgregory084::mill-tpolecat:0.2.0`
import com.goyeau.mill.git.{GitVersionModule, GitVersionedPublishModule}
import com.goyeau.mill.scalafix.StyleModule
import de.tobiasroeser.mill.integrationtest._
import io.github.davidgregory084.TpolecatModule
import mill._
import mill.contrib.buildinfo.BuildInfo
import mill.scalalib.publish.{Developer, License, PomSettings, VersionControl}
import scalalib._

object `mill-scalafix`
    extends ScalaModule
    with TpolecatModule
    with StyleModule
    with BuildInfo
    with GitVersionedPublishModule {
  override def scalaVersion = "2.13.5"

  lazy val millVersion = "0.9.8"
  override def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    ivy"com.lihaoyi::mill-main:$millVersion",
    ivy"com.lihaoyi::mill-scalalib:$millVersion"
  )
  val scalafixVersion = "0.9.29"
  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"ch.epfl.scala:scalafix-interfaces:$scalafixVersion",
    ivy"org.scala-lang.modules::scala-collection-compat:2.4.4",
    ivy"org.scala-lang.modules::scala-java8-compat:1.0.0"
  )

  override def buildInfoPackageName = Some("com.goyeau.mill.scalafix")
  override def buildInfoMembers     = Map("scalafixVersion" -> scalafixVersion)

  override def publishVersion = GitVersionModule.version(withSnapshotSuffix = true)()
  def pomSettings =
    PomSettings(
      description = "A Scalafix plugin for Mill build tool",
      organization = "com.goyeau",
      url = "https://github.com/joan38/mill-scalafix",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("joan38", "mill-scalafix"),
      developers = Seq(Developer("joan38", "Joan Goyeau", "https://github.com/joan38"))
    )

  val semanticdbScalac = ivy"org.scalameta:::semanticdb-scalac:4.4.19"

  override def generatedSources = T {
    val dest = T.ctx.dest
    os.write(
      dest / "Versions.scala",
      s"""package com.goyeau.mill.scalafix
         |object Versions {
         |  val semanticdbScalac = "${semanticdbScalac.dep.version}"
         |}
         |""".stripMargin
    )
    super.generatedSources() ++ Seq(PathRef(dest))
  }
}

object itest extends MillIntegrationTestModule {
  def millTestVersion  = `mill-scalafix`.millVersion
  def pluginsUnderTest = Seq(`mill-scalafix`)
  override def testInvocations =
    Seq(
      PathRef(sources().head.path / "fix") -> Seq(
        TestInvocation.Targets(Seq("__.fix")),
        TestInvocation.Targets(Seq("verify"))
      ),
      PathRef(sources().head.path / "check") -> Seq(
        TestInvocation.Targets(Seq("__.fix", "--check"))
      ),
      PathRef(sources().head.path / "check-failed") -> Seq(
        TestInvocation.Targets(Seq("__.fix", "--check"), expectedExitCode = 1)
      ),
      PathRef(sources().head.path / "custom-rule") -> Seq(
        TestInvocation.Targets(Seq("__.fix")),
        TestInvocation.Targets(Seq("verify"))
      ),
      PathRef(sources().head.path / "no-source") -> Seq(
        TestInvocation.Targets(Seq("__.fix"))
      )
    )
}
