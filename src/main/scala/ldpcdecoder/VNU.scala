package ldpcdecoder

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import utility._

class VNUCoreInput (implicit p: Parameters) extends DecBundle{
    val en = Bool()
    val v2cSignOld = UInt(1.W)
    val c2vRowMsgOld = UInt(C2VRowMsgBits.W)
    val counter = UInt(log2Ceil(MaxDegreeOfCNU).W)
    val shiftedLLR = SInt(LLRBits.W)
}

class VNUCoreOutput (implicit p: Parameters) extends DecBundle{
    val v2cMsg = SInt((LLRBits + 1).W)
    val gsgn = UInt(1.W)
    val min0 = UInt(LLRBits.W)
    val min1 = UInt(LLRBits.W)
    val idx0 = UInt(log2Ceil(MaxDegreeOfCNU).W)
}

class VNUCoreIO(implicit p: Parameters) extends DecBundle{
    val in = Input(new VNUCoreInput)
    val out = Output(new VNUCoreOutput)
}

class VNUCore(implicit p: Parameters) extends DecModule {
    val io = IO(new VNUCoreIO())

    // Stage 0
    val gsgnOld  = io.in.c2vRowMsgOld(0).asUInt
    val min0Old = io.in.c2vRowMsgOld(5, 1).asUInt   // (LLRBits - 1) bits
    val min1Old = io.in.c2vRowMsgOld(10, 6).asUInt   // (LLRBits - 1) bits
    val idx0Old = io.in.c2vRowMsgOld(15, 11).asUInt

    val signMagCombinator = Module(new SignMagCmb(LLRBits - 1))
    signMagCombinator.io.en := io.in.en
    signMagCombinator.io.sign := gsgnOld ^ io.in.v2cSignOld
    signMagCombinator.io.magnitude := Mux(idx0Old === io.in.counter, min1Old, min0Old)
    
    val c2vMsgPrevIter = Wire(SInt(LLRBits.W))
    c2vMsgPrevIter := signMagCombinator.io.out

    val v2cMsg = Wire(SInt((LLRBits + 1).W))
    v2cMsg := io.in.shiftedLLR -& c2vMsgPrevIter // keep the carry bit
    io.out.v2cMsg := v2cMsg

    // printf(p"shiftedLLR: ${io.in.shiftedLLR}, c2vMsgPrevIter: ${c2vMsgPrevIter}, v2cMsg: ${v2cMsg}(${Binary(v2cMsg.asUInt)}), v2cMsg.valid: ${io.in.en} \n")

    val v2cMsgReg = RegEnable(v2cMsg, 0.S((LLRBits + 1).W), io.in.en)

    // Stage 1
    val gsgn = RegInit(0.U(1.W))
    val min0 = RegInit(Fill(LLRBits, 1.U(1.W)))
    val min1 = RegInit(Fill(LLRBits, 1.U(1.W))) 
    val idx0 = RegInit(0.U(log2Ceil(MaxDegreeOfCNU).W))

    when(io.in.en && io.in.counter === 0.U){
        gsgn := 0.U
        idx0 := 0.U
        min0 := Fill(LLRBits, 1.U(1.W))
        min1 := Fill(LLRBits, 1.U(1.W))
    }

    val enDelayed = RegNext(io.in.en, init = false.B)

    val signMagSeperator = Module(new SignMagSep(LLRBits))
    signMagSeperator.io.en := enDelayed
    signMagSeperator.io.in := v2cMsgReg

    val sign = signMagSeperator.io.sign 
    val magnitude = signMagSeperator.io.magnitude 

    // printf(p"en_delay: ${signMagSeperator.io.en}, in: ${signMagSeperator.io.in}, sign: ${sign}, magnitude: ${magnitude} \n")

    when(enDelayed) {
        gsgn := gsgn ^ sign
        when(magnitude < min0) {
            min1 := min0
            min0 := magnitude
            idx0 := RegNext(io.in.counter, init = 0.U)
        }.elsewhen(magnitude < min1) {
            min1 := magnitude
        }
    }

    io.out.gsgn := gsgn
    io.out.min0 := min0
    io.out.min1 := min1
    io.out.idx0 := idx0

}

class C2VMsgInfo (implicit p: Parameters) extends DecBundle {
    val gsgn = UInt(1.W)
    val min0 = UInt(LLRBits.W)
    val min1 = UInt(LLRBits.W)
    val idx0 = UInt(log2Ceil(MaxDegreeOfCNU).W)
}

class VNUsInput (implicit p: Parameters) extends DecBundle {
    val en = Bool()
    val zSize = UInt(log2Ceil(MaxZSize).W)
    val v2cSignOld = Vec(MaxZSize, UInt(1.W))
    val c2vRowMsgOld = Vec(MaxZSize, UInt(C2VRowMsgBits.W))
    val counter = UInt(log2Ceil(MaxDegreeOfCNU).W)
    val shiftedLLR = Vec(MaxZSize, UInt(LLRBits.W))
}

class VNUsOutput (implicit p: Parameters) extends DecBundle{
    val v2cMsg = ValidIO(Vec(MaxZSize, SInt((LLRBits + 1).W)))
    val c2vRowMsg = Vec(MaxZSize, new C2VMsgInfo)
}

class VNUsIO(implicit p: Parameters) extends DecBundle{
    val in = Input(new VNUsInput)
    val out = Output(new VNUsOutput)
}

class VNUs(implicit p: Parameters) extends DecModule {
    val io = IO(new VNUsIO())

    val vnuCores = Seq.fill(MaxZSize)(Module(new VNUCore))

    val Zc1H = UIntToOH(io.in.zSize, MaxZSize)

    vnuCores.zipWithIndex.foreach {
        case (core, i) => 
            core.io.in.en           := Zc1H(i) && io.in.en
            core.io.in.v2cSignOld   := io.in.v2cSignOld(i)
            core.io.in.c2vRowMsgOld := io.in.c2vRowMsgOld(i)
            core.io.in.counter      := io.in.counter
            core.io.in.shiftedLLR   := io.in.shiftedLLR(i).asSInt

            io.out.v2cMsg.bits(i) := core.io.out.v2cMsg
            io.out.c2vRowMsg(i).gsgn := core.io.out.gsgn
            io.out.c2vRowMsg(i).min0 := core.io.out.min0
            io.out.c2vRowMsg(i).min1 := core.io.out.min1
            io.out.c2vRowMsg(i).idx0 := core.io.out.idx0
    }

    io.out.v2cMsg.valid := io.in.en
}