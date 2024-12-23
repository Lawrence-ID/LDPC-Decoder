package ldpcdecoder

import chisel3._
import ldpcdecoder._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters

class QSNInput(implicit p: Parameters) extends DecBundle{
  val srcData = UInt(MaxZSize.W)
  val zSize = UInt(log2Ceil(MaxZSize).W)
  val shiftSize = UInt(log2Ceil(MaxZSize).W) // need to ensure shiftSize <= zSize
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

  // stage 0
  val s0_valid = io.in.valid
  val s0_zSize = io.in.bits.zSize
  val s0_srcData = io.in.bits.srcData
  val s0_shiftSize = io.in.bits.shiftSize

  val s0_mask = WireInit(0.U(MaxZSize.W))
  s0_mask := (1.U << s0_zSize) - 1.U
  val s0_maskedInput = s0_srcData & s0_mask

  // stage 1
  val s1_valid = RegNext(s0_valid, false.B)
  val s1_maskedInput = RegEnable(s0_maskedInput, 0.U, s0_valid)
  val s1_shiftSize = RegEnable(s0_shiftSize, 0.U, s0_valid)
  val s1_zSize = RegEnable(s0_zSize, 0.U, s0_valid)
  val s1_srcData = RegEnable(s0_srcData, 0.U, s0_valid)
  val s1_mask = RegEnable(s0_mask, 0.U, s0_valid)

  val s1_result = WireInit(0.U(MaxZSize.W))
  s1_result := (if (shiftLeft) {
    (s1_maskedInput >> s1_shiftSize) | (s1_maskedInput << (s1_zSize - s1_shiftSize))
  } else {
    (s1_maskedInput << s1_shiftSize) | (s1_maskedInput >> (s1_zSize - s1_shiftSize))
  })

  // stage 2
  val s2_valid = RegNext(s1_valid, false.B)
  val s2_srcData = RegEnable(s1_srcData, 0.U, s1_valid)
  val s2_mask = RegEnable(s1_mask, 0.U, s1_valid)
  val s2_shifted = RegEnable(s1_result, 0.U, s1_valid)

  val s2_result = WireInit(0.U(MaxZSize.W))
  s2_result := (s2_srcData & ~s2_mask) | (s2_shifted & s2_mask)

  // stage 3 (Output valid)
  val s3_valid = RegNext(s2_valid, false.B)
  val s3_result = RegEnable(s2_result, 0.U, s2_valid)

  io.out.bits := s3_result
  io.out.valid := s3_valid

}
