package ldpcdecoder

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import utility._
import javax.xml.crypto.Data

class Mc2vFIFO(DataWidth: Int, Depth: Int)(implicit p: Parameters) extends DecModule{
    val io = IO(new Bundle {
        val in = Flipped(Decoupled(UInt(DataWidth.W)))
        val out = Decoupled(UInt(DataWidth.W))
    })
    val fifo = Queue(io.in, Depth)
    io.out <> fifo
}

