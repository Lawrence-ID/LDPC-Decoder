package ldpcdecoder

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import utility._

class CNUCoreInput(implicit p: Parameters) extends DecBundle {
  val en      = Bool()
  val counter = UInt(log2Ceil(MaxDegreeOfCNU).W)
  val v2cMsg  = SInt((LLRBits + 1).W)
  val gsgn    = UInt(1.W)
  val min0    = UInt(LLRBits.W)
  val min1    = UInt(LLRBits.W)
  val idx0    = UInt(log2Ceil(MaxDegreeOfCNU).W)
}

class CNUCoreOutput(implicit p: Parameters) extends DecBundle {
  val v2cMsgSign   = UInt(1.W)
  val c2vMsg       = UInt(C2VRowMsgBits.W)
  val unshiftedLLR = SInt(LLRBits.W)
}

class CNUCoreIO(implicit p: Parameters) extends DecBundle {
  val in  = Input(new CNUCoreInput)
  val out = Output(new CNUCoreOutput)
}

class CNUCore(implicit p: Parameters) extends DecModule {
  val io = IO(new CNUCoreIO())

  // Stage 0
  val satMin0 = UnsignedSaturator(io.in.min0, LLRBits, LLRBits - 3)
  val satMin1 = UnsignedSaturator(io.in.min1, LLRBits, LLRBits - 3)

  io.out.c2vMsg := Cat(io.in.idx0, satMin1, satMin0, io.in.gsgn)

  val v2cMsgSign = io.in.v2cMsg(LLRBits + 1 - 1)
  io.out.v2cMsgSign := v2cMsgSign

  val signMagCombinator = Module(new SignMagCmb(LLRBits))
  signMagCombinator.io.en        := io.in.en
  signMagCombinator.io.sign      := io.in.gsgn ^ v2cMsgSign
  signMagCombinator.io.magnitude := Mux(io.in.idx0 === io.in.counter, io.in.min1, io.in.min0)

  val c2vMsgReg = RegEnable(signMagCombinator.io.out, io.in.en)
  val v2cMsgReg = RegEnable(io.in.v2cMsg, io.in.en)

  // Stage 1
  val enDelayed = RegNext(io.in.en, init = false.B)

  val LLR = Wire(SInt((LLRBits + 2).W))
  LLR := c2vMsgReg +& v2cMsgReg

  val satLLR    = SignedSaturator(LLR, LLRBits + 2, LLRBits)
  val satLLRReg = RegEnable(satLLR, enDelayed)

  io.out.unshiftedLLR := satLLRReg

}

class CNUsInput(implicit p: Parameters) extends DecBundle {
  val en      = Bool()
  val zSize   = UInt(log2Ceil(MaxZSize).W)
  val counter = UInt(log2Ceil(MaxDegreeOfCNU).W)
  val v2cMsg  = Vec(MaxZSize, SInt((LLRBits + 1).W))
  // val gsgn = Vec(MaxZSize, UInt(1.W))
  // val min0 = Vec(MaxZSize, UInt(LLRBits.W))
  // val min1 = Vec(MaxZSize, UInt(LLRBits.W))
  // val idx0 = Vec(MaxZSize, UInt(log2Ceil(MaxDegreeOfCNU).W))
  val c2vRowMsg = Vec(MaxZSize, new C2VMsgInfo)
}

class CNUsOutput(implicit p: Parameters) extends DecBundle {
  val v2cMsgSign   = Valid(Vec(MaxZSize, UInt(1.W)))
  val c2vMsg       = Valid(Vec(MaxZSize, UInt(C2VRowMsgBits.W)))
  val unshiftedLLR = Vec(MaxZSize, UInt(LLRBits.W))
}

class CNUsIO(implicit p: Parameters) extends DecBundle {
  val in  = Input(new CNUsInput)
  val out = Output(new CNUsOutput)
}

class CNUs(implicit p: Parameters) extends DecModule {
  val io = IO(new CNUsIO())

  val cnuCores = Seq.fill(MaxZSize)(Module(new CNUCore))

  val bitMask = (1.U << io.in.zSize) - 1.U

  cnuCores.zipWithIndex.foreach {
    case (core, i) =>
      core.io.in.en      := bitMask(i) && io.in.en
      core.io.in.counter := io.in.counter
      core.io.in.v2cMsg  := io.in.v2cMsg(i)
      core.io.in.gsgn    := io.in.c2vRowMsg(i).gsgn
      core.io.in.min0    := io.in.c2vRowMsg(i).min0
      core.io.in.min1    := io.in.c2vRowMsg(i).min1
      core.io.in.idx0    := io.in.c2vRowMsg(i).idx0

      io.out.v2cMsgSign.bits(i) := core.io.out.v2cMsgSign
      io.out.c2vMsg.bits(i)     := core.io.out.c2vMsg
      io.out.unshiftedLLR(i)    := core.io.out.unshiftedLLR.asUInt
  }

  io.out.c2vMsg.valid     := io.in.en
  io.out.v2cMsgSign.valid := io.in.en
}
