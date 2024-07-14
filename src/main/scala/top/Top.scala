package top

import chisel3._
import chisel3.util._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage
import chisel3.stage.ChiselGeneratorAnnotation

import freechips.rocketchip
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.diplomacy.{LazyModule}
import freechips.rocketchip.tile._
import org.chipsalliance.cde.config._
import ldpcdec.DecParamsKey
import ldpcdec.DebugOptionsKey

trait HasDecoderParameter{
  implicit val p: Parameters
  val decoder = p(DecParamsKey)
  val debugOpts = p(DebugOptionsKey)
}

class ldpcDecTop ()(implicit p: Parameters) extends LazyModule with HasDecoderParameter{

  class ldpcDecTopImp(wrapper: ldpcDecTop) extends LazyModuleImp(wrapper){
    val io = IO(new Bundle{
      val in = Input(UInt(16.W))
      val out = Output(UInt(16.W))
    })
    io.out := io.in
  }

  lazy val module = new ldpcDecTopImp(this)
}

object TopMain extends App {

    val (config, firrtlOpts, firtoolOpts) = ArgParser.parse(args)

    val decoder = DisableMonitors(p => LazyModule(new ldpcDecTop()(p)))(config)

    (new ChiselStage).execute(firrtlOpts, Seq(ChiselGeneratorAnnotation(() => decoder.module)))
}