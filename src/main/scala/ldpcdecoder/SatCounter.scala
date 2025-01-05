package ldpcdecoder

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import utility._

class SignedSatCounter(val numBr: Int, val Width: Int)(implicit p: Parameters) extends DecModule with BPUUtils {
  val io = IO(new Bundle {
    val umask     = Input(Vec(numBr, Bool()))
    val deltaType = Input(Vec(numBr, UInt(3.W)))
    val value     = Output(SInt(Width.W))
  })

  val ctr = RegInit(0.S(Width.W));
  ctr      := signedSatUpdate(io.umask, ctr, Width, io.deltaType)
  io.value := ctr;

}
