package ldpcdecoder

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import utility._

class Mv2cFIFO[T <: Data](gen: T, Depth: Int)(implicit p: Parameters) extends DecModule {
    val io = IO(new Bundle {
        val in = Flipped(Decoupled(gen))
        val out = Decoupled(gen)
    })
    val fifo = Queue(io.in, Depth)
    io.out <> fifo
}