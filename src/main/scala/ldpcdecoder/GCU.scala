package ldpcdecoder

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import utility._

class LLRAddrGenerator(implicit p: Parameters) extends DecModule {
  val io = IO(new Bundle {
    val ren        = Input(Bool())
    val llrRAddr   = Output(UInt(log2Ceil(ColNum).W))
    val isLastCol  = Output(Bool())
    val isFirstCol = Output(Bool())

    val wen      = Input(Bool())
    val llrWAddr = Output(UInt(log2Ceil(ColNum).W))
  })

  val colIdxVec     = VecInit(ColIdxOrder.map(_.U))
  val isLastColVec  = VecInit(IsLastCol.map(_.B))
  val isFirstColVec = VecInit(IsFirstCol.map(_.B))
  assert(colIdxVec.length == isLastColVec.length)

  val rcounter = RegInit(0.U(log2Ceil(ColIdxOrder.length).W))
  when(io.ren === 1.U) {
    when(rcounter === (ColIdxOrder.length - 1).U) {
      rcounter := 0.U
    }.otherwise {
      rcounter := rcounter + 1.U
    }
  }
  io.llrRAddr   := colIdxVec(rcounter)
  io.isLastCol  := isLastColVec(rcounter)
  io.isFirstCol := isFirstColVec(rcounter)

  val wcounter = RegInit(0.U(log2Ceil(ColIdxOrder.length).W))
  when(io.wen === 1.U) {
    when(wcounter === (ColIdxOrder.length - 1).U) {
      wcounter := 0.U
    }.otherwise {
      wcounter := wcounter + 1.U
    }
  }
  io.llrWAddr := colIdxVec(wcounter)
}

class ShiftValueGenerator(implicit p: Parameters) extends DecModule {
  val io = IO(new Bundle {
    val shiftEn    = Input(Bool())
    val shiftValue = Output(UInt(log2Ceil(MaxZSize).W))

    val reShiftEn    = Input(Bool())
    val reShiftValue = Output(UInt(log2Ceil(MaxZSize).W))
  })
  val shiftValueVec = VecInit(ShiftValue.map(_.U))

  val shiftCounter = RegInit(0.U(log2Ceil(ShiftValue.length).W))
  when(io.shiftEn === 1.U) {
    when(shiftCounter === (ShiftValue.length - 1).U) {
      shiftCounter := 0.U
    }.otherwise {
      shiftCounter := shiftCounter + 1.U
    }
  }
  io.shiftValue := shiftValueVec(shiftCounter)

  val reShiftCounter = RegInit(0.U(log2Ceil(ShiftValue.length).W))
  when(io.reShiftEn === 1.U) {
    when(reShiftCounter === (ShiftValue.length - 1).U) {
      reShiftCounter := 0.U
    }.otherwise {
      reShiftCounter := reShiftCounter + 1.U
    }
  }
  io.reShiftValue := shiftValueVec(reShiftCounter)
}

class GCU(implicit p: Parameters) extends DecModule {
  val io = IO(new Bundle {
    val llrInitDone       = Input(Bool())
    val llrRAddr          = ValidIO(UInt(log2Ceil(ColNum).W))
    val shiftValue        = ValidIO(UInt(log2Ceil(MaxZSize).W))
    val llrRIsLastCol     = Output(Bool())
    val llrReadyIsLastCal = Output(Bool())
    val c2vRamRdReq       = ValidIO(UInt(log2Ceil(LayerNum).W))
    val v2cSignRamRdReq   = ValidIO(UInt(log2Ceil(MaxEdgeNum).W))
    val v2cSignRamWrReq   = ValidIO(UInt(log2Ceil(MaxEdgeNum).W))
    val v2cFifoIn         = Output(Bool())
    val vnuCoreEn         = Output(Bool())
    val vnuCoreCounter    = Output(UInt(log2Ceil(MaxDegreeOfCNU).W))
    val vnuLayerCounter   = Output(UInt(log2Ceil(LayerNum).W))
    val v2cFifoOut        = Output(Bool())
    val decoupledFifoIn   = Output(Bool())
    val decoupledFifoOut  = Output(Bool())
    val cnuCoreEn         = Output(Bool())
    val cnuCoreCounter    = Output(UInt(log2Ceil(MaxDegreeOfCNU).W))
    val cnuLayerCounter   = Output(UInt(log2Ceil(LayerNum).W))
    val reShiftValue      = ValidIO(UInt(log2Ceil(MaxZSize).W))
    val llrWAddr          = ValidIO(UInt(log2Ceil(ColNum).W))
  })

  val cnuLastLayerDone = RegInit(false.B)
  val vnuLastLayerDone = RegInit(false.B)

  // VNU Logic
  val colScoreBoard = RegInit(VecInit(Seq.fill(ColNum)(true.B)))

  val llrAddrGenerator = Module(new LLRAddrGenerator)

  io.llrRAddr.valid := llrAddrGenerator.io.ren
  io.llrRAddr.bits  := llrAddrGenerator.io.llrRAddr
  io.llrRIsLastCol  := llrAddrGenerator.io.isLastCol

  val llrReady          = DelayN(io.llrRAddr.valid, 1) // read LLR need 1 cycle
  val llrReadyIsLastCal = DelayN(io.llrRIsLastCol, 1)
  io.llrReadyIsLastCal := llrReadyIsLastCal

  val shiftValueGenerator = Module(new ShiftValueGenerator)
  shiftValueGenerator.io.shiftEn := llrReady

  io.shiftValue.valid := llrReady
  io.shiftValue.bits  := shiftValueGenerator.io.shiftValue

  when(io.llrRAddr.valid) { // llr ren
    colScoreBoard(io.llrRAddr.bits) := false.B
  }

  val delay2LastCol = DelayN(llrReadyIsLastCal, 2)
  val delay3LastCol = DelayN(llrReadyIsLastCal, 3) // LastCol value shift done, vnuCoreEnLastCol

  val c2vRamRLayer = RegInit(0.U(log2Ceil(LayerNum).W))
  when(io.c2vRamRdReq.valid) {
    c2vRamRLayer := Mux(c2vRamRLayer === (LayerNum - 1).U, 0.U, c2vRamRLayer + 1.U)
  }

  io.c2vRamRdReq.valid := llrAddrGenerator.io.isFirstCol && llrAddrGenerator.io.ren
  io.c2vRamRdReq.bits  := c2vRamRLayer

  val vnuCoreBegin   = DelayN(llrReady, DelayOfShifter)
  val vnuCoreDone    = DelayN(vnuCoreBegin, DelayOfVNU - 1)
  val vnuCoreCounter = RegInit(0.U(log2Ceil(MaxDegreeOfCNU).W))
  when(vnuCoreBegin) {
    when(delay3LastCol) {
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
    when(v2cSignRamRcounter + 1.U === EdgeNum.U) {
      v2cSignRamRcounter := 0.U
    }.otherwise {
      v2cSignRamRcounter := v2cSignRamRcounter + 1.U
    }
  }
  io.v2cSignRamRdReq.valid := v2cSignRamRen
  io.v2cSignRamRdReq.bits  := v2cSignRamRcounter

  val vnuLastColStage0 = delay3LastCol
  val vnuLastColStage1 = DelayN(delay3LastCol, DelayOfVNU - 1).suggestName("vnuLastColStage1")
  val vnuLastColStage2 = DelayN(delay3LastCol, DelayOfVNU).suggestName("vnuLastColStage2")

  val vnuLayerCounter    = RegInit(0.U(log2Ceil(LayerNum).W))
  val vnuLayerScoreBoard = RegInit(VecInit(Seq.fill(LayerNum)(false.B)))
  when(vnuLastColStage1 && vnuLayerCounter === LayerNum.U) {
    vnuLastLayerDone := true.B
  }
  when(delay3LastCol && !vnuLastLayerDone) {
    vnuLayerCounter                     := vnuLayerCounter + 1.U
    vnuLayerScoreBoard(vnuLayerCounter) := true.B
  }
  io.decoupledFifoIn := vnuLastColStage1
  io.vnuLayerCounter := vnuLayerCounter

  // CNU Logic
  val numAtLayer = VecInit(NumAtLayer.map(_.U))

  val cnuLayerCounter = RegInit(0.U(log2Ceil(LayerNum).W))
  val cnuCoreCounter  = RegInit(0.U(log2Ceil(MaxDegreeOfCNU).W))

  val cnuCoreBegin = RegInit(false.B)
  val cnuCoreDone  = DelayN(cnuCoreBegin, DelayOfCNU)
  when(vnuLayerScoreBoard(cnuLayerCounter) && !cnuCoreBegin && !cnuLastLayerDone) {
    cnuCoreBegin := true.B
  }.elsewhen(cnuCoreBegin && cnuCoreCounter === numAtLayer(cnuLayerCounter) - 1.U) {
    cnuCoreBegin := false.B
  }

  val v2cSignRamWen      = cnuCoreBegin
  val v2cSignRamWcounter = RegInit(0.U(log2Ceil(MaxEdgeNum).W))
  when(v2cSignRamWen) {
    when(v2cSignRamWcounter + 1.U === EdgeNum.U) {
      v2cSignRamWcounter := 0.U
    }.otherwise {
      v2cSignRamWcounter := v2cSignRamWcounter + 1.U
    }
  }
  io.v2cSignRamWrReq.valid := v2cSignRamWen
  io.v2cSignRamWrReq.bits  := v2cSignRamWcounter

  when(cnuCoreBegin && !cnuLastLayerDone) {
    when(cnuCoreCounter === numAtLayer(cnuLayerCounter) - 1.U) {
      // when(cnuLayerCounter + 1.U === LayerNum.U){
      //     cnuLastLayerDone := true.B
      // }
      cnuLayerCounter := cnuLayerCounter + 1.U
      cnuCoreCounter  := 0.U
    }.otherwise {
      cnuCoreCounter := cnuCoreCounter + 1.U
    }
  }

  val cnuLastColStage1 = RegInit(false.B) // cnu LastCol calc done
  when(cnuLastColStage1) {
    cnuLastColStage1 := false.B
  }.elsewhen(cnuCoreBegin && cnuCoreCounter === numAtLayer(cnuLayerCounter) - 1.U) {
    cnuLastColStage1 := true.B
  }

  when(cnuLastColStage1 && cnuLayerCounter === LayerNum.U) {
    cnuLastLayerDone := true.B
  }

  io.decoupledFifoOut := cnuCoreCounter === numAtLayer(cnuLayerCounter) - 1.U && !cnuLastLayerDone

  io.v2cFifoOut      := cnuCoreBegin
  io.cnuCoreEn       := cnuCoreBegin
  io.cnuCoreCounter  := cnuCoreCounter
  io.cnuLayerCounter := cnuLayerCounter

  io.reShiftValue.valid            := DelayN(cnuCoreBegin, DelayOfCNU)
  shiftValueGenerator.io.reShiftEn := io.reShiftValue.valid
  io.reShiftValue.bits             := shiftValueGenerator.io.reShiftValue

  val reShifterDone = DelayN(cnuCoreBegin, DelayOfCNU + DelayOfShifter)

  val llrLastLayerWriteDone = DelayN(cnuLastLayerDone, 1 + DelayOfShifter)
  llrAddrGenerator.io.wen := reShifterDone && !llrLastLayerWriteDone
  io.llrWAddr.valid       := llrAddrGenerator.io.wen
  io.llrWAddr.bits        := llrAddrGenerator.io.llrWAddr

  when(io.llrWAddr.valid) { // llr wen
    colScoreBoard(io.llrWAddr.bits) := true.B
  }

  llrAddrGenerator.io.ren := io.llrInitDone && colScoreBoard(
    llrAddrGenerator.io.llrRAddr
  ) && !vnuLastLayerDone && !cnuLastLayerDone && !(vnuLayerCounter === (LayerNum - 1).U && delay3LastCol)

}
