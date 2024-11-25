// ram.scala
package utility
 
import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import utility._
class SinglePortRAM extends Module {
  val io = IO(new Bundle {
    val addr = Input(UInt(10.W))
    val dataIn = Input(UInt(32.W))
    val en = Input(Bool())
    val we = Input(Bool())
    val dataOut = Output(UInt(32.W))  
  })
  val syncRAM = SyncReadMem(1024, UInt(32.W))
  when(io.en) {
    when(io.we) {
      syncRAM.write(io.addr, io.dataIn)
      io.dataOut := DontCare
    } .otherwise {
      io.dataOut := syncRAM.read(io.addr)
    }
  } .otherwise {
    io.dataOut := DontCare
  }
}
