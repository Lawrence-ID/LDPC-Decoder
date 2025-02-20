package ldpcdecoder

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import utility._

class LDPCDecoderReq(implicit p: Parameters) extends DecBundle {
  val isBG1  = Bool()
  val iLS    = UInt(log2Ceil(8).W)
  val zPow   = UInt(log2Ceil(8).W)
  val rawLLR = Vec(MaxZSize, UInt(LLRBits.W))
}

class LDPCDecoderResp(implicit p: Parameters) extends DecBundle {
  val idx        = UInt(log2Ceil(MaxColNum).W)
  val last       = Bool()
  val decodedLLR = Vec(MaxZSize, UInt(LLRBits.W))
}

object zSizeCalculator {
  def apply(iLS: UInt, zPow: UInt): UInt = {

    val zSizeTable = VecInit(Seq(
      VecInit(Seq(2.U(9.W), 4.U(9.W), 8.U(9.W), 16.U(9.W), 32.U(9.W), 64.U(9.W), 128.U(9.W), 256.U(9.W))),
      VecInit(Seq(3.U(9.W), 6.U(9.W), 12.U(9.W), 24.U(9.W), 48.U(9.W), 96.U(9.W), 192.U(9.W), 384.U(9.W))),
      VecInit(Seq(5.U(9.W), 10.U(9.W), 20.U(9.W), 40.U(9.W), 80.U(9.W), 160.U(9.W), 320.U(9.W), 0.U(9.W))),
      VecInit(Seq(7.U(9.W), 14.U(9.W), 28.U(9.W), 56.U(9.W), 112.U(9.W), 224.U(9.W), 0.U(9.W), 0.U(9.W))),
      VecInit(Seq(9.U(9.W), 18.U(9.W), 36.U(9.W), 72.U(9.W), 144.U(9.W), 288.U(9.W), 0.U(9.W), 0.U(9.W))),
      VecInit(Seq(11.U(9.W), 22.U(9.W), 44.U(9.W), 88.U(9.W), 176.U(9.W), 352.U(9.W), 0.U(9.W), 0.U(9.W))),
      VecInit(Seq(13.U(9.W), 26.U(9.W), 52.U(9.W), 104.U(9.W), 208.U(9.W), 0.U(9.W), 0.U(9.W), 0.U(9.W))),
      VecInit(Seq(15.U(9.W), 30.U(9.W), 60.U(9.W), 120.U(9.W), 240.U(9.W), 0.U(9.W), 0.U(9.W), 0.U(9.W)))
    ))

    zSizeTable(iLS)(zPow)
  }
}

class LDPCDecoderCore(implicit p: Parameters) extends DecModule {
  val io = IO(new Bundle {
    // Input
    val llrIn = Flipped(DecoupledIO(new LDPCDecoderReq))

    // Output
    val llrOut = DecoupledIO(new LDPCDecoderResp)

    // Debug
    val llrRAddr                 = ValidIO(UInt(log2Ceil(MaxColNum).W))
    val shiftValue               = ValidIO(UInt(log2Ceil(MaxZSize).W))
    val llrRIsLastCol            = Output(Bool())
    val llrReadyToShiftIsLastCal = Output(Bool())
    val c2vRamRdReq              = ValidIO(UInt(log2Ceil(MaxLayerNum).W))
    val v2cSignRamRdReq          = ValidIO(UInt(log2Ceil(MaxEdgeNum).W))
    val v2cSignRamWrReq          = ValidIO(UInt(log2Ceil(MaxEdgeNum).W))
    val v2cFifoIn                = Output(Bool())
    val vnuCoreEn                = Output(Bool())
    val vnuCoreCounter           = Output(UInt(log2Ceil(MaxDegreeOfCNU).W))
    val vnuLayerCounter          = Output(UInt(log2Ceil(MaxLayerNum).W))
    val v2cFifoOut               = Output(Bool())
    val cnuCoreEn                = Output(Bool())
    val cnuCoreCounter           = Output(UInt(log2Ceil(MaxDegreeOfCNU).W))
    val cnuLayerCounter          = Output(UInt(log2Ceil(MaxLayerNum).W))
    val reShiftValue             = ValidIO(UInt(log2Ceil(MaxZSize).W))
    val llrWAddr                 = ValidIO(UInt(log2Ceil(MaxColNum).W))
    val decoupledFifoIn          = Output(Bool())
    val decoupledFifoOut         = Output(Bool())
  })

  // =====================RAMs and Fifos Definition=====================
  val LLRRAM = Module(new SRAMTemplate(
    Vec(MaxZSize, UInt(LLRBits.W)),
    set = MaxColNum,
    singlePort = false, // need read and write port both
    bypassWrite = true,
    withClockGate = false,
    separateGateClock = false,
    holdRead = true
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

  val decodeMaxIterDone = Wire(Bool())
  val llrInitCounter    = RegInit(0.U(log2Ceil(MaxColNum).W))
  val llrOutputCounter  = RegInit(0.U(log2Ceil(MaxColNum).W))

  // =====================State Machine=====================
  val m_idle :: m_llrInput :: m_decoding :: m_llrOutput :: Nil = Enum(4)
  val state                                                    = RegInit(m_idle)
  val next_state                                               = WireDefault(state)

  val isBG1  = RegEnable(io.llrIn.bits.isBG1, state === m_idle && next_state === m_llrInput)
  val iLS    = RegEnable(io.llrIn.bits.iLS, state === m_idle && next_state === m_llrInput)
  val zPow   = RegEnable(io.llrIn.bits.zPow, state === m_idle && next_state === m_llrInput)
  val colNum = Mux(isBG1, BG1ColNum.U, BG2ColNum.U)

  val zSize = RegEnable(
    zSizeCalculator(iLS, zPow), 
    0.U(log2Ceil(MaxZSize).W), 
    state === m_llrInput && next_state === m_decoding
  )
  val maskByZSize = RegEnable(
    (1.U << zSize) - 1.U,
    0.U(MaxZSize.W),
    RegNext(state === m_llrInput && next_state === m_decoding)
  )

  dontTouch(state)
  dontTouch(next_state)
  state := next_state

  switch(state) {
    is(m_idle) {
      when(io.llrIn.fire) {
        next_state := m_llrInput
      }
    }
    is(m_llrInput) {
      when(llrInitCounter === colNum - 1.U && io.llrIn.fire) {
        next_state := m_decoding
      }
    }
    is(m_decoding) {
      when(decodeMaxIterDone) {
        next_state := m_llrOutput
      }
    }
    is(m_llrOutput) {
      when(llrOutputCounter === colNum - 1.U) {
        next_state := m_idle
      }
    }
  }

  io.llrIn.ready := state === m_idle || state === m_llrInput

  // =====================Logic and wire connection=====================

  // llrRAM Write In
  when(next_state =/= m_llrInput) {
    llrInitCounter := 0.U
  }.elsewhen(io.llrIn.fire) { // next_state === m_llrInput
    llrInitCounter := llrInitCounter + 1.U
  }

  val llrRAMWAddr = Mux(io.llrIn.ready, llrInitCounter, GCU.io.llrWAddr.bits)
  val llrRAMWData = Mux(io.llrIn.ready, io.llrIn.bits.rawLLR, reCyclicShifter.io.out.bits)

  LLRRAM.io.w(
    valid = (state === m_decoding && GCU.io.llrWAddr.valid) || io.llrIn.fire,
    data = llrRAMWData,
    setIdx = llrRAMWAddr,
    waymask = 1.U
  )

  // llrRAM Read Out
  val llrRamOutputRen = state === m_llrOutput && io.llrOut.ready
  when(next_state =/= m_llrOutput) {
    llrOutputCounter := 0.U
  }.elsewhen(llrRamOutputRen) { // next_state === m_llrOutput
    llrOutputCounter := llrOutputCounter + 1.U
  }

  val llrRAMRData = LLRRAM.io.r(
    valid = (state === m_decoding && GCU.io.llrRAddr.valid) || llrRamOutputRen,
    setIdx = Mux(io.llrOut.valid, llrOutputCounter, GCU.io.llrRAddr.bits)
  ).resp.data(0)

  io.llrOut.valid           := DelayN(llrRamOutputRen, 1)
  io.llrOut.bits.idx        := DelayN(llrOutputCounter, 1)
  io.llrOut.bits.decodedLLR := llrRAMRData
  io.llrOut.bits.last       := io.llrOut.valid & !llrRamOutputRen

  // GCU IO
  GCU.io.gcuEn      := state === m_decoding
  decodeMaxIterDone := GCU.io.decodeMaxIterDone
  GCU.io.isBG1      := isBG1
  GCU.io.zSize      := zSize
  GCU.io.iLS        := iLS
  GCU.io.zPow       := zPow

  // CyclicShifter Input
  cyclicShifter.io.in.valid          := GCU.io.shiftValue.valid // shift en
  cyclicShifter.io.in.bits.llr       := llrRAMRData             // need to be read from llrRam
  cyclicShifter.io.in.bits.zSize     := zSize
  cyclicShifter.io.in.bits.mask      := maskByZSize
  cyclicShifter.io.in.bits.iLS       := iLS
  cyclicShifter.io.in.bits.zPow      := zPow
  cyclicShifter.io.in.bits.shiftSize := GCU.io.shiftValue.bits
  cyclicShifter.io.out.ready         := GCU.io.vnuCoreEn

  // VNUs Input
  val v2cSignPrevIter = Mux(
    GCU.io.vnuIterCounter =/= 0.U,
    Mv2cSignRAM.io.r(GCU.io.v2cSignRamRdReq.valid, GCU.io.v2cSignRamRdReq.bits).resp.data(0),
    VecInit(Seq.fill(MaxZSize)(0.U(1.W)))
  )
  val c2vRowMsgPrevIter = Mux(
    GCU.io.vnuIterCounter =/= 0.U,
    Mc2vRAM.io.r(GCU.io.c2vRamRdReq.valid, GCU.io.c2vRamRdReq.bits).resp.data(0),
    VecInit(Seq.fill(MaxZSize)(0.U(C2VRowMsgBits.W)))
  )
  vnus.io.in.en           := GCU.io.vnuCoreEn
  vnus.io.in.mask         := maskByZSize
  vnus.io.in.counter      := GCU.io.vnuCoreCounter
  vnus.io.in.v2cSignOld   := v2cSignPrevIter
  vnus.io.in.c2vRowMsgOld := c2vRowMsgPrevIter
  vnus.io.in.shiftedLLR   := cyclicShifter.io.out.bits

  // VNUs Output
  Mv2cFifo.io.enq.valid := vnus.io.out.v2cMsg.valid // equal to GCU.io.vnuCoreEn
  Mv2cFifo.io.enq.bits  := vnus.io.out.v2cMsg.bits

  DecoupledFifo.io.enq.valid := GCU.io.decoupledFifoIn
  DecoupledFifo.io.enq.bits  := vnus.io.out.c2vRowMsg

  // CNUs Input
  cnus.io.in.en        := GCU.io.cnuCoreEn
  cnus.io.in.mask      := maskByZSize
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
  reCyclicShifter.io.in.bits.zSize     := zSize
  reCyclicShifter.io.in.bits.mask      := maskByZSize
  reCyclicShifter.io.in.bits.iLS       := iLS
  reCyclicShifter.io.in.bits.zPow      := zPow
  reCyclicShifter.io.in.bits.shiftSize := GCU.io.reShiftValue.bits
  reCyclicShifter.io.out.ready         := true.B

  // GCU Output
  io.llrRAddr                 := GCU.io.llrRAddr
  io.shiftValue               := GCU.io.shiftValue
  io.llrRIsLastCol            := GCU.io.llrRIsLastCol
  io.llrReadyToShiftIsLastCal := GCU.io.llrReadyToShiftIsLastCal
  io.c2vRamRdReq              := GCU.io.c2vRamRdReq
  io.v2cSignRamRdReq          := GCU.io.v2cSignRamRdReq
  io.v2cSignRamWrReq          := GCU.io.v2cSignRamWrReq
  io.v2cFifoIn                := GCU.io.v2cFifoIn
  io.vnuCoreEn                := GCU.io.vnuCoreEn
  io.vnuCoreCounter           := GCU.io.vnuCoreCounter
  io.vnuLayerCounter          := GCU.io.vnuLayerCounter
  io.decoupledFifoIn          := GCU.io.decoupledFifoIn
  io.decoupledFifoOut         := GCU.io.decoupledFifoOut
  io.v2cFifoOut               := GCU.io.v2cFifoOut
  io.cnuCoreEn                := GCU.io.cnuCoreEn
  io.cnuCoreCounter           := GCU.io.cnuCoreCounter
  io.cnuLayerCounter          := GCU.io.cnuLayerCounter
  io.reShiftValue             := GCU.io.reShiftValue
  io.llrWAddr                 := GCU.io.llrWAddr
}
