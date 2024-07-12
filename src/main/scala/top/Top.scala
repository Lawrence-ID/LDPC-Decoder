package top

import chisel3._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage
import chisel3.stage.ChiselGeneratorAnnotation
import org.chipsalliance.cde.config.{Config, Parameters}

class ldpcDecTop (width: Int = 16) extends Module{
  val io = IO(new Bundle{
    val in = Input(UInt(width.W))
    val out = Output(UInt(width.W))
  })
  io.out := io.in
}

object TopMain extends App {
  (new ChiselStage).execute(
    Array("--target-dir", "build/rtl", "--target", "verilog"),
    Seq(ChiselGeneratorAnnotation(() => new ldpcDecTop()))
  )
}