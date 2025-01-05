package ldpcdecoder

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import utility._

class UnsignedSaturator(val InWidth: Int, val OutWidth: Int)(implicit p: Parameters) extends DecModule {
  val io = IO(new Bundle {
    val in  = Input(UInt(InWidth.W))
    val out = Output(UInt(OutWidth.W))
  })

  val maxVal = WireInit(Fill(OutWidth, 1.U(1.W)))

  io.out := Mux(io.in > maxVal, maxVal, io.in(OutWidth - 1, 0))

}

object UnsignedSaturator {
  def apply(in: UInt, InWidth: Int, OutWidth: Int)(implicit p: Parameters): UInt = {
    val saturator = Module(new UnsignedSaturator(InWidth, OutWidth))
    saturator.io.in := in
    saturator.io.out
  }
}

class SignedSaturator(val InWidth: Int, val OutWidth: Int)(implicit p: Parameters) extends DecModule {
  val io = IO(new Bundle {
    val in  = Input(SInt(InWidth.W))
    val out = Output(SInt(OutWidth.W))
  })

  val maxVal = WireInit((1.S << (OutWidth - 1)) - 1.S) // 011...1
  val minVal = WireInit(-(1.S << (OutWidth - 1)))      // 100...0

  io.out := Mux(io.in > maxVal, maxVal, Mux(io.in < minVal, minVal, io.in(OutWidth - 1, 0).asSInt))

}

object SignedSaturator {
  def apply(in: SInt, InWidth: Int, OutWidth: Int)(implicit p: Parameters): SInt = {
    val saturator = Module(new SignedSaturator(InWidth, OutWidth))
    saturator.io.in := in
    saturator.io.out
  }
}
