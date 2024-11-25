package ldpcdecoder

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import utility._

class VNUCoreInput(implicit p: Parameters) extends DecBundle{
    val en = Bool()
    val c2vMsg = UInt(C2VMsgBits.W)
    val counter = UInt(log2Ceil(MaxDegreeOfCNU).W)
    val shiftedLLR = SInt(LLRBits.W)
}

class VNUCoreOutput(implicit p: Parameters) extends DecBundle{
    val v2cMsg = SInt((LLRBits + 1).W) // fifo data
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

    // First Stage
    val gsgnOld = io.in.c2vMsg(0).asUInt
    val min0Old = io.in.c2vMsg(5, 1).asUInt   // (LLRBits - 1) bits
    val min1Old = io.in.c2vMsg(10, 6).asUInt   // (LLRBits - 1) bits
    val idx0Old = io.in.c2vMsg(15, 11).asUInt

    val signMagCombinator = Module(new SignMagCmb(LLRBits - 1))
    signMagCombinator.io.en := io.in.en
    signMagCombinator.io.sign := gsgnOld
    signMagCombinator.io.magnitude := Mux(idx0Old === io.in.counter, min1Old, min0Old)
    
    val c2vMsgPrevIter = Wire(SInt(LLRBits.W))
    c2vMsgPrevIter := signMagCombinator.io.out

    val v2cMsg = Wire(SInt((LLRBits + 1).W))
    v2cMsg := io.in.shiftedLLR -& c2vMsgPrevIter // keep the carry bit
    io.out.v2cMsg := v2cMsg

    // printf(p"shiftedLLR: ${io.in.shiftedLLR}, c2vMsgPrevIter: ${c2vMsgPrevIter}, v2cMsg: ${v2cMsg}(${Binary(v2cMsg.asUInt)}), v2cMsg.valid: ${io.in.en} \n")

    // Second Stage
    val v2cMsgReg = RegEnable(v2cMsg, 0.S((LLRBits + 1).W), io.in.en)

    val gsgn = RegInit(0.U(1.W))
    val min0 = RegInit(Fill(LLRBits, 1.U(1.W)))
    val min1 = RegInit(Fill(LLRBits, 1.U(1.W))) 
    val idx0 = RegInit(0.U(log2Ceil(MaxDegreeOfCNU).W))

    val enDelayed = RegNext(io.in.en, init = false.B)

    val signMagSeperator = Module(new SignMagSep(LLRBits))
    signMagSeperator.io.en := enDelayed
    signMagSeperator.io.in := v2cMsgReg

    val sign = signMagSeperator.io.sign 
    val magnitude = signMagSeperator.io.magnitude 

    // printf(p"en_delay: ${signMagSeperator.io.en}, in: ${signMagSeperator.io.in}, sign: ${sign}, magnitude: ${magnitude} \n")
    
    val magLessThan0 = magnitude < min0
    val magLessThan1 = magnitude < min1
    
    
    when(io.in.en && io.in.counter === 0.U){
        gsgn := 0.U
        min0 := Fill(LLRBits, 1.U(1.W))
        min1 := Fill(LLRBits, 1.U(1.W))
        idx0 := 0.U
    }.elsewhen(enDelayed){
        gsgn := gsgn ^ sign
        when(magLessThan0) {
            min1 := min0
            min0 := magnitude
            idx0 := RegNext(io.in.counter, init = 0.U)
        }.elsewhen(magLessThan1) {
            min1 := magnitude
        }
    }

    io.out.gsgn := Mux(enDelayed, gsgn ^ sign, gsgn)
    io.out.idx0 := Mux(enDelayed && magLessThan0, RegNext(io.in.counter, init = 0.U), idx0)
    io.out.min0 := Mux(enDelayed && magLessThan0, magnitude, min0)
    io.out.min1 := Mux(enDelayed && magLessThan0, min0, 
                   Mux(enDelayed && magLessThan1, magnitude, min1))

}


class VNUsInput(implicit p: Parameters) extends DecBundle{
    val en = Bool()
    val ZSize = UInt(log2Ceil(MaxZSize).W)
    val c2vMsg = Vec(MaxZSize, UInt(C2VMsgBits.W)) // Each row in a layer
    val counter = UInt(log2Ceil(MaxDegreeOfCNU).W)
    val isLastCol = Bool()
    val shiftedLLR = Vec(MaxZSize, SInt(LLRBits.W))
}

class VNUsOutput(implicit p: Parameters) extends DecBundle{
    val v2cMsgValid = Bool() // Mv2cFifo en
    val v2cMsg = Vec(MaxZSize, SInt((LLRBits + 1).W)) // Mv2cFifo data, Each row in a layer
    val vnuLayerDone = Bool() // fifo in en
    val gsgn = Vec(MaxZSize, UInt(1.W))
    val min0 = Vec(MaxZSize, UInt(LLRBits.W))
    val min1 = Vec(MaxZSize, UInt(LLRBits.W))
    val idx0 = Vec(MaxZSize, UInt(log2Ceil(MaxDegreeOfCNU).W))
    val v2cLayerMsg = Vec(MaxZSize, UInt((1 + LLRBits + LLRBits + log2Ceil(MaxDegreeOfCNU)).W))
}

class VNUsIO(implicit p: Parameters) extends DecBundle{
    val in = Input(new VNUsInput)
    val out = Output(new VNUsOutput)
}

class VNUs(implicit p: Parameters) extends DecModule {
    val io = IO(new VNUsIO())

    val vnuCoresMask = MaskGenerator(io.in.ZSize, MaxZSize)

    val vnuCores = Seq.fill(MaxZSize)(Module(new VNUCore()))

    for(i <- 0 until MaxZSize){
        vnuCores(i).io.in.en := vnuCoresMask(i)
        vnuCores(i).io.in.c2vMsg := io.in.c2vMsg(i)
        vnuCores(i).io.in.counter := io.in.counter // TODO: fix fanout, add dup_regs in GCU
        vnuCores(i).io.in.shiftedLLR := io.in.shiftedLLR(i)
    }

    io.out.v2cMsgValid := io.in.en
    io.out.vnuLayerDone := DelayN(io.in.isLastCol, 1)
    for(i <- 0 until MaxZSize){
        io.out.v2cMsg(i) := vnuCores(i).io.out.v2cMsg
        io.out.gsgn(i) := vnuCores(i).io.out.gsgn
        io.out.min0(i) := vnuCores(i).io.out.min0
        io.out.min1(i) := vnuCores(i).io.out.min1
        io.out.idx0(i) := vnuCores(i).io.out.idx0
        io.out.v2cLayerMsg(i) := Cat(vnuCores(i).io.out.idx0, vnuCores(i).io.out.min1, vnuCores(i).io.out.min0, vnuCores(i).io.out.gsgn)
    }
}