package ldpcdecoder

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import utility._

class DynamicOnes(maxWidth: Int)(implicit p: Parameters) extends DecModule {
  val io = IO(new Bundle {
    val in = Input(UInt(log2Ceil(maxWidth + 1).W))
    val out = Output(UInt(maxWidth.W))
  })

  val allOnesFull = Fill(maxWidth, 1.U)

  val shiftAmount = maxWidth.U - io.in

  io.out := allOnesFull >> shiftAmount
}