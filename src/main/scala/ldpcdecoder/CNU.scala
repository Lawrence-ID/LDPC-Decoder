package ldpcdecoder

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import utility._

class CNUCoreInput (implicit p: Parameters) extends DecBundle{
    val en = Bool()
    val counter = UInt(log2Ceil(MaxDegreeOfCNU).W)
    val v2cMsg = ValidIO(SInt((LLRBits + 1).W))
    val gsgn = UInt(1.W)
    val min0 = UInt(LLRBits.W)
    val min1 = UInt(LLRBits.W)
    val idx0 = UInt(log2Ceil(MaxDegreeOfCNU).W)
}

class CNUCoreOutput (implicit p: Parameters) extends DecBundle{
    val c2vMsg = ValidIO(UInt(C2VRowMsgBits.W))
    val unshiftedLLR = SInt(LLRBits.W)
}

class CNUCoreIO(implicit p: Parameters) extends DecBundle{
    val in = Input(new CNUCoreInput)
    val out = Output(new CNUCoreOutput)
}

class CNUCore(implicit p: Parameters) extends DecModule {
    val io = IO(new CNUCoreIO())

    // Stage 0
    val satMin0 = UnsignedSaturator(io.in.min0, LLRBits, LLRBits - 1)
    val satMin1 = UnsignedSaturator(io.in.min1, LLRBits, LLRBits - 1)

    io.out.c2vMsg.valid := io.in.en
    io.out.c2vMsg.bits := Cat(io.in.idx0, satMin1, satMin0, io.in.gsgn)

    val signMagCombinator = Module(new SignMagCmb(LLRBits))
    signMagCombinator.io.en := io.in.en
    signMagCombinator.io.sign := io.in.gsgn ^ io.in.v2cMsg.bits(LLRBits + 1 - 1)
    signMagCombinator.io.magnitude := Mux(io.in.idx0 === io.in.counter, io.in.min1, io.in.min0)

    val c2vMsgReg = RegEnable(signMagCombinator.io.out, io.in.en)
    val v2cMsgReg = RegEnable(io.in.v2cMsg.bits, io.in.en)

    // Stage 1
    val enDelayed = RegNext(io.in.en, init = false.B)

    val LLR = Wire(SInt((LLRBits + 2).W))
    LLR := c2vMsgReg +& v2cMsgReg

    val satLLR = SignedSaturator(LLR, LLRBits + 2, LLRBits)
    val satLLRReg = RegEnable(satLLR, enDelayed)

    io.out.unshiftedLLR := satLLRReg

}