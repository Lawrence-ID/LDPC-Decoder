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

  // class LDPCDecoderTopImp(wrapper: LDPCDecoderTop) extends LazyModuleImp(wrapper){
  //   val io = IO(new Bundle{
  //     val in = Input(UInt(MaxZSize.W))
  //     val out = Output(UInt(MaxZSize.W))
  //   })
  //   io.out := io.in
  // }

  class LDPCDecoderImp(wrapper: LDPCDecoderTop) extends LazyModuleImp(wrapper) {
    val io = IO(new Bundle {
      val isBG1      = Input(Bool())
      val zSize      = Input(UInt(log2Ceil(MaxZSize).W))
      val llrInValid = Input(Bool())
      val llrInData  = Input(Vec(MaxZSize, UInt(LLRBits.W)))
      // val llrInReady = Output(Bool())

      // val llrOutValid = Output(Bool())
      // val llrOutReady = Input(Bool())
      // val llrOut = Output(UInt(MaxZSize.W))

      // Debug
      val llrRAddr          = Output(ValidIO(UInt(log2Ceil(MaxColNum).W)))
      val shiftValue        = Output(ValidIO(UInt(log2Ceil(MaxZSize).W)))
      val llrRIsLastCol     = Output(Bool())
      val llrReadyIsLastCal = Output(Bool())
      val c2vRamRdReq       = Output(ValidIO(UInt(log2Ceil(MaxLayerNum).W)))
      val v2cSignRamRdReq   = Output(ValidIO(UInt(log2Ceil(MaxEdgeNum).W)))
      val v2cSignRamWrReq   = Output(ValidIO(UInt(log2Ceil(MaxEdgeNum).W)))
      val v2cFifoIn         = Output(Bool())
      val vnuCoreEn         = Output(Bool())
      val vnuCoreCounter    = Output(UInt(log2Ceil(MaxDegreeOfCNU).W))
      val vnuLayerCounter   = Output(UInt(log2Ceil(MaxLayerNum).W))
      val v2cFifoOut        = Output(Bool())
      val cnuCoreEn         = Output(Bool())
      val cnuCoreCounter    = Output(UInt(log2Ceil(MaxDegreeOfCNU).W))
      val cnuLayerCounter   = Output(UInt(log2Ceil(MaxLayerNum).W))
      val reShiftValue      = Output(ValidIO(UInt(log2Ceil(MaxZSize).W)))
      val llrWAddr          = Output(ValidIO(UInt(log2Ceil(MaxColNum).W)))
      val decoupledFifoIn   = Output(Bool())
      val decoupledFifoOut  = Output(Bool())
      val llrOut            = Output(Vec(MaxZSize, UInt(LLRBits.W)))
    })

    val isBG1 = io.isBG1

    // =====================RAMs and Fifos Definition=====================
    val LLRRAM = Module(new SRAMTemplate(
      Vec(MaxZSize, UInt(LLRBits.W)),
      set = MaxColNum,
      singlePort = false, // need read and write port both
      bypassWrite = true,
      withClockGate = true
    ))

    val cyclicShifter   = Module(new CyclicShifter(true))
    val reCyclicShifter = Module(new CyclicShifter(false))
    val vnus            = Module(new VNUs)
    val cnus            = Module(new CNUs)
    val GCU             = Module(new GCU)

    val Mc2vRAM = Module(new SRAMTemplate(
      Vec(MaxZSize, UInt(C2VRowMsgBits.W)),
      set = MaxLayerNum,
      holdRead = true
    ))

    val Mv2cSignRAM = Module(new SRAMTemplate(
      Vec(MaxZSize, UInt(1.W)),
      set = MaxEdgeNum
    ))

    val Mv2cFifo      = Module(new Queue(Vec(MaxZSize, SInt((LLRBits + 1).W)), 20))
    val DecoupledFifo = Module(new Queue(Vec(MaxZSize, new C2VMsgInfo), 2))

    // =====================Logic and wire connection=====================
    val llrRAMRData = LLRRAM.io.r(GCU.io.llrRAddr.valid, GCU.io.llrRAddr.bits).resp.data(0)

    LLRRAM.io.w(
      valid = GCU.io.llrWAddr.valid,
      data = Mux(GCU.io.llrIniting, io.llrInData, reCyclicShifter.io.out.bits),
      setIdx = GCU.io.llrWAddr.bits,
      waymask = 1.U
    )

    GCU.io.llrInValid := io.llrInValid
    GCU.io.isBG1      := isBG1

    cyclicShifter.io.in.valid          := GCU.io.shiftValue.valid // shift en
    cyclicShifter.io.in.bits.llr       := llrRAMRData             // need to be read from llrRam
    cyclicShifter.io.in.bits.zSize     := io.zSize
    cyclicShifter.io.in.bits.shiftSize := GCU.io.shiftValue.bits
    cyclicShifter.io.out.ready         := GCU.io.vnuCoreEn

    // VNUs Input
    vnus.io.in.en           := GCU.io.vnuCoreEn
    vnus.io.in.zSize        := io.zSize
    vnus.io.in.counter      := GCU.io.vnuCoreCounter
    vnus.io.in.v2cSignOld   := Mv2cSignRAM.io.r(GCU.io.v2cSignRamRdReq.valid, GCU.io.v2cSignRamRdReq.bits).resp.data(0)
    vnus.io.in.c2vRowMsgOld := Mc2vRAM.io.r(GCU.io.c2vRamRdReq.valid, GCU.io.c2vRamRdReq.bits).resp.data(0)
    vnus.io.in.shiftedLLR   := cyclicShifter.io.out.bits

    // VNUs Output
    Mv2cFifo.io.enq.valid := vnus.io.out.v2cMsg.valid // equal to GCU.io.vnuCoreEn
    Mv2cFifo.io.enq.bits  := vnus.io.out.v2cMsg.bits

    DecoupledFifo.io.enq.valid := GCU.io.decoupledFifoIn
    DecoupledFifo.io.enq.bits  := vnus.io.out.c2vRowMsg

    // CNUs Input
    cnus.io.in.en        := GCU.io.cnuCoreEn
    cnus.io.in.zSize     := io.zSize
    cnus.io.in.counter   := GCU.io.cnuCoreCounter
    cnus.io.in.v2cMsg    := Mv2cFifo.io.deq.bits
    cnus.io.in.c2vRowMsg := DecoupledFifo.io.deq.bits

    Mv2cFifo.io.deq.ready      := GCU.io.cnuCoreEn
    DecoupledFifo.io.deq.ready := GCU.io.decoupledFifoOut

    // CNUs Output
    Mc2vRAM.io.w(
      valid = GCU.io.cnuCoreEn && GCU.io.cnuCoreCounter === 0.U,
      data = cnus.io.out.c2vMsg.bits,
      setIdx = GCU.io.cnuLayerCounter,
      waymask = 1.U
    )

    Mv2cSignRAM.io.w(
      valid = GCU.io.v2cSignRamWrReq.valid && cnus.io.out.v2cMsgSign.valid, // equal to cnuCoreEn
      data = cnus.io.out.v2cMsgSign.bits,
      setIdx = GCU.io.v2cSignRamWrReq.bits,
      waymask = 1.U
    )

    // reshifted
    reCyclicShifter.io.in.valid          := GCU.io.reShiftValue.valid
    reCyclicShifter.io.in.bits.llr       := cnus.io.out.unshiftedLLR
    reCyclicShifter.io.in.bits.zSize     := io.zSize
    reCyclicShifter.io.in.bits.shiftSize := GCU.io.reShiftValue.bits
    reCyclicShifter.io.out.ready         := true.B

    io.llrOut := reCyclicShifter.io.out.bits // TODO: QSN need 3 cycle

    // GCU Output
    io.llrRAddr          := GCU.io.llrRAddr
    io.shiftValue        := GCU.io.shiftValue
    io.llrRIsLastCol     := GCU.io.llrRIsLastCol
    io.llrReadyIsLastCal := GCU.io.llrReadyIsLastCal
    io.c2vRamRdReq       := GCU.io.c2vRamRdReq
    io.v2cSignRamRdReq   := GCU.io.v2cSignRamRdReq
    io.v2cSignRamWrReq   := GCU.io.v2cSignRamWrReq
    io.v2cFifoIn         := GCU.io.v2cFifoIn
    io.vnuCoreEn         := GCU.io.vnuCoreEn
    io.vnuCoreCounter    := GCU.io.vnuCoreCounter
    io.vnuLayerCounter   := GCU.io.vnuLayerCounter
    io.decoupledFifoIn   := GCU.io.decoupledFifoIn
    io.decoupledFifoOut  := GCU.io.decoupledFifoOut
    io.v2cFifoOut        := GCU.io.v2cFifoOut
    io.cnuCoreEn         := GCU.io.cnuCoreEn
    io.cnuCoreCounter    := GCU.io.cnuCoreCounter
    io.cnuLayerCounter   := GCU.io.cnuLayerCounter
    io.reShiftValue      := GCU.io.reShiftValue
    io.llrWAddr          := GCU.io.llrWAddr

  }

  lazy val module = new LDPCDecoderImp(this)
}

object TopMain extends App {

  val (config, firrtlOpts, firtoolOpts) = ArgParser.parse(args)

  val decoder = DisableMonitors(p => LazyModule(new LDPCDecoderTop()(p)))(config)

  (new ChiselStage).execute(firrtlOpts, Seq(ChiselGeneratorAnnotation(() => decoder.module)))
}
