// package ldpcdecoder

// import chisel3._
// import chisel3.util._
// import chiseltest._
// import scala.util.Random
// import org.scalatest.flatspec.AnyFlatSpec
// import org.scalatest.matchers.should.Matchers
// import top.DefaultConfig
// import top.ArgParser

// import ldpcdecoder._
// import utility._

// class SRAMSpec extends AnyFlatSpec with ChiselScalatestTester with Matchers {
//     "SRAM" should "correctly output the data" in {
//         val defaultConfig = (new DefaultConfig)
//         implicit val config = defaultConfig.alterPartial({
//             case DecParamsKey => defaultConfig(DecParamsKey)
//         })

//         test(new SinglePortRAM()) { c =>
//             c.io.en.poke(true.B)
//             c.io.we.poke(true.B)
//             c.io.addr.poke(1.U)
//             c.io.dataIn.poke(10.U)

//             c.clock.step(1)

//             c.io.we.poke(false.B)
//             println(s"rdata: ${c.io.dataOut.peek().litValue}")

//             c.clock.step(1)
//             println(s"rdata: ${c.io.dataOut.peek().litValue}")
//         }

//         val LLRBits = 6

//         test(new Mv2cFIFO(LLRBits + 1, 30)) { c =>
//             c.io.out.ready.poke(false.B)
//             c.io.in.valid.poke(true.B)  // Enqueue an element
//             c.io.in.bits.poke(42.U)
//             println(s"Starting:")
//             println(s"\tio.in: ready=${c.io.in.ready.peek().litValue}")
//             println(s"\tio.out: valid=${c.io.out.valid.peek().litValue}, bits=${c.io.out.bits.peek().litValue}")
//             c.clock.step(1)

//             c.io.in.valid.poke(true.B)  // Enqueue another element
//             c.io.in.bits.poke(43.U)
            
//             println(s"After first enqueue:")
//             println(s"\tio.in: ready=${c.io.in.ready.peek().litValue}")
//             println(s"\tio.out: valid=${c.io.out.valid.peek().litValue}, bits=${c.io.out.bits.peek().litValue}")
//             c.clock.step(1)

//             c.io.in.valid.poke(true.B)  // Read a element, attempt to enqueue
//             c.io.in.bits.poke(44.U)
//             c.io.out.ready.poke(true.B)
//             // What do you think io.in.ready will be, and will this enqueue succeed, and what will be read?
//             println(s"On first read:")
//             println(s"\tio.in: ready=${c.io.in.ready.peek()}")
//             println(s"\tio.out: valid=${c.io.out.valid.peek()}, bits=${c.io.out.bits.peek()}")
//             c.clock.step(1)

//             c.io.in.valid.poke(false.B)  // Read elements out
//             c.io.out.ready.poke(true.B)
//             // What do you think will be read here?
//             println(s"On second read:")
//             println(s"\tio.in: ready=${c.io.in.ready.peek()}")
//             println(s"\tio.out: valid=${c.io.out.valid.peek()}, bits=${c.io.out.bits.peek()}")
//             c.clock.step(1)

//             // Will a third read produce anything?
//             println(s"On third read:")
//             println(s"\tio.in: ready=${c.io.in.ready.peek()}")
//             println(s"\tio.out: valid=${c.io.out.valid.peek()}, bits=${c.io.out.bits.peek()}")
//             c.clock.step(1)
//         }

//     }
// }