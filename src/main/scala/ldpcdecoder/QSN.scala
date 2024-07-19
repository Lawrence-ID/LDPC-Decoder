package ldpcdecoder

import chisel3._
import ldpcdecoder._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters

abstract class DecModule(implicit val p: Parameters) extends Module with HasDecParameter

class QSN(val shiftLeft: Boolean = true)(implicit p: Parameters) extends DecModule{
    val io = IO(new Bundle{
      val in = Flipped(Decoupled(UInt(MaxZSize.W)))
      val p = Input(UInt(log2Ceil(MaxZSize).W))
      val c = Input(UInt(log2Ceil(MaxZSize).W))
      val out = Decoupled(UInt(MaxZSize.W))
    })
  io.in.ready := io.out.ready
  io.out.valid := io.in.valid
  io.out.bits := 0.U

  when(io.in.valid && io.out.ready) {
    val mask = (1.U << io.p) - 1.U
    val maskedInput = io.in.bits & mask

    val shifted = if (shiftLeft) {
      (maskedInput << io.c) | (maskedInput >> (io.p - io.c))
    } else {
      (maskedInput >> io.c) | (maskedInput << (io.p - io.c))
    }

    io.out.bits := (io.in.bits & ~mask) | (shifted & mask)
  }
}
