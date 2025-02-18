package ldpcdecoder

import chisel3._
import chisel3.util._
import ldpcdecoder._
import org.chipsalliance.cde.config.Parameters

class CyclicShifterInput(implicit p: Parameters) extends DecBundle {
  val llr       = Vec(MaxZSize, UInt(LLRBits.W))
  val zSize     = UInt(log2Ceil(MaxZSize).W)
  val iLS       = UInt(log2Ceil(8).W)
  val zPow      = UInt(log2Ceil(8).W)
  val shiftSize = UInt(log2Ceil(MaxZSize).W)
}

class CyclicShifterIO(implicit p: Parameters) extends DecBundle {
  val in  = Flipped(DecoupledIO(new CyclicShifterInput))
  val out = DecoupledIO(Vec(MaxZSize, UInt(LLRBits.W)))
}

class CyclicShifter(val shiftLeft: Boolean = true)(implicit p: Parameters) extends DecModule {
  val io = IO(new CyclicShifterIO())

  val QSNs = Seq.fill(LLRBits)(Module(new QSN(shiftLeft)))

  val shiftValueModZc = Module(new ShiftValueModZc)
  shiftValueModZc.io.s := io.in.bits.shiftSize
  shiftValueModZc.io.iLS := io.in.bits.iLS
  shiftValueModZc.io.zPow := io.in.bits.zPow

  // val shiftSize = io.in.bits.shiftSize % io.in.zSize// ensure actual shiftSize <= zSize
  val shiftSize = shiftValueModZc.io.out

  for (i <- 0 until LLRBits) {
    QSNs(i).io.in.valid := io.in.fire

    // VecInit(Seq(0.U, 1.U, 1.U, 0.U, 1.U)).asUInt => 22
    QSNs(i).io.in.bits.srcData := VecInit(
      io.in.bits.llr.map(_(i))
    ).asUInt // UInt: {llr(383)(0), ..., llr(1)(0), llr(0)(0)}
    QSNs(i).io.in.bits.zSize     := io.in.bits.zSize
    QSNs(i).io.in.bits.shiftSize := shiftSize
  }

  io.out.valid := QSNs(0).io.out.valid
  io.in.ready  := true.B

  io.out.bits.zipWithIndex.foreach { case (shiftedLLR, i) =>
    shiftedLLR := VecInit(QSNs.map(_.io.out.bits(i))).asUInt
  }

}
