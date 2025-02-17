package top

// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage
import chisel3._
import chisel3.stage.ChiselGeneratorAnnotation
import chisel3.util._
import freechips.rocketchip
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.diplomacy.LazyModule
import freechips.rocketchip.tile._
import ldpcdecoder._
import org.chipsalliance.cde.config._
import utility._

trait HasDecoderParameter {
  implicit val p: Parameters
  val decoder   = p(DecParamsKey)
  val debugOpts = p(DebugOptionsKey)
}

class LDPCDecoderTop()(implicit p: Parameters) extends LazyModule with HasDecParameter {

  class LDPCDecoderImp(wrapper: LDPCDecoderTop) extends LazyModuleImp(wrapper) {
    val io = IO(new Bundle {
      // Input
      val in = Flipped(DecoupledIO(new LLRInTransferReq))

      // Output
      val out = DecoupledIO(new LLROutTransferReq)

      // Debug
      // val llrRAddr                 = ValidIO(UInt(log2Ceil(MaxColNum).W))
      // val shiftValue               = ValidIO(UInt(log2Ceil(MaxZSize).W))
      // val llrRIsLastCol            = Output(Bool())
      // val llrReadyToShiftIsLastCal = Output(Bool())
      // val c2vRamRdReq              = ValidIO(UInt(log2Ceil(MaxLayerNum).W))
      // val v2cSignRamRdReq          = ValidIO(UInt(log2Ceil(MaxEdgeNum).W))
      // val v2cSignRamWrReq          = ValidIO(UInt(log2Ceil(MaxEdgeNum).W))
      // val v2cFifoIn                = Output(Bool())
      // val vnuCoreEn                = Output(Bool())
      // val vnuCoreCounter           = Output(UInt(log2Ceil(MaxDegreeOfCNU).W))
      // val vnuLayerCounter          = Output(UInt(log2Ceil(MaxLayerNum).W))
      // val v2cFifoOut               = Output(Bool())
      // val cnuCoreEn                = Output(Bool())
      // val cnuCoreCounter           = Output(UInt(log2Ceil(MaxDegreeOfCNU).W))
      // val cnuLayerCounter          = Output(UInt(log2Ceil(MaxLayerNum).W))
      // val reShiftValue             = ValidIO(UInt(log2Ceil(MaxZSize).W))
      // val llrWAddr                 = ValidIO(UInt(log2Ceil(MaxColNum).W))
      // val decoupledFifoIn          = Output(Bool())
      // val decoupledFifoOut         = Output(Bool())
    })

    val llrInTransfer  = Module(new LLRInTransfer)
    val llrOutTransfer = Module(new LLROutTransfer)

    val ldpcDecoderCore = Module(new LDPCDecoderCore)

    llrInTransfer.io.in <> io.in
    ldpcDecoderCore.io.llrIn <> llrInTransfer.io.out
    llrOutTransfer.io.in <> ldpcDecoderCore.io.llrOut
    io.out <> llrOutTransfer.io.out

    // io.llrRAddr                 := ldpcDecoderCore.io.llrRAddr
    // io.shiftValue               := ldpcDecoderCore.io.shiftValue
    // io.llrRIsLastCol            := ldpcDecoderCore.io.llrRIsLastCol
    // io.llrReadyToShiftIsLastCal := ldpcDecoderCore.io.llrReadyToShiftIsLastCal
    // io.c2vRamRdReq              := ldpcDecoderCore.io.c2vRamRdReq
    // io.v2cSignRamRdReq          := ldpcDecoderCore.io.v2cSignRamRdReq
    // io.v2cSignRamWrReq          := ldpcDecoderCore.io.v2cSignRamWrReq
    // io.v2cFifoIn                := ldpcDecoderCore.io.v2cFifoIn
    // io.vnuCoreEn                := ldpcDecoderCore.io.vnuCoreEn
    // io.vnuCoreCounter           := ldpcDecoderCore.io.vnuCoreCounter
    // io.vnuLayerCounter          := ldpcDecoderCore.io.vnuLayerCounter
    // io.v2cFifoOut               := ldpcDecoderCore.io.v2cFifoOut
    // io.cnuCoreEn                := ldpcDecoderCore.io.cnuCoreEn
    // io.cnuCoreCounter           := ldpcDecoderCore.io.cnuCoreCounter
    // io.cnuLayerCounter          := ldpcDecoderCore.io.cnuLayerCounter
    // io.reShiftValue             := ldpcDecoderCore.io.reShiftValue
    // io.llrWAddr                 := ldpcDecoderCore.io.llrWAddr
    // io.decoupledFifoIn          := ldpcDecoderCore.io.decoupledFifoIn
    // io.decoupledFifoOut         := ldpcDecoderCore.io.decoupledFifoOut

  }

  lazy val module = new LDPCDecoderImp(this)
}

object TopMain extends App {

  val (config, firrtlOpts, firtoolOpts) = ArgParser.parse(args)

  val decoder = DisableMonitors(p => LazyModule(new LDPCDecoderTop()(p)))(config)

  (new ChiselStage).execute(firrtlOpts, Seq(ChiselGeneratorAnnotation(() => decoder.module)))
}
