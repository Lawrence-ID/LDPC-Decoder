package utility

import chisel3._
import chisel3.util._

class MaskGenerator(Length: Int) extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(log2Ceil(Length).W))
    val out = Output(Vec(Length, Bool()))
  })

  io.out := MaskGenerator.apply(io.in, Length)
}


object MaskGenerator {
  def apply(in: UInt, Length: Int): Vec[Bool] = {
    val mask = Wire(UInt(Length.W))
    mask := (1.U << in) - 1.U

    VecInit(mask.asBools)
  }
}