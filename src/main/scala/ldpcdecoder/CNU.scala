package ldpcdecoder

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import utility._

class CNUCoreInput (implicit p: Parameters) extends DecBundle{
    val en = Bool()
    val v2cMsg = SInt((LLRBits + 1).W)
    val counter = UInt(log2Ceil(MaxDegreeOfCNU).W)
    val gsgn = UInt(1.W)
    val min0 = UInt(LLRBits.W)
    val min1 = UInt(LLRBits.W)
    val idx0 = UInt(log2Ceil(MaxDegreeOfCNU).W)
}

class CNUCoreOutput (implicit p: Parameters) extends DecBundle{
    val c2vMsgOld = UInt(C2VMsgBits.W) // Write to C2V RAM
    val LLR = SInt(LLRBits.W)
}

class CNUCoreIO(implicit p: Parameters) extends DecBundle{
    val in = Input(new CNUCoreInput)
    val out = Output(new CNUCoreOutput)
}

class CNUCore(implicit p: Parameters) extends DecModule {
    val io = IO(new CNUCoreIO())

    // First Stage
    val saturatorMin0 = Module(new UnsignedSaturator(LLRBits, LLRBits - 1))
    val saturatorMin1 = Module(new UnsignedSaturator(LLRBits, LLRBits - 1))

    saturatorMin0.io.in := io.in.min0
    saturatorMin1.io.in := io.in.min1

    val min0Sat = saturatorMin0.io.out
    val min1Sat = saturatorMin1.io.out

    io.out.c2vMsgOld := Cat(io.in.idx0, min1Sat, min0Sat, io.in.gsgn)

    // printf(p"idx0: ${io.in.idx0}, min1Sat: ${min1Sat}, min0Sat: ${min0Sat}, gsgn: ${io.in.gsgn} \n")

    val c2vMsgMagitude = Mux(io.in.counter === io.in.idx0, io.in.min1, io.in.min0)
    val c2vMsgSign = io.in.gsgn ^ io.in.v2cMsg(LLRBits)

    val signMagCombinator = Module(new SignMagCmb(LLRBits))
    signMagCombinator.io.en := io.in.en
    signMagCombinator.io.sign := c2vMsgSign
    signMagCombinator.io.magnitude := c2vMsgMagitude

    val c2vMsgReg = RegInit(0.S((LLRBits + 1).W))
    val v2cMsgReg = RegInit(0.S((LLRBits + 1).W))
    when(io.in.en){
        c2vMsgReg := signMagCombinator.io.out
        v2cMsgReg := io.in.v2cMsg
    }

    // Second Stage
    val enDelayed = DelayN(io.in.en, 1)
    val LLR = Wire(SInt((LLRBits + 2).W))
    LLR := c2vMsgReg +& v2cMsgReg

    val LLRMax = (1.S << (LLRBits - 1)) - 1.S 
    val LLRMin = -(1.S << (LLRBits - 1))

    val saturatorLLR = Module(new SignedSaturator(LLRBits + 2, LLRBits))
    saturatorLLR.io.in := LLR

    val LLRSat = RegEnable(saturatorLLR.io.out, 0.S(LLRBits.W), enDelayed)

    io.out.LLR := Mux(enDelayed, saturatorLLR.io.out, LLRSat)

    
}

class CNUsInput(implicit p: Parameters) extends DecBundle{
    val en = Bool()
    val ZSize = UInt(log2Ceil(MaxZSize).W)
    val v2cMsg = Vec(MaxZSize, SInt((LLRBits + 1).W)) // Mc2vFifo data, Each row in a layer
    val counter = UInt(log2Ceil(MaxDegreeOfCNU).W)
    val gsgn = Vec(MaxZSize, UInt(1.W))
    val min0 = Vec(MaxZSize, UInt(LLRBits.W))
    val min1 = Vec(MaxZSize, UInt(LLRBits.W))
    val idx0 = Vec(MaxZSize, UInt(log2Ceil(MaxDegreeOfCNU).W))
}

class CNUsOutput(implicit p: Parameters) extends DecBundle{
    val c2vMsgWen = Bool()                              // Write en
    val c2vMsgOld = Vec(MaxZSize, UInt(C2VMsgBits.W))   // Write to C2V RAM
    val LLRValid = Bool()
    val LLR = Vec(MaxZSize, SInt(LLRBits.W))
}

class CNUsIO(implicit p: Parameters) extends DecBundle{
    val in = Input(new CNUsInput)
    val out = Output(new CNUsOutput)
}

class CNUs(implicit p: Parameters) extends DecModule {
    val io = IO(new CNUsIO())

    val cnuCoresMask = MaskGenerator(io.in.ZSize, MaxZSize)

    val cnuCores = Seq.fill(MaxZSize)(Module(new CNUCore()))

    for(i <- 0 until MaxZSize){
        cnuCores(i).io.in.en := cnuCoresMask(i)
        cnuCores(i).io.in.v2cMsg := io.in.v2cMsg(i)
        cnuCores(i).io.in.counter := io.in.counter // TODO: fix fanout, add dup_regs in GCU
        cnuCores(i).io.in.gsgn := io.in.gsgn(i) 
        cnuCores(i).io.in.min0 := io.in.min0(i) 
        cnuCores(i).io.in.min1 := io.in.min1(i) 
        cnuCores(i).io.in.idx0 := io.in.idx0(i) 
    }

    io.out.c2vMsgWen := io.in.en && io.in.counter === 0.U
    io.out.LLRValid := DelayN(io.in.en, 1)
    for(i <- 0 until MaxZSize){
        io.out.LLR(i) := cnuCores(i).io.out.LLR
        io.out.c2vMsgOld(i) := cnuCores(i).io.out.c2vMsgOld
    }
}