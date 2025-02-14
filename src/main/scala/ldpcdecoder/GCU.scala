package ldpcdecoder

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import utility._

class LLRAddrGenerator(implicit p: Parameters) extends DecModule {
  val io = IO(new Bundle {
    val isBG1      = Input(Bool())
    val ren        = Input(Bool())
    val llrRAddr   = Output(UInt(log2Ceil(MaxColNum).W))
    val isLastCol  = Output(Bool())
    val isFirstCol = Output(Bool())

    val wen      = Input(Bool())
    val llrWAddr = Output(UInt(log2Ceil(MaxColNum).W))
  })

  val isBG1 = io.isBG1

  assert(BG1ColIdx.length > BG2ColIdx.length)

  val BG2ColIdxPadded = BG2ColIdx.padTo(BG1ColIdx.length, 0)
  val colIdxVec = Mux(
    isBG1,
    VecInit(BG1ColIdx.map(_.U(log2Ceil(MaxColNum).W))),
    VecInit(BG2ColIdxPadded.map(_.U(log2Ceil(MaxColNum).W)))
  )
  val colIdxVecLen = Mux(isBG1, BG1ColIdx.length.U, BG2ColIdx.length.U)

  val BG2IsLastColPadded = BG2IsLastCol.padTo(BG1IsLastCol.length, false)
  val isLastColVec       = Mux(isBG1, VecInit(BG1IsLastCol.map(_.B)), VecInit(BG2IsLastColPadded.map(_.B)))

  val BG2IsFirstColPadded = BG2IsFirstCol.padTo(BG1IsFirstCol.length, false)
  val isFirstColVec       = Mux(isBG1, VecInit(BG1IsFirstCol.map(_.B)), VecInit(BG2IsFirstColPadded.map(_.B)))

  assert(colIdxVec.length == isLastColVec.length)

  val rcounter = RegInit(0.U(log2Ceil(MaxColIdxVecLen).W))
  when(io.ren === 1.U) {
    when(rcounter === colIdxVecLen - 1.U) {
      rcounter := 0.U
    }.otherwise {
      rcounter := rcounter + 1.U
    }
  }
  io.llrRAddr   := colIdxVec(rcounter)
  io.isLastCol  := isLastColVec(rcounter)
  io.isFirstCol := isFirstColVec(rcounter)

  val wcounter = RegInit(0.U(log2Ceil(MaxColIdxVecLen).W))
  when(io.wen === 1.U) {
    when(wcounter === colIdxVecLen - 1.U) {
      wcounter := 0.U
    }.otherwise {
      wcounter := wcounter + 1.U
    }
  }
  io.llrWAddr := colIdxVec(wcounter)
}

class ShiftValueGenerator(implicit p: Parameters) extends DecModule {
  val io = IO(new Bundle {
    val isBG1      = Input(Bool())
    val shiftEn    = Input(Bool())
    val shiftValue = Output(UInt(log2Ceil(MaxZSize).W))

    val reShiftEn    = Input(Bool())
    val reShiftValue = Output(UInt(log2Ceil(MaxZSize).W))
  })
  val isBG1 = io.isBG1
  assert(BG1ShiftValue.length > BG2ShiftValue.length)
  val BG2ShiftValuePadded = BG2ShiftValue.padTo(BG1ShiftValue.length, 0)
  val shiftValueVec = Mux(
    isBG1,
    VecInit(BG1ShiftValue.map(_.U(log2Ceil(MaxZSize).W))),
    VecInit(BG2ShiftValuePadded.map(_.U(log2Ceil(MaxZSize).W)))
  )
  val shiftValueVecLen = Mux(isBG1, BG1ShiftValue.length.U, BG2ShiftValue.length.U)

  val shiftCounter = RegInit(0.U(log2Ceil(MaxShiftValueVecLen).W))
  when(io.shiftEn === 1.U) {
    when(shiftCounter === shiftValueVecLen - 1.U) {
      shiftCounter := 0.U
    }.otherwise {
      shiftCounter := shiftCounter + 1.U
    }
  }
  io.shiftValue := shiftValueVec(shiftCounter)

  val reShiftCounter = RegInit(0.U(log2Ceil(MaxShiftValueVecLen).W))
  when(io.reShiftEn === 1.U) {
    when(reShiftCounter === shiftValueVecLen - 1.U) {
      reShiftCounter := 0.U
    }.otherwise {
      reShiftCounter := reShiftCounter + 1.U
    }
  }
  io.reShiftValue := shiftValueVec(reShiftCounter)
}

class GCU(implicit p: Parameters) extends DecModule {
  val io = IO(new Bundle {
    val isBG1             = Input(Bool())
    val llrInValid        = Input(Bool())
    val llrRAddr          = ValidIO(UInt(log2Ceil(MaxColNum).W))
    val shiftValue        = ValidIO(UInt(log2Ceil(MaxZSize).W))
    val llrRIsLastCol     = Output(Bool())
    val llrReadyIsLastCal = Output(Bool())
    val c2vRamRdReq       = ValidIO(UInt(log2Ceil(MaxLayerNum).W))
    val v2cSignRamRdReq   = ValidIO(UInt(log2Ceil(MaxEdgeNum).W))
    val v2cSignRamWrReq   = ValidIO(UInt(log2Ceil(MaxEdgeNum).W))
    val v2cFifoIn         = Output(Bool())
    val vnuCoreEn         = Output(Bool())
    val vnuCoreCounter    = Output(UInt(log2Ceil(MaxDegreeOfCNU).W))
    val vnuLayerCounter   = Output(UInt(log2Ceil(MaxLayerNum).W))
    val vnuIterCounter    = Output(UInt(log2Ceil(MaxIterNum).W))
    val v2cFifoOut        = Output(Bool())
    val decoupledFifoIn   = Output(Bool())
    val decoupledFifoOut  = Output(Bool())
    val cnuCoreEn         = Output(Bool())
    val cnuCoreCounter    = Output(UInt(log2Ceil(MaxDegreeOfCNU).W))
    val cnuLayerCounter   = Output(UInt(log2Ceil(MaxLayerNum).W))
    val cnuIterCounter    = Output(UInt(log2Ceil(MaxIterNum).W))
    val reShiftValue      = ValidIO(UInt(log2Ceil(MaxZSize).W))
    val llrWAddr          = ValidIO(UInt(log2Ceil(MaxColNum).W))
    val llrIniting        = Output(Bool())
    // val llrDecodedWriteDone = Output(Bool())
  })

  // val m_idle :: m_llrInput :: m_decoding :: m_llrOutput :: Nil = Enum(4)
  // val state = RegInit(m_idle)
  // val next_state = WireDefault(state)
  // dontTouch(state)
  // dontTouch(next_state)
  // state := next_state

  // switch(state){
  //   is(m_idle){
  //     when(){
  //       next_state := m_llrInput
  //     }
  //   }
  //   is(m_llrInput){
  //     when(){
  //       next_state := m_decoding
  //     }
  //   }
  //   is(m_decoding){
  //     when(){
  //       next_state := m_llrOutput
  //     }
  //   }
  //   is(m_llrOutput){
  //     when(){
  //       next_state := m_idle
  //     }
  //   }
  // }

  val isBG1               = io.isBG1
  val BG2NumAtLayerPadded = BG2NumAtLayer.padTo(BG1NumAtLayer.length, 0)
  val numAtLayer = Mux(
    isBG1,
    VecInit(BG1NumAtLayer.map(n => n.U(log2Ceil(MaxLayerNum).W))),
    VecInit(BG2NumAtLayerPadded.map(n => n.U(log2Ceil(MaxLayerNum).W)))
  )
  val edgeNum  = Mux(isBG1, BG1ColIdx.length.U, BG2ColIdx.length.U)
  val layerNum = Mux(isBG1, BG1RowNum.U, BG2RowNum.U)

  val cnuLastLayerDone = RegInit(false.B)
  val vnuLastLayerDone = RegInit(false.B)
  val cnuLastIterDone  = RegInit(false.B)
  val vnuLastIterDone  = RegInit(false.B)

  // VNU Logic
  val colScoreBoard = RegInit(VecInit(Seq.fill(MaxColNum)(true.B)))

  val llrAddrGenerator = Module(new LLRAddrGenerator)
  llrAddrGenerator.io.isBG1 := isBG1

  io.llrRAddr.valid := llrAddrGenerator.io.ren
  io.llrRAddr.bits  := llrAddrGenerator.io.llrRAddr
  io.llrRIsLastCol  := llrAddrGenerator.io.isLastCol

  val llrReady          = DelayN(io.llrRAddr.valid, 1) // read LLR need 1 cycle
  val llrReadyIsLastCal = DelayN(io.llrRIsLastCol, 1)
  io.llrReadyIsLastCal := llrReadyIsLastCal

  val shiftValueGenerator = Module(new ShiftValueGenerator)
  shiftValueGenerator.io.shiftEn := llrReady
  shiftValueGenerator.io.isBG1   := isBG1

  io.shiftValue.valid := llrReady
  io.shiftValue.bits  := shiftValueGenerator.io.shiftValue

  when(io.llrRAddr.valid) { // llr ren
    colScoreBoard(io.llrRAddr.bits) := false.B
  }

  val delay2LastCol = DelayN(llrReadyIsLastCal, 2)
  val delay3LastCol = DelayN(llrReadyIsLastCal, 3) // LastCol value shift done, vnuCoreEnLastCol

  val vnuLastColStage0 = delay3LastCol
  val vnuLastColStage1 = DelayN(delay3LastCol, DelayOfVNU - 1).suggestName("vnuLastColStage1")
  val vnuLastColStage2 = DelayN(delay3LastCol, DelayOfVNU).suggestName("vnuLastColStage2")

  val c2vRamRLayer = RegInit(0.U(log2Ceil(MaxLayerNum).W))
  when(io.c2vRamRdReq.valid) {
    c2vRamRLayer := Mux(c2vRamRLayer === layerNum - 1.U, 0.U, c2vRamRLayer + 1.U)
  }

  io.c2vRamRdReq.valid := DelayN(llrAddrGenerator.io.isFirstCol && llrAddrGenerator.io.ren, DelayOfShifter)
  io.c2vRamRdReq.bits  := c2vRamRLayer

  val vnuCoreBegin   = DelayN(llrReady, DelayOfShifter)
  val vnuCoreDone    = DelayN(vnuCoreBegin, DelayOfVNU - 1)
  val vnuCoreCounter = RegInit(0.U(log2Ceil(MaxDegreeOfCNU).W))
  when(vnuCoreBegin) {
    when(vnuLastColStage0) {
      vnuCoreCounter := 0.U
    }.otherwise {
      vnuCoreCounter := vnuCoreCounter + 1.U
    }
  }
  io.v2cFifoIn      := vnuCoreBegin
  io.vnuCoreEn      := vnuCoreBegin
  io.vnuCoreCounter := vnuCoreCounter

  val v2cSignRamRen      = DelayN(llrReady, DelayOfShifter - 1) // sign ram ren before vnuBegin
  val v2cSignRamRcounter = RegInit(0.U(log2Ceil(MaxEdgeNum).W))
  when(v2cSignRamRen) {
    when(v2cSignRamRcounter + 1.U === edgeNum) {
      v2cSignRamRcounter := 0.U
    }.otherwise {
      v2cSignRamRcounter := v2cSignRamRcounter + 1.U
    }
  }
  io.v2cSignRamRdReq.valid := v2cSignRamRen
  io.v2cSignRamRdReq.bits  := v2cSignRamRcounter

  val vnuLayerCounter = RegInit(0.U(log2Ceil(MaxLayerNum).W))
  val vnuIterCounter  = RegInit(0.U(log2Ceil(MaxIterNum).W))
  val layerScoreBoard = RegInit(VecInit(Seq.fill(MaxLayerNum)(false.B)))
  when(vnuLayerCounter === layerNum - 1.U && vnuIterCounter === (MaxIterNum - 1).U &&
    vnuCoreCounter === numAtLayer(vnuLayerCounter) - 1.U) {
    vnuLastIterDone := true.B
  }
  when(vnuLastColStage0 && !vnuLastIterDone) {
    when(vnuLayerCounter === layerNum - 1.U) {
      vnuLayerCounter := 0.U
      vnuIterCounter  := vnuIterCounter + 1.U
    }.otherwise {
      vnuLayerCounter := vnuLayerCounter + 1.U
    }
  }

  io.decoupledFifoIn := vnuLastColStage1
  io.vnuLayerCounter := vnuLayerCounter
  io.vnuIterCounter  := vnuIterCounter

  // CNU Logic
  val cnuLayerCounter = RegInit(0.U(log2Ceil(MaxLayerNum).W))
  val cnuCoreCounter  = RegInit(0.U(log2Ceil(MaxDegreeOfCNU).W))
  val cnuIterCounter  = RegInit(0.U(log2Ceil(MaxIterNum).W))

  val cnuCoreBegin = RegInit(false.B)
  val cnuCoreDone  = DelayN(cnuCoreBegin, DelayOfCNU)
  when(layerScoreBoard(cnuLayerCounter) && !cnuCoreBegin && !cnuLastIterDone) {
    cnuCoreBegin := true.B
  }.elsewhen(cnuCoreBegin && cnuCoreCounter === numAtLayer(cnuLayerCounter) - 1.U) {
    cnuCoreBegin := false.B
  }

  val v2cSignRamWen      = cnuCoreBegin
  val v2cSignRamWcounter = RegInit(0.U(log2Ceil(MaxEdgeNum).W))
  when(v2cSignRamWen) {
    when(v2cSignRamWcounter + 1.U === edgeNum) {
      v2cSignRamWcounter := 0.U
    }.otherwise {
      v2cSignRamWcounter := v2cSignRamWcounter + 1.U
    }
  }
  io.v2cSignRamWrReq.valid := v2cSignRamWen
  io.v2cSignRamWrReq.bits  := v2cSignRamWcounter

  when(cnuCoreBegin && !cnuLastIterDone) {
    when(cnuCoreCounter === numAtLayer(cnuLayerCounter) - 1.U) {
      when(cnuLayerCounter === layerNum - 1.U) {
        cnuLayerCounter := 0.U
        cnuIterCounter  := cnuIterCounter + 1.U
      }.otherwise {
        cnuLayerCounter := cnuLayerCounter + 1.U
      }
      cnuCoreCounter := 0.U
    }.otherwise {
      cnuCoreCounter := cnuCoreCounter + 1.U
    }
  }

  when(cnuCoreBegin && !cnuLastIterDone && cnuCoreCounter === numAtLayer(cnuLayerCounter) - 1.U) {
    layerScoreBoard(cnuLayerCounter) := false.B
    when(vnuLastColStage0 && !vnuLastIterDone) {
      assert(
        vnuLayerCounter =/= cnuLayerCounter,
        "Assertion failed, vnuLayerCounter should not equal to cnuLayerCounter!"
      )
    }
  }

  when(vnuLastColStage0 && !vnuLastIterDone) {
    layerScoreBoard(vnuLayerCounter) := true.B
    when(cnuCoreBegin && !cnuLastIterDone && cnuCoreCounter === numAtLayer(cnuLayerCounter) - 1.U) {
      assert(
        vnuLayerCounter =/= cnuLayerCounter,
        "Assertion failed, vnuLayerCounter should not equal to cnuLayerCounter!"
      )
    }
  }

  val cnuLastColStage1 = RegInit(false.B) // cnu LastCol calc done
  when(cnuLastColStage1) {
    cnuLastColStage1 := false.B
  }.elsewhen(cnuCoreBegin && cnuCoreCounter === numAtLayer(cnuLayerCounter) - 1.U) {
    cnuLastColStage1 := true.B
  }

  when(cnuLayerCounter === layerNum - 1.U && cnuIterCounter === (MaxIterNum - 1).U &&
    cnuCoreCounter === numAtLayer(cnuLayerCounter) - 1.U) {
    cnuLastIterDone := true.B
  }

  io.decoupledFifoOut := cnuCoreCounter === numAtLayer(cnuLayerCounter) - 1.U && !cnuLastIterDone

  io.v2cFifoOut      := cnuCoreBegin
  io.cnuCoreEn       := cnuCoreBegin
  io.cnuCoreCounter  := cnuCoreCounter
  io.cnuLayerCounter := cnuLayerCounter
  io.cnuIterCounter  := cnuIterCounter

  io.reShiftValue.valid            := DelayN(cnuCoreBegin, DelayOfCNU)
  shiftValueGenerator.io.reShiftEn := io.reShiftValue.valid
  io.reShiftValue.bits             := shiftValueGenerator.io.reShiftValue

  val reShifterDone = DelayN(cnuCoreBegin, DelayOfCNU + DelayOfShifter)

  val llrLastLayerWriteDone = DelayN(cnuLastIterDone, DelayOfCNU + DelayOfShifter)
  llrAddrGenerator.io.wen := reShifterDone && !llrLastLayerWriteDone

  val llrInitDoneReg = RegInit(false.B)
  val llrInitCounter = RegInit(0.U(log2Ceil(MaxColNum).W))
  val colNum         = Mux(isBG1, BG1ColNum.U, BG2ColNum.U)
  when(llrInitCounter === colNum - 1.U) {
    llrInitDoneReg := true.B
  }
  when(io.llrInValid && !llrInitDoneReg) {
    llrInitCounter := llrInitCounter + 1.U
  }
  io.llrIniting     := io.llrInValid && !llrInitDoneReg
  io.llrWAddr.valid := llrAddrGenerator.io.wen || io.llrIniting
  io.llrWAddr.bits  := Mux(io.llrIniting, llrInitCounter, llrAddrGenerator.io.llrWAddr)

  when(io.llrWAddr.valid) { // llr wen
    colScoreBoard(io.llrWAddr.bits) := true.B
  }

  llrAddrGenerator.io.ren := llrInitDoneReg && colScoreBoard(
    llrAddrGenerator.io.llrRAddr
  ) && !vnuLastIterDone && !cnuLastIterDone

}
