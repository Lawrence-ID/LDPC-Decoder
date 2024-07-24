package ldpcdecoder

import chisel3._
import ldpcdecoder._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters

class QSNInput(implicit p: Parameters) extends DecBundle{
  val srcData = UInt(MaxZSize.W)
  val zSize = UInt(log2Ceil(MaxZSize).W)
  val shiftSize = UInt(log2Ceil(MaxZSize).W)
}

/* 
 * Accacually we need to cyclic shift Seq(in[0], in[1], in[2], ..., in[MaxZSize-1])
 * Notice: the direction `shiftLeft` or `!shiftLeft` is intended for Seq(in[0], in[1], in[2], ..., in[MaxZSize-1])
 * For example, we need to cyclic Shift Left the Seq(in[0], in[1], in[2], ..., in[MaxZSize-1]), zSize = 5, shiftSize = 3
 * It's equal to cyclic Shift Right the low 5 bits of UInt[MaxZSize:0] for 3 times.
 */

class QSN(val shiftLeft: Boolean = true)(implicit p: Parameters) extends DecModule {
  val io = IO(new Bundle {
    val in = Flipped(ValidIO(new QSNInput))
    val out = ValidIO(UInt(MaxZSize.W))
  })

  io.out.valid := io.in.valid
  io.out.bits := 0.U

  val zSize = io.in.bits.zSize
  val srcData = io.in.bits.srcData
  val shiftSize = io.in.bits.shiftSize

  when(io.in.valid) {
    val mask = (1.U << zSize) - 1.U
    val maskedInput = srcData & mask

    val shifted = if (shiftLeft) {
      (maskedInput >> shiftSize) | (maskedInput << (zSize - shiftSize))
    } else {
      (maskedInput << shiftSize) | (maskedInput >> (zSize - shiftSize))
    }

    io.out.bits := (srcData & ~mask) | (shifted & mask)
  }
}
