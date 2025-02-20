package ldpcdecoder

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import utility._

class LLRAddrGenerator(implicit p: Parameters) extends DecModule {
  val io = IO(new Bundle {
    val isBG1              = Input(Bool())
    val llrRAddrGenCounter = Input(UInt(log2Ceil(MaxColIdxVecLen).W))
    val llrRAddr           = Output(UInt(log2Ceil(MaxColNum).W))
    val isLastCol          = Output(Bool())
    val isFirstCol         = Output(Bool())

    val llrWAddrGenCounter = Input(UInt(log2Ceil(MaxColIdxVecLen).W))
    val llrWAddr           = Output(UInt(log2Ceil(MaxColNum).W))
  })

  val isBG1 = io.isBG1

  assert(BG1ColIdx.length > BG2ColIdx.length)

  val BG2ColIdxPadded = BG2ColIdx.padTo(BG1ColIdx.length, 0)
  val colIdxVec = Mux(
    isBG1,
    VecInit(BG1ColIdx.map(_.U(log2Ceil(MaxColNum).W))),
    VecInit(BG2ColIdxPadded.map(_.U(log2Ceil(MaxColNum).W)))
  )

  val BG2IsLastColPadded = BG2IsLastCol.padTo(BG1IsLastCol.length, false)
  val isLastColVec       = Mux(isBG1, VecInit(BG1IsLastCol.map(_.B)), VecInit(BG2IsLastColPadded.map(_.B)))

  val BG2IsFirstColPadded = BG2IsFirstCol.padTo(BG1IsFirstCol.length, false)
  val isFirstColVec       = Mux(isBG1, VecInit(BG1IsFirstCol.map(_.B)), VecInit(BG2IsFirstColPadded.map(_.B)))

  assert(colIdxVec.length == isLastColVec.length)

  io.llrRAddr   := colIdxVec(io.llrRAddrGenCounter)
  io.isLastCol  := isLastColVec(io.llrRAddrGenCounter)
  io.isFirstCol := isFirstColVec(io.llrRAddrGenCounter)
  io.llrWAddr   := colIdxVec(io.llrWAddrGenCounter)
}

class ShiftValueGenerator(implicit p: Parameters) extends DecModule {
  val io = IO(new Bundle {
    val isBG1 = Input(Bool())
    val iLS   = Input(UInt(log2Ceil(8).W))
    val zPow  = Input(UInt(log2Ceil(8).W))
    val zSize = Input(UInt(log2Ceil(MaxZSize).W))

    val shiftValueRen = Input(Bool())
    val shiftValue    = Output(UInt(log2Ceil(MaxZSize).W))

    val reShiftValueRen = Input(Bool())
    val reShiftValue    = Output(UInt(log2Ceil(MaxZSize).W))
  })
  val isBG1 = io.isBG1
  val zSize = io.zSize
  val iLS   = io.iLS
  val zPow  = io.zPow
  assert(BG1ShiftValue.length > BG2ShiftValue.length)
  val BG2ShiftValuePadded = BG2ShiftValue.padTo(BG1ShiftValue.length, 0)
  val shiftValueVec = Mux(
    isBG1,
    VecInit(BG1ShiftValue.map(_.U(log2Ceil(MaxZSize).W))),
    VecInit(BG2ShiftValuePadded.map(_.U(log2Ceil(MaxZSize).W)))
  )
  val shiftValueVecLen = Mux(isBG1, BG1ShiftValue.length.U, BG2ShiftValue.length.U)

  val shiftCounter = RegInit(0.U(log2Ceil(MaxShiftValueVecLen).W))
  when(io.shiftValueRen === 1.U) {
    when(shiftCounter === shiftValueVecLen - 1.U) {
      shiftCounter := 0.U
    }.otherwise {
      shiftCounter := shiftCounter + 1.U
    }
  }

  val shiftValueInHBG = RegEnable(shiftValueVec(shiftCounter), io.shiftValueRen)

  val reShiftCounter = RegInit(0.U(log2Ceil(MaxShiftValueVecLen).W))
  when(io.reShiftValueRen === 1.U) {
    when(reShiftCounter === shiftValueVecLen - 1.U) {
      reShiftCounter := 0.U
    }.otherwise {
      reShiftCounter := reShiftCounter + 1.U
    }
  }
  val reShiftValueInHBG = RegEnable(shiftValueVec(reShiftCounter), io.reShiftValueRen)

  // actual shift value = shiftValueInHBG % zSize
  io.shiftValue   := shiftValueInHBG
  io.reShiftValue := reShiftValueInHBG
}

class GCU(implicit p: Parameters) extends DecModule {
  val io = IO(new Bundle {
    val gcuEn = Input(Bool())
    val isBG1 = Input(Bool())
    val iLS   = Input(UInt(log2Ceil(8).W))
    val zPow  = Input(UInt(log2Ceil(8).W))
    val zSize = Input(UInt(log2Ceil(MaxZSize).W))

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
    val vnuIterCounter           = Output(UInt(log2Ceil(MaxIterNum).W))
    val v2cFifoOut               = Output(Bool())
    val decoupledFifoIn          = Output(Bool())
    val decoupledFifoOut         = Output(Bool())
    val cnuCoreEn                = Output(Bool())
    val cnuCoreCounter           = Output(UInt(log2Ceil(MaxDegreeOfCNU).W))
    val cnuLayerCounter          = Output(UInt(log2Ceil(MaxLayerNum).W))
    val cnuIterCounter           = Output(UInt(log2Ceil(MaxIterNum).W))
    val reShiftValue             = ValidIO(UInt(log2Ceil(MaxZSize).W))
    val llrWAddr                 = ValidIO(UInt(log2Ceil(MaxColNum).W))
    val decodeMaxIterDone        = Output(Bool())
  })
  val llrRamRen           = WireInit(false.B)
  val llrRamWen           = WireInit(false.B)
  val isBG1               = io.isBG1
  val zSize               = io.zSize
  val iLS                 = io.iLS
  val zPow                = io.zPow
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

  val decodeMaxIterDone = WireInit(false.B)

  // llrAddrGenerator Logic
  val llrAddrGenerator       = Module(new LLRAddrGenerator)
  val llrRAddrGenCounter     = RegInit(0.U(log2Ceil(MaxColIdxVecLen).W))
  val llrWAddrGenCounter     = RegInit(0.U(log2Ceil(MaxColIdxVecLen).W))
  val llrRAddrGenIterCounter = RegInit(0.U(log2Ceil(MaxIterNum).W))
  val llrRAddrGenDone        = RegInit(false.B)

  llrAddrGenerator.io.isBG1              := isBG1
  llrAddrGenerator.io.llrRAddrGenCounter := llrRAddrGenCounter
  llrAddrGenerator.io.llrWAddrGenCounter := llrWAddrGenCounter

  when(llrRamRen) {
    when(llrRAddrGenCounter === edgeNum - 1.U) {
      llrRAddrGenCounter := 0.U
      when(llrRAddrGenIterCounter === (MaxIterNum - 1).U) {
        llrRAddrGenIterCounter := 0.U
      }.otherwise {
        llrRAddrGenIterCounter := llrRAddrGenIterCounter + 1.U
      }
    }.otherwise {
      llrRAddrGenCounter := llrRAddrGenCounter + 1.U
    }
  }

  when(decodeMaxIterDone) {
    llrRAddrGenDone := false.B
  }.elsewhen(
    llrRamRen && llrRAddrGenCounter === edgeNum - 1.U && llrRAddrGenIterCounter === (MaxIterNum - 1).U
  ) {
    llrRAddrGenDone := true.B
  }

  when(llrRamWen) {
    when(llrWAddrGenCounter === edgeNum - 1.U) {
      llrWAddrGenCounter := 0.U
    }.otherwise {
      llrWAddrGenCounter := llrWAddrGenCounter + 1.U
    }
  }

  io.llrRAddr.valid := llrRamRen
  io.llrRAddr.bits  := llrAddrGenerator.io.llrRAddr
  io.llrRIsLastCol  := llrAddrGenerator.io.isLastCol
  io.llrWAddr.valid := llrRamWen
  io.llrWAddr.bits  := llrAddrGenerator.io.llrWAddr

  // VNU Logic
  val colScoreBoard = RegInit(VecInit(Seq.fill(MaxColNum)(true.B)))
  when(llrRamRen) {
    colScoreBoard(llrAddrGenerator.io.llrRAddr) := false.B
  }
  when(llrRamWen) {
    colScoreBoard(llrAddrGenerator.io.llrWAddr) := true.B
  }

  val llrReadyToShift          = DelayN(llrRamRen, 1) // read LLR need 1 cycle
  val llrReadyToShiftIsLastCal = DelayN(llrAddrGenerator.io.isLastCol, 1)
  io.llrReadyToShiftIsLastCal := llrReadyToShiftIsLastCal

  val shiftValueRen       = llrRamRen
  val shiftValueGenerator = Module(new ShiftValueGenerator)
  shiftValueGenerator.io.shiftValueRen := shiftValueRen
  shiftValueGenerator.io.isBG1         := isBG1
  shiftValueGenerator.io.zSize         := zSize
  shiftValueGenerator.io.iLS           := iLS
  shiftValueGenerator.io.zPow          := zPow

  io.shiftValue.valid := llrReadyToShift
  io.shiftValue.bits  := shiftValueGenerator.io.shiftValue

  val delay2LastCol = DelayN(llrReadyToShiftIsLastCal, 2)
  val delay3LastCol = DelayN(llrReadyToShiftIsLastCal, 3) // LastCol value shift done, vnuCoreEnLastCol

  val vnuLastColStage0 = delay3LastCol
  val vnuLastColStage1 = DelayN(delay3LastCol, DelayOfVNU - 1).suggestName("vnuLastColStage1")
  val vnuLastColStage2 = DelayN(delay3LastCol, DelayOfVNU).suggestName("vnuLastColStage2")

  val vnuCoreBegin   = DelayN(llrReadyToShift, DelayOfShifter)
  val vnuCoreDone    = DelayN(vnuCoreBegin, DelayOfVNU - 1)
  val vnuCoreCounter = RegInit(0.U(log2Ceil(MaxDegreeOfCNU).W))

  val vnuLayerCounter = RegInit(0.U(log2Ceil(MaxLayerNum).W))
  val vnuIterCounter  = RegInit(0.U(log2Ceil(MaxIterNum).W))
  val layerScoreBoard = RegInit(VecInit(Seq.fill(MaxLayerNum)(false.B)))

  val c2vRamRLayer = RegInit(0.U(log2Ceil(MaxLayerNum).W))
  when(io.c2vRamRdReq.valid) {
    c2vRamRLayer := Mux(c2vRamRLayer === layerNum - 1.U, 0.U, c2vRamRLayer + 1.U)
  }
  io.c2vRamRdReq.valid := DelayN(
    llrAddrGenerator.io.isFirstCol && llrRamRen,
    DelayOfShifter
  ) && vnuIterCounter =/= 0.U
  io.c2vRamRdReq.bits := c2vRamRLayer

  when(vnuCoreBegin) {
    when(vnuLastColStage0) {
      vnuCoreCounter := 0.U
    }.elsewhen(!vnuLastIterDone) {
      vnuCoreCounter := vnuCoreCounter + 1.U
    }
  }
  io.v2cFifoIn      := vnuCoreBegin
  io.vnuCoreEn      := vnuCoreBegin
  io.vnuCoreCounter := vnuCoreCounter

  val v2cSignRamRen =
    DelayN(llrReadyToShift, DelayOfShifter - 1) && vnuIterCounter =/= 0.U // sign ram ren before vnuBegin
  val v2cSignRamRcounter = RegInit(0.U(log2Ceil(MaxEdgeNum).W))
  when(v2cSignRamRen) {
    when(v2cSignRamRcounter + 1.U === edgeNum) {
      v2cSignRamRcounter := 0.U
    }.elsewhen(!vnuLastIterDone) {
      v2cSignRamRcounter := v2cSignRamRcounter + 1.U
    }
  }
  io.v2cSignRamRdReq.valid := v2cSignRamRen
  io.v2cSignRamRdReq.bits  := v2cSignRamRcounter

  val vnuLastIterLastLayerLastColStage0 =
    vnuLayerCounter === layerNum - 1.U && vnuIterCounter === (MaxIterNum - 1).U && vnuCoreCounter === numAtLayer(
      vnuLayerCounter
    ) - 1.U
  when(decodeMaxIterDone) {
    vnuLastIterDone := false.B
  }.elsewhen(vnuLastIterLastLayerLastColStage0) {
    vnuLastIterDone := true.B
  }

  when(vnuLastColStage0 && !vnuLastIterDone) {
    when(vnuLayerCounter === layerNum - 1.U) {
      vnuLayerCounter := 0.U
      when(vnuIterCounter === (MaxIterNum - 1).U) {
        vnuIterCounter := 0.U
      }.elsewhen(vnuLayerCounter === layerNum - 1.U) {
        vnuIterCounter := vnuIterCounter + 1.U
      }
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
        when(cnuIterCounter === (MaxIterNum - 1).U) {
          cnuIterCounter := 0.U
        }.otherwise {
          cnuIterCounter := cnuIterCounter + 1.U
        }
      }.otherwise {
        cnuLayerCounter := cnuLayerCounter + 1.U
      }
      cnuCoreCounter := 0.U
    }.elsewhen(!cnuLastIterDone) {
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

  when(decodeMaxIterDone) {
    cnuLastIterDone := false.B
  }.elsewhen(cnuLayerCounter === layerNum - 1.U && cnuIterCounter === (MaxIterNum - 1).U &&
    cnuCoreCounter === numAtLayer(cnuLayerCounter) - 1.U) {
    cnuLastIterDone := true.B
  }

  io.decoupledFifoOut := cnuCoreCounter === numAtLayer(cnuLayerCounter) - 1.U && !cnuLastIterDone

  io.v2cFifoOut      := cnuCoreBegin
  io.cnuCoreEn       := cnuCoreBegin
  io.cnuCoreCounter  := cnuCoreCounter
  io.cnuLayerCounter := cnuLayerCounter
  io.cnuIterCounter  := cnuIterCounter

  io.reShiftValue.valid                  := DelayN(cnuCoreBegin, DelayOfCNU)
  shiftValueGenerator.io.reShiftValueRen := DelayN(cnuCoreBegin, DelayOfCNU - 1)
  io.reShiftValue.bits                   := shiftValueGenerator.io.reShiftValue

  val reShifterDone = DelayN(cnuCoreBegin, DelayOfCNU + DelayOfShifter)

  val llrLastLayerWriteDone = DelayN(cnuLastIterDone, DelayOfCNU + DelayOfShifter)

  // LLR RAM Ren & Wen Control
  val llrNextColValid = colScoreBoard(llrAddrGenerator.io.llrRAddr)
  llrRamRen := io.gcuEn && llrNextColValid && !vnuLastIterDone && !cnuLastIterDone && !llrRAddrGenDone
  llrRamWen := reShifterDone && !llrLastLayerWriteDone

  decodeMaxIterDone := llrLastLayerWriteDone & !RegNext(llrLastLayerWriteDone)

  io.decodeMaxIterDone := decodeMaxIterDone

}
