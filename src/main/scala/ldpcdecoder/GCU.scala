package ldpcdecoder

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import utility._

class LLRAddrGenerator(implicit p: Parameters) extends DecModule {
    val io = IO(new Bundle {
        val ren = Input(Bool())
        val llrRAddr = Output(UInt(log2Ceil(ColNum).W))
        val isLastCol = Output(Bool())

        val wen = Input(Bool())
        val llrWAddr = Output(UInt(log2Ceil(ColNum).W))
    })

    val colIdxVec = VecInit(ColIdxOrder.map(_.U))
    val isLastColVec = VecInit(IsLastCol.map(_.B))
    assert(colIdxVec.length == isLastColVec.length)

    val rcounter = RegInit(0.U(log2Ceil(ColIdxOrder.length).W))
    when(io.ren === 1.U) {
        when(rcounter === (ColIdxOrder.length - 1).U) {
            rcounter := 0.U
        } .otherwise {
            rcounter := rcounter + 1.U
        }
    }
    io.llrRAddr := colIdxVec(rcounter)
    io.isLastCol := isLastColVec(rcounter)

    val wcounter = RegInit(0.U(log2Ceil(ColIdxOrder.length).W))
    when(io.wen === 1.U) {
        when(wcounter === (ColIdxOrder.length - 1).U) {
            wcounter := 0.U
        } .otherwise {
            wcounter := wcounter + 1.U
        }
    }
    io.llrWAddr := colIdxVec(wcounter)
}

class ShiftValueGenerator(implicit p: Parameters) extends DecModule{
    val io = IO(new Bundle {
        val shiftEn = Input(Bool())
        val shiftValue = Output(UInt(log2Ceil(MaxZSize).W))

        val reShiftEn = Input(Bool())
        val reShiftValue = Output(UInt(log2Ceil(MaxZSize).W))
    })
    val shiftValueVec = VecInit(ShiftValue.map(_.U))

    val shiftCounter = RegInit(0.U(log2Ceil(ShiftValue.length).W))
    when(io.shiftEn === 1.U) {
        when(shiftCounter === (ShiftValue.length - 1).U) {
            shiftCounter := 0.U
        } .otherwise {
            shiftCounter := shiftCounter + 1.U
        }
    }
    io.shiftValue := shiftValueVec(shiftCounter)

    val reShiftCounter = RegInit(0.U(log2Ceil(ShiftValue.length).W))
    when(io.reShiftEn === 1.U) {
        when(reShiftCounter === (ShiftValue.length - 1).U) {
            reShiftCounter := 0.U
        } .otherwise {
            reShiftCounter := reShiftCounter + 1.U
        }
    }
    io.reShiftValue := shiftValueVec(reShiftCounter)
}

class GCU(implicit p: Parameters) extends DecModule{
    val io = IO(new Bundle{
        val llrRAddr = ValidIO(UInt(log2Ceil(ColNum).W))
        val shiftValue = ValidIO(UInt(log2Ceil(MaxZSize).W))
        val isLastCol = Output(Bool())
        val c2vRamRLayer = ValidIO(UInt(log2Ceil(LayerNum).W))
        val v2cFifoIn = Output(Bool())
        val vnuCoreEn = Output(Bool())
        val vnuCoreCounter = Output(UInt(log2Ceil(MaxDegreeOfCNU).W))
        val v2cFifoOut = Output(Bool())
        val decoupledFifoIn = Output(Bool())
        val decoupledFifoOut = Output(Bool())
        val cnuCoreEn = Output(Bool())
        val cnuCoreCounter = Output(UInt(log2Ceil(MaxDegreeOfCNU).W))
        val reShiftValue = ValidIO(UInt(log2Ceil(MaxZSize).W))
        val llrWAddr = ValidIO(UInt(log2Ceil(ColNum).W))
    })

    val cnuLastLayerDone = RegInit(false.B)
    val vnuLastLayerDone = RegInit(false.B)

    // VNU Logic
    val colScoreBoard = RegInit(VecInit(Seq.fill(ColNum)(true.B)))

    val llrAddrGenerator = Module(new LLRAddrGenerator)
    llrAddrGenerator.io.ren := colScoreBoard(llrAddrGenerator.io.llrRAddr) && !vnuLastLayerDone && !cnuLastLayerDone

    io.llrRAddr.valid := llrAddrGenerator.io.ren
    io.llrRAddr.bits := llrAddrGenerator.io.llrRAddr
    io.isLastCol := llrAddrGenerator.io.isLastCol

    val shiftValueGenerator = Module(new ShiftValueGenerator)
    shiftValueGenerator.io.shiftEn := io.llrRAddr.valid

    io.shiftValue.valid := io.llrRAddr.valid
    io.shiftValue.bits  := shiftValueGenerator.io.shiftValue

    when(io.llrRAddr.valid){ // llr ren
        colScoreBoard(io.llrRAddr.bits) := false.B
    }

    val delay2LastCol = DelayN(io.isLastCol, 2)
    val delay3LastCol = DelayN(io.isLastCol, 3)

    val c2vRamRLayer = RegInit(0.U(log2Ceil(LayerNum).W))
    when(c2vRamRLayer === (LayerNum - 1).U){
        c2vRamRLayer := 0.U
    }.otherwise{
        c2vRamRLayer := RegEnable(c2vRamRLayer + 1.U, delay2LastCol)
    }

    io.c2vRamRLayer.valid := DelayN(io.llrRAddr.valid, DelayOfShifter - 1)
    io.c2vRamRLayer.bits := c2vRamRLayer

    val vnuCoreBegin = DelayN(io.llrRAddr.valid, DelayOfShifter)
    val vnuCoreDone = DelayN(vnuCoreBegin, DelayOfVNU - 1)
    val vnuCoreCounter = RegInit(0.U(log2Ceil(MaxDegreeOfCNU).W))
    when(vnuCoreBegin){
        when(delay3LastCol){
            vnuCoreCounter := 0.U
        }.otherwise{
            vnuCoreCounter := vnuCoreCounter + 1.U
        }
    }
    io.v2cFifoIn := vnuCoreBegin
    io.vnuCoreEn := vnuCoreBegin
    io.vnuCoreCounter := vnuCoreCounter

    val vnuLastColDone = DelayN(delay3LastCol, DelayOfVNU - 1)

    val vnuLayerCounter = RegInit(0.U(log2Ceil(LayerNum).W))
    val vnuLayerScoreBoard = RegInit(VecInit(Seq.fill(LayerNum)(false.B)))
    when(delay3LastCol && vnuLayerCounter + 1.U === LayerNum.U){
        vnuLastLayerDone := true.B
    }
    when(delay3LastCol && !vnuLastLayerDone){
        vnuLayerCounter := vnuLayerCounter + 1.U
        vnuLayerScoreBoard(vnuLayerCounter) := true.B
    }
    io.decoupledFifoIn := vnuLastColDone

    // CNU Logic
    val numAtLayer = VecInit(NumAtLayer.map(_.U))

    val cnuLayerCounter = RegInit(0.U(log2Ceil(LayerNum).W))
    val cnuCoreCounter = RegInit(0.U(log2Ceil(MaxDegreeOfCNU).W))

    val cnuCoreBegin = RegInit(false.B)
    val cnuCoreDone = DelayN(cnuCoreBegin, DelayOfCNU)
    when(vnuLayerScoreBoard(cnuLayerCounter) && !cnuCoreBegin){
        cnuCoreBegin := true.B
    }.elsewhen(cnuCoreBegin && cnuCoreCounter === numAtLayer(cnuLayerCounter) - 1.U){
        cnuCoreBegin := false.B
    }

    when(cnuCoreBegin && !cnuLastLayerDone){
        when(cnuCoreCounter === numAtLayer(cnuLayerCounter) - 1.U){
            when(cnuLayerCounter + 1.U === LayerNum.U){
                cnuLastLayerDone := true.B
            }
            cnuLayerCounter := cnuLayerCounter + 1.U
            cnuCoreCounter := 0.U
        }.otherwise{
            cnuCoreCounter := cnuCoreCounter + 1.U
        }
    }

    io.decoupledFifoOut := cnuCoreCounter === numAtLayer(cnuLayerCounter) - 1.U && !cnuLastLayerDone

    io.v2cFifoOut := cnuCoreBegin
    io.cnuCoreEn := cnuCoreBegin
    io.cnuCoreCounter := cnuCoreCounter

    io.reShiftValue.valid := DelayN(cnuCoreBegin, DelayOfCNU)
    shiftValueGenerator.io.reShiftEn := io.reShiftValue.valid
    io.reShiftValue.bits := shiftValueGenerator.io.reShiftValue

    val reShifterDone = DelayN(cnuCoreBegin, DelayOfCNU + DelayOfShifter)

    llrAddrGenerator.io.wen := reShifterDone && !cnuLastLayerDone
    io.llrWAddr.valid := llrAddrGenerator.io.wen
    io.llrWAddr.bits := llrAddrGenerator.io.llrWAddr

    when(io.llrWAddr.valid){ // llr wen
        colScoreBoard(io.llrWAddr.bits) := true.B
    }

}