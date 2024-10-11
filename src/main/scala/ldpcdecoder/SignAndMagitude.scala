package ldpcdecoder

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import utility._

class SignMagSep(val MagWidth: Int)(implicit p: Parameters) extends DecModule {
    val io = IO(new Bundle {
        val en = Input(Bool())
        val in = Input(SInt((MagWidth + 1).W))
        val sign = Output(UInt(1.W))
        val magnitude = Output(UInt(MagWidth.W)) 
    })

    io.sign := 0.U 
    io.magnitude := 0.U

    when(io.en) {
        when(io.in === -math.pow(2, MagWidth).toInt.S) {
            // 特例处理: in = -32 时，输出 -31 的表示
            io.sign := 1.U
            io.magnitude := (math.pow(2, MagWidth).toInt - 1).U // 11111 表示 31
        }.elsewhen(io.in(MagWidth) === 1.U){ 
            io.sign := 1.U
            io.magnitude := (~io.in(MagWidth - 1, 0)).asUInt + 1.U 
        } .otherwise {
            io.sign := 0.U
            io.magnitude := io.in(MagWidth - 1, 0).asUInt 
        }
    }
}

object SignMagSep {
    def apply(en: Bool, in: SInt, MagWidth: Int)(implicit p: Parameters): (UInt, UInt) = {
        val sep = Module(new SignMagSep(MagWidth))
        sep.io.en := en
        sep.io.in := in
        (sep.io.sign, sep.io.magnitude)
    }
}

class SignMagCmb(val MagWidth: Int)(implicit p: Parameters) extends DecModule {
    val io = IO(new Bundle {
        val en = Input(Bool())
        val sign = Input(UInt(1.W))
        val magnitude = Input(UInt(MagWidth.W)) 
        val out = Output(SInt((MagWidth + 1).W))
    })

    io.out := 0.S

    when(io.en) {
        when(io.magnitude === 0.U) {
            io.out := 0.S
        } .elsewhen(io.sign === 1.U) {
            io.out := Cat(io.sign, (~io.magnitude).asUInt + 1.U).asSInt
        } .otherwise {
            io.out := Cat(io.sign, io.magnitude).asSInt
        }
    }
}
