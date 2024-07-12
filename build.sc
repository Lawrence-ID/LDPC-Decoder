// import Mill dependency
import mill._
import mill.scalalib._
import mill.scalalib.publish._
import mill.define.Sources
import mill.modules.Util
import mill.scalalib.TestModule.ScalaTest
import scalalib._
// support BSP
import mill.bsp._

import $file.cde.common

object cde extends CDE

trait CDE
  extends millbuild.cde.common.CDEModule
    with ldpcDecPublishModule
    with ScalaModule {

  override def scalaVersion = ldpcDecTop.scalaVersion

  override def millSourcePath = os.pwd / "cde" / "cde"
}

object ldpcDecTop extends SbtModule { m =>
  override def millSourcePath = os.pwd
  override def scalaVersion = "2.13.12"
  override def scalacOptions = Seq(
    "-language:reflectiveCalls",
    "-deprecation",
    "-feature",
    "-Xcheckinit",
  )
  override def ivyDeps = Agg(
    ivy"org.chipsalliance::chisel:6.2.0",
  )
  override def scalacPluginIvyDeps = Agg(
    ivy"org.chipsalliance:::chisel-plugin:6.2.0",
  )
  object test extends SbtModuleTests with TestModule.ScalaTest {
    override def ivyDeps = m.ivyDeps() ++ Agg(
      ivy"org.scalatest::scalatest::3.2.16"
    )
  }
  def cdeModule = cde

  override def moduleDeps = super.moduleDeps ++ Seq(cdeModule)

}

trait ldpcDecPublishModule
  extends PublishModule {
  def pomSettings = PomSettings(
    description = artifactName(),
    organization = "bupt",
    url = "https://github.com/Lawrence-ID/ldpcDec",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl.github("", ""),
    developers = Seq(
      Developer("Lawrence-ID", "Peng Xiao", "https://github.com/Lawrence-ID")
    )
  )

  override def publishVersion: T[String] = T("1.6-SNAPSHOT")
}
