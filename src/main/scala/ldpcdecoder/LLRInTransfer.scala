/***************************************************************************************
* Copyright (c) 2020-2021 Institute of Computing Technology, Chinese Academy of Sciences
* Copyright (c) 2020-2021 Peng Cheng Laboratory
*
* XiangShan is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

package ldpcdecoder

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import top._
import utility._

class LLRInTransferReq(implicit p: Parameters) extends DecBundle {
  val isBG1    = Bool()
  val zSize    = UInt(log2Ceil(MaxZSize).W)
  val llrBlock = UInt(LLRFromWidth.W) // 256 / 8 = 32 llr(8.W)
}

class LLRInTransfer(implicit p: Parameters) extends DecModule {
  val io = IO(new Bundle {
    val in  = Flipped(DecoupledIO(new LLRInTransferReq))
    val out = DecoupledIO(new LDPCDecoderReq)
  })

  val shiftCounter = RegInit(0.U(log2Ceil(MaxZSize * 8 / LLRFromWidth).W)) // TODO: add comment

  // =====================State Machine=====================
  val m_idle :: m_in :: m_out :: Nil = Enum(3)

  val state = RegInit(m_idle)
  val next_state = WireDefault(state)
  dontTouch(state)
  dontTouch(next_state)
  state := next_state

  switch(state) {
    is(m_idle) {
      when(io.in.fire) {
        next_state := m_in
      }
    }
    is(m_in) {
      when(shiftCounter === ((MaxZSize * 8 / LLRFromWidth) - 1).U) {
        next_state := m_out
      }
    }
    is(m_out) {
      when(io.out.fire) {
        next_state := m_idle
      }
    }
  }

  io.in.ready := Mux(state === m_out, false.B, true.B)
  io.out.valid := Mux(state === m_out, true.B, false.B)

  io.out.bits.isBG1 := io.in.bits.isBG1
  io.out.bits.zSize := io.in.bits.zSize

  when(next_state =/= m_in) {
    shiftCounter := 0.U
  }.elsewhen(io.in.fire) { // next_state === m_in
    shiftCounter := shiftCounter + 1.U
  }

  val rawLLR_slice = Wire(Vec(LLRFromWidth / 8, UInt(LLRBits.W)))
  for (ii <- 0 until (LLRFromWidth / 8)) {
    rawLLR_slice(ii) := io.in.bits.llrBlock(ii * 8 + LLRBits - 1, ii * 8)
  }
  

  val rawLLR_in_1ZcGroup =
    Reg(Vec(MaxZSize * 8 / LLRFromWidth, UInt((LLRFromWidth / 8 * LLRBits).W))) // (256 / 8) llr(LLRBits.W)
  when(io.in.fire) {
    rawLLR_in_1ZcGroup(shiftCounter) := rawLLR_slice.asUInt
  }

  // 将 rawLLR_in_1ZcGroup 的每个 192 位元素拆分并分配到 rawLLR 中
  for (i <- 0 until (MaxZSize * 8 / LLRFromWidth)) { // 处理 rawLLR_in_1ZcGroup 的每个元素
    for (j <- 0 until (LLRFromWidth / 8)) { // 192 位拆分成 32 个 6 位段
      // 将 rawLLR_in_1ZcGroup(i) 的 j*6 到 (j+1)*6-1 位赋值给 rawLLR(i*32 + j)
      io.out.bits.rawLLR(i * (LLRFromWidth / 8) + j) := rawLLR_in_1ZcGroup(i)(j * LLRBits + LLRBits - 1, j * LLRBits)
    }
  }

}
