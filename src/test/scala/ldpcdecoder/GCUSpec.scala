package ldpcdecoder

import chisel3._
import chiseltest._
import scala.util.Random
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import top.DefaultConfig
import top.ArgParser

import ldpcdecoder._

// class GCUSpec extends AnyFlatSpec with ChiselScalatestTester with Matchers {
//     "GCU" should "correctly output the col-id" in {
//         val defaultConfig = (new DefaultConfig)
//         implicit val config = defaultConfig.alterPartial({
//             case DecParamsKey => defaultConfig(DecParamsKey)
//         })

//         test(new GCU()) { c =>
//             c.reset.poke(true.B)
//             c.clock.step()
//             c.reset.poke(false.B)

//             for(i <- 0 until 100){
//                 println(s"llrRAddr : ${c.io.llrRAddr.bits.peek().litValue}, llrRen: ${c.io.llrRAddr.valid.peek().litValue}, isLastCol: ${c.io.isLastCol.peek().litValue}")
//                 c.clock.step()
//             }
//         }
//     }
// }