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

// class LDPCDecoderResp(implicit p: Parameters) extends DecBundle {
//   val idx        = UInt(log2Ceil(MaxZSize).W)
//   val last       = Bool()
//   val decodedLLR = Vec(MaxZSize, UInt(LLRBits.W))
// }

class LLROutTransferReq(implicit p: Parameters) extends DecBundle {
  val idx         = UInt(log2Ceil(MaxZSize).W)
  val last        = Bool()
  val decodedBits = UInt(MaxZSize.W)
}

class LLROutTransfer(implicit p: Parameters) extends DecModule {
  val io = IO(new Bundle {
    val in  = Flipped(DecoupledIO(new LDPCDecoderResp))
    val out = DecoupledIO(new LLROutTransferReq)
  })

  io.out.valid := io.in.valid
  io.in.ready  := io.out.ready

  io.out.bits.decodedBits := io.in.bits.decodedLLR.map(llr => llr(LLRBits - 1)).foldRight(0.U) { (bit, acc) =>
    Cat(acc, bit)
  }
  io.out.bits.idx  := io.in.bits.idx
  io.out.bits.last := io.in.bits.last

}
