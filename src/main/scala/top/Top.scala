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

  class LLRAddrGeneratorImp(wrapper: LDPCDecoderTop) extends LazyModuleImp(wrapper) {
    val io = IO(new Bundle{
        val llrRAddr = ValidIO(UInt(log2Ceil(ColNum).W))
        val shiftValue = ValidIO(UInt(log2Ceil(MaxZSize).W))
        val isLastCol = Output(Bool())
        val c2vRamRLayer = ValidIO(UInt(log2Ceil(LayerNum).W))
        val v2cFifoIn = Output(Bool())
        val vnuCoreEn = Output(Bool())
        val vnuCoreCounter = Output(UInt(log2Ceil(MaxDegreeOfCNU).W))
        val v2cFifoOut = Output(Bool())
        val cnuCoreEn = Output(Bool())
        val cnuCoreCounter = Output(UInt(log2Ceil(MaxDegreeOfCNU).W))
        val reShiftValue = ValidIO(UInt(log2Ceil(MaxZSize).W))
        val llrWAddr = ValidIO(UInt(log2Ceil(ColNum).W))
    })

    // VNU Logic
    val colScoreBoard = RegInit(VecInit(Seq.fill(ColNum)(true.B)))

    val llrAddrGenerator = Module(new LLRAddrGenerator)
    llrAddrGenerator.io.ren := colScoreBoard(llrAddrGenerator.io.llrRAddr)

    io.llrRAddr.valid := colScoreBoard(llrAddrGenerator.io.llrRAddr)
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

    val vnuCoreBegin = DelayN(io.llrRAddr.valid, DelayOfShifter + DelayOfVNU - 1)
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
    when(vnuLastColDone){
        vnuLayerCounter := vnuLayerCounter + 1.U
        vnuLayerScoreBoard(vnuLayerCounter) := true.B
    }

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

    io.reShiftValue.valid := DelayN(cnuCoreBegin, DelayOfCNU)
    shiftValueGenerator.io.reShiftEn := io.reShiftValue.valid
    io.reShiftValue.bits := shiftValueGenerator.io.reShiftValue

    val reShifterDone = DelayN(cnuCoreBegin, DelayOfCNU + DelayOfShifter)

    llrAddrGenerator.io.wen := reShifterDone
    io.llrWAddr.valid := reShifterDone
    io.llrWAddr.bits := llrAddrGenerator.io.llrWAddr

    when(io.llrWAddr.valid){ // llr wen
        colScoreBoard(io.llrWAddr.bits) := true.B
    }
  }

  // lazy val module = new LDPCDecoderTopImp(this)
  lazy val module = new LLRAddrGeneratorImp(this)
}

object TopMain extends App {

    val (config, firrtlOpts, firtoolOpts) = ArgParser.parse(args)

    val decoder = DisableMonitors(p => LazyModule(new LDPCDecoderTop()(p)))(config)

    (new ChiselStage).execute(firrtlOpts, Seq(ChiselGeneratorAnnotation(() => decoder.module)))
}