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

  class LDPCDecoderTopImp(wrapper: LDPCDecoderTop) extends LazyModuleImp(wrapper) {
    val io = IO(new Bundle{
        val llrShifted     = Input(Vec(MaxZSize, SInt(LLRBits.W)))
        val ZSize          = Input(UInt(log2Ceil(MaxZSize).W))
        val llrBeforeReShifted   = Output(Vec(MaxZSize, SInt(LLRBits.W)))

        val llrRAddr       = ValidIO(UInt(log2Ceil(ColNum).W))
        val shiftValue     = ValidIO(UInt(log2Ceil(MaxZSize).W))
        val isLastCol      = Output(Bool())
        val c2vRamLayerR   = ValidIO(UInt(log2Ceil(LayerNum).W))
        val c2vRamLayerW   = ValidIO(UInt(log2Ceil(LayerNum).W))
        val v2cFifoIn      = Output(Bool())
        val vnuCoreEn      = Output(Bool())
        val vnuCoreCounter = Output(UInt(log2Ceil(MaxDegreeOfCNU).W))
        val v2cFifoOut     = Output(Bool())
        val cnuCoreEn      = Output(Bool())
        val cnuCoreCounter = Output(UInt(log2Ceil(MaxDegreeOfCNU).W))
        val reShiftValue   = ValidIO(UInt(log2Ceil(MaxZSize).W))
        val llrWAddr       = ValidIO(UInt(log2Ceil(ColNum).W))
    })

    val gcu = Module(new GCU())

    val vnus = Module(new VNUs())

    val Mc2vRAM = Module(new SRAMTemplate(Vec(MaxZSize, UInt(C2VMsgBits.W)), set=LayerNum, way=1, shouldReset=true, holdRead=true, singlePort=false, bypassWrite=true))
    Mc2vRAM.io.r.req.valid       := gcu.io.c2vRamLayerR.valid
    Mc2vRAM.io.r.req.bits.setIdx := gcu.io.c2vRamLayerR.bits

    vnus.io.in.en         := gcu.io.vnuCoreEn
    vnus.io.in.ZSize      := io.ZSize
    vnus.io.in.c2vMsg     := Mc2vRAM.io.r.resp.data(0)
    vnus.io.in.counter    := gcu.io.vnuCoreCounter
    vnus.io.in.isLastCol  := DelayN(gcu.io.isLastCol, DelayOfShifter)
    vnus.io.in.shiftedLLR := io.llrShifted

    val Mv2cFifo = Module(new Mv2cFIFO(Vec(MaxZSize, SInt((LLRBits + 1).W)), 24))
    Mv2cFifo.io.in.valid := gcu.io.v2cFifoIn
    Mv2cFifo.io.in.bits  := vnus.io.out.v2cMsg
    Mv2cFifo.io.out.ready := gcu.io.v2cFifoOut

    val Mv2cLayerGsgnFifo = Module(new Mv2cFIFO(Vec(MaxZSize, UInt(1.W)), 2))
    val Mv2cLayerMin0Fifo = Module(new Mv2cFIFO(Vec(MaxZSize, UInt(LLRBits.W)), 2))
    val Mv2cLayerMin1Fifo = Module(new Mv2cFIFO(Vec(MaxZSize, UInt(LLRBits.W)), 2))
    val Mv2cLayerIdx0Fifo = Module(new Mv2cFIFO(Vec(MaxZSize, UInt(log2Ceil(MaxDegreeOfCNU).W)), 2))

    Mv2cLayerGsgnFifo.io.in.valid := vnus.io.out.vnuLayerDone
    Mv2cLayerMin0Fifo.io.in.valid := vnus.io.out.vnuLayerDone
    Mv2cLayerMin1Fifo.io.in.valid := vnus.io.out.vnuLayerDone
    Mv2cLayerIdx0Fifo.io.in.valid := vnus.io.out.vnuLayerDone

    Mv2cLayerGsgnFifo.io.in.bits := vnus.io.out.gsgn
    Mv2cLayerMin0Fifo.io.in.bits := vnus.io.out.min0
    Mv2cLayerMin1Fifo.io.in.bits := vnus.io.out.min1
    Mv2cLayerIdx0Fifo.io.in.bits := vnus.io.out.idx0

    Mv2cLayerGsgnFifo.io.out.ready := gcu.io.cnuCoreEn
    Mv2cLayerMin0Fifo.io.out.ready := gcu.io.cnuCoreEn
    Mv2cLayerMin1Fifo.io.out.ready := gcu.io.cnuCoreEn
    Mv2cLayerIdx0Fifo.io.out.ready := gcu.io.cnuCoreEn

    val cnus = Module(new CNUs())

    cnus.io.in.en      := gcu.io.cnuCoreEn
    cnus.io.in.ZSize   := io.ZSize
    cnus.io.in.v2cMsg  := Mv2cFifo.io.out.bits
    cnus.io.in.counter := gcu.io.cnuCoreCounter
    cnus.io.in.gsgn    := Mv2cLayerGsgnFifo.io.out.bits
    cnus.io.in.min0    := Mv2cLayerMin0Fifo.io.out.bits
    cnus.io.in.min1    := Mv2cLayerMin1Fifo.io.out.bits
    cnus.io.in.idx0    := Mv2cLayerIdx0Fifo.io.out.bits

    Mc2vRAM.io.w.apply(
      valid   = gcu.io.c2vRamLayerW.valid,
      setIdx  = gcu.io.c2vRamLayerW.bits,
      data    = cnus.io.out.c2vMsgOld,
      waymask = 1.U // Don't Care
    )
    io.llrBeforeReShifted := cnus.io.out.LLR
    

    io.llrRAddr       := gcu.io.llrRAddr      
    io.shiftValue     := gcu.io.shiftValue    
    io.isLastCol      := gcu.io.isLastCol     
    io.c2vRamLayerR   := gcu.io.c2vRamLayerR  
    io.c2vRamLayerW   := gcu.io.c2vRamLayerW  
    io.v2cFifoIn      := gcu.io.v2cFifoIn     
    io.vnuCoreEn      := gcu.io.vnuCoreEn     
    io.vnuCoreCounter := gcu.io.vnuCoreCounter
    io.v2cFifoOut     := gcu.io.v2cFifoOut    
    io.cnuCoreEn      := gcu.io.cnuCoreEn     
    io.cnuCoreCounter := gcu.io.cnuCoreCounter
    io.reShiftValue   := gcu.io.reShiftValue  
    io.llrWAddr       := gcu.io.llrWAddr      
  }

  // lazy val module = new LDPCDecoderTopImp(this)
  lazy val module = new LDPCDecoderTopImp(this)
}

object TopMain extends App {

    val (config, firrtlOpts, firtoolOpts) = ArgParser.parse(args)

    val decoder = DisableMonitors(p => LazyModule(new LDPCDecoderTop()(p)))(config)

    (new ChiselStage).execute(firrtlOpts, Seq(ChiselGeneratorAnnotation(() => decoder.module)))
}