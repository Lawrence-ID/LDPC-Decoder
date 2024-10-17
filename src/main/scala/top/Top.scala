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
import ldpcdecoder._
import utility._

trait HasDecoderParameter{
  implicit val p: Parameters
  val decoder = p(DecParamsKey)
  val debugOpts = p(DebugOptionsKey)
}

class LDPCDecoderTop ()(implicit p: Parameters) extends LazyModule with HasDecParameter{

  // class LDPCDecoderTopImp(wrapper: LDPCDecoderTop) extends LazyModuleImp(wrapper){
  //   val io = IO(new Bundle{
  //     val in = Input(UInt(MaxZSize.W))
  //     val out = Output(UInt(MaxZSize.W))
  //   })
  //   io.out := io.in
  // }

  class LDPCDecoderImp(wrapper: LDPCDecoderTop) extends LazyModuleImp(wrapper) {
    val io = IO(new Bundle{
        val llrRAddr       = ValidIO(UInt(log2Ceil(ColNum).W))
        val shiftValue     = ValidIO(UInt(log2Ceil(MaxZSize).W))
        val llrRIsLastCol  = Output(Bool())
        val llrReadyIsLastCal = Output(Bool())
        val c2vRamReq = ValidIO(UInt(log2Ceil(LayerNum).W))
        val v2cFifoIn      = Output(Bool())
        val vnuCoreEn      = Output(Bool())
        val vnuCoreCounter = Output(UInt(log2Ceil(MaxDegreeOfCNU).W))
        val v2cFifoOut     = Output(Bool())
        val cnuCoreEn      = Output(Bool())
        val cnuCoreCounter = Output(UInt(log2Ceil(MaxDegreeOfCNU).W))
        val reShiftValue   = ValidIO(UInt(log2Ceil(MaxZSize).W))
        val llrWAddr       = ValidIO(UInt(log2Ceil(ColNum).W))
        val decoupledFifoIn = Output(Bool())
        val decoupledFifoOut = Output(Bool())
    })

    val GCU = Module(new GCU)
    io.llrRAddr         := GCU.io.llrRAddr      
    io.shiftValue       := GCU.io.shiftValue    
    io.llrRIsLastCol    := GCU.io.llrRIsLastCol
    io.llrReadyIsLastCal:= GCU.io.llrReadyIsLastCal
    io.c2vRamReq        := GCU.io.c2vRamReq
    io.v2cFifoIn        := GCU.io.v2cFifoIn     
    io.vnuCoreEn        := GCU.io.vnuCoreEn     
    io.vnuCoreCounter   := GCU.io.vnuCoreCounter
    io.decoupledFifoIn  := GCU.io.decoupledFifoIn
    io.decoupledFifoOut := GCU.io.decoupledFifoOut
    io.v2cFifoOut       := GCU.io.v2cFifoOut    
    io.cnuCoreEn        := GCU.io.cnuCoreEn     
    io.cnuCoreCounter   := GCU.io.cnuCoreCounter
    io.reShiftValue     := GCU.io.reShiftValue  
    io.llrWAddr         := GCU.io.llrWAddr  

    // val VNUs = Seq.fill(MaxZSize)(Module(new VNUCore))
    // val CNUs = Seq.fill(MaxZSize)(Module(new CNUCore))

    // val Mc2vRAM = Module(new SRAMTemplate(
    //   UInt((C2VRowMsgBits * MaxZSize).W),
    //   set = LayerNum,
    //   withClockGate = true
    // ))
    // val Mv2cSignRAM

    // val Mv2cFifo
    // val DecoupledFifo

  }

  lazy val module = new LDPCDecoderImp(this)
}

object TopMain extends App {

    val (config, firrtlOpts, firtoolOpts) = ArgParser.parse(args)

    val decoder = DisableMonitors(p => LazyModule(new LDPCDecoderTop()(p)))(config)

    (new ChiselStage).execute(firrtlOpts, Seq(ChiselGeneratorAnnotation(() => decoder.module)))
}