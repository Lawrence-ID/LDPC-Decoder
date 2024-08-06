package ldpcdecoder

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import _root_.circt.stage.ChiselStage
import top.DefaultConfig
import ldpcdecoder.DecParamsKey
import java.lang.reflect.Parameter
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

class GCU(implicit p: Parameters) extends DecModule{
    val io = IO(new Bundle{
        val llrRAddr = ValidIO(UInt(log2Ceil(ColNum).W))
        val isLastCol = Output(Bool())
        val c2vRamRLayer = ValidIO(UInt(log2Ceil(LayerNum).W))
        val v2cFifoIn = Output(Bool())
        val vnuCoreEn = Output(Bool())
        val vnuCoreCounter = Output(UInt(log2Ceil(ColNum).W))
        val v2cFifoOut = Output(Bool())
        val cnuCoreEn = Output(Bool())
        val cnuCoreCounter = Output(UInt(log2Ceil(ColNum).W))
        val reShifterEn = Output(Bool())
        val llrWAddr = ValidIO(UInt(log2Ceil(ColNum).W))
    })

    // VNU Logic
    val colScoreBoard = RegInit(VecInit(Seq.fill(ColNum)(true.B)))

    val llrAddrGenerator = Module(new LLRAddrGenerator)
    llrAddrGenerator.io.ren := colScoreBoard(llrAddrGenerator.io.llrRAddr)

    io.llrRAddr.valid := colScoreBoard(llrAddrGenerator.io.llrRAddr)
    io.llrRAddr.bits := llrAddrGenerator.io.llrRAddr
    io.isLastCol := llrAddrGenerator.io.isLastCol

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

    val vnuCoreBegin = DelayN(io.llrRAddr.valid, DelayOfShifter + DelayOfVNU - 1)
    val vnuCoreDone = DelayN(vnuCoreBegin, DelayOfVNU - 1)
    val vnuCoreCounter = RegInit(0.U(log2Ceil(ColNum).W))
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
    when(vnuLastColDone){
        vnuLayerCounter := vnuLayerCounter + 1.U
        vnuLayerScoreBoard(vnuLayerCounter) := true.B
    }

    // CNU Logic
    val numAtLayer = VecInit(NumAtLayer.map(_.U))

    val cnuLayerCounter = RegInit(0.U(log2Ceil(LayerNum).W))
    val cnuCoreCounter = RegInit(0.U(log2Ceil(ColNum).W))

    val cnuCoreBegin = RegInit(false.B)
    val cnuCoreDone = DelayN(cnuCoreBegin, DelayOfCNU)
    when(vnuLayerScoreBoard(cnuLayerCounter) && !cnuCoreBegin){
        cnuCoreBegin := true.B
    }.elsewhen(cnuCoreBegin && cnuCoreCounter === numAtLayer(cnuLayerCounter) - 1.U){
        cnuCoreBegin := false.B
    }

    when(cnuCoreBegin){
        when(cnuCoreCounter === numAtLayer(cnuLayerCounter) - 1.U){
            cnuLayerCounter := cnuLayerCounter + 1.U
            cnuCoreCounter := 0.U
        }.otherwise{
            cnuCoreCounter := cnuCoreCounter + 1.U
        }
    }

    io.v2cFifoOut := cnuCoreBegin
    io.cnuCoreEn := cnuCoreBegin
    io.cnuCoreCounter := cnuCoreCounter

    io.reShifterEn := DelayN(cnuCoreBegin, DelayOfCNU)
    val reShifterDone = DelayN(cnuCoreBegin, DelayOfCNU + DelayOfShifter)

    llrAddrGenerator.io.wen := reShifterDone
    io.llrWAddr.valid := reShifterDone
    io.llrWAddr.bits := llrAddrGenerator.io.llrWAddr

    when(io.llrWAddr.valid){ // llr wen
        colScoreBoard(io.llrWAddr.bits) := true.B
    }

}