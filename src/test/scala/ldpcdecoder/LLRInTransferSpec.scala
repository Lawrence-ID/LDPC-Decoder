package ldpcdecoder

import chisel3._
import chiseltest._
import scala.util.Random
import ldpcdecoder.{QSN, CyclicShifter}
import org.scalatest.flatspec.AnyFlatSpec
import top.DefaultConfig
import ldpcdecoder.DecParamsKey
import org.scalatest.matchers.should.Matchers
import chisel3.util._

class LLRInTransferSpec extends AnyFlatSpec with ChiselScalatestTester with Matchers {
    "LLRInTransfer" should "correctly transfer llr from PC to DecoderCore" in {
        val defaultConfig = (new DefaultConfig)
        implicit val config = defaultConfig.alterPartial({
            case DecParamsKey => defaultConfig(DecParamsKey)
        })
        val MaxZSize: Int = 384
        val LLRBits: Int = 6
        val LLRFromWidth: Int = 256

        test(new LLRInTransfer()) { c => 
            c.io.in.bits.zSize.poke(3.U)
            c.io.in.bits.isBG1.poke(true.B)
            c.io.in.valid.poke(true.B)
            c.io.out.ready.poke(true.B)

            for(i <- 0 until 14) {
                // Create an array with elements from 0 to 31, each element being an 8-bit number.
                val byteArray = (i * 32 until i * 32 + 32).map(i => i.toByte)

                // Concatenate the elements into a single 256-bit binary number (as a string for clarity)
                val concatenated = byteArray.reverse.map(b => String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0')).mkString
                println(s"concatenated: ${concatenated}")

                // Convert the concatenated string to a BigInt (256-bit number)
                val concatenatedBigInt = BigInt(concatenated, 2)
                c.io.in.bits.llrBlock.poke(concatenatedBigInt.U)

                c.clock.step()
                if (c.io.out.valid.peek().litValue == 1){
                    for (j <- 0 until 384) {
                        println(s"llrOut: ${c.io.out.bits.rawLLR(j).peek().litValue}")
                    }
                }
            }

        }
    }
}