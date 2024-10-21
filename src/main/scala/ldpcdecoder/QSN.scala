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

  // 三个寄存器用于延迟输出
  val delayedOut1 = RegInit(0.U(MaxZSize.W))
  val delayedOut2 = RegInit(0.U(MaxZSize.W))
  val delayedOut3 = RegInit(0.U(MaxZSize.W))

  // 三个寄存器用于延迟有效信号
  val validDelay1 = RegInit(false.B)
  val validDelay2 = RegInit(false.B)
  val validDelay3 = RegInit(false.B)

  when(io.in.valid) {
    val zSize = io.in.bits.zSize
    val srcData = io.in.bits.srcData
    val shiftSize = io.in.bits.shiftSize

    val mask = (1.U << zSize) - 1.U
    val maskedInput = srcData & mask

    val shifted = if (shiftLeft) {
      (maskedInput >> shiftSize) | (maskedInput << (zSize - shiftSize))
    } else {
      (maskedInput << shiftSize) | (maskedInput >> (zSize - shiftSize))
    }

    // 更新延迟寄存器
    delayedOut1 := (srcData & ~mask) | (shifted & mask) // 计算当前输出
    delayedOut2 := delayedOut1                           // 第一拍延迟
    delayedOut3 := delayedOut2                           // 第二拍延迟

    // 更新有效信号
    validDelay1 := true.B
    validDelay2 := validDelay1
    validDelay3 := validDelay2
  }.otherwise {
    // 当输入无效时，保持有效信号为假
    validDelay1 := false.B
    validDelay2 := false.B
    validDelay3 := false.B
  }

  io.out.bits := delayedOut3             // 输出最后的延迟值
  io.out.valid := validDelay3            // 输出延迟的有效信号
}

