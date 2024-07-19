package ldpcdecoder

import chisel3._
import chisel3.util._

import org.chipsalliance.cde.config.{Field, Parameters}

case object DecParamsKey extends Field[DecParameters]

case class DecParameters
(
    MaxZSize: Int = 384,
    LLRBits: Int = 6
)

case object DebugOptionsKey extends Field[DebugOptions]

case class DebugOptions
(
  FPGAPlatform: Boolean = false,
  EnableDebug: Boolean = false,
  EnablePerfDebug: Boolean = true,
)

trait HasDecParameter{
  implicit val p: Parameters
  def MaxZSize = p(DecParamsKey).MaxZSize
  def LLRBits = p(DecParamsKey).LLRBits
}