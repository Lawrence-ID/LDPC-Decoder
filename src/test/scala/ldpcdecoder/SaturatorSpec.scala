package ldpcdecoder

import chisel3._
import chisel3.util._
import chiseltest._
import scala.util.Random
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import top.DefaultConfig
import top.ArgParser

import ldpcdecoder._

class SaturatorSpec extends AnyFlatSpec with ChiselScalatestTester {
    "Saturator" should "correctly saturate the input" in {
    val defaultConfig = (new DefaultConfig)
    implicit val config = defaultConfig.alterPartial({
        case DecParamsKey => defaultConfig(DecParamsKey)
    })

    val LLRBits: Int = 6
    var InWidth = LLRBits
    var OutWidth = LLRBits - 1

    test(new UnsignedSaturator(InWidth, OutWidth)) { c =>
        val maxVal = (math.pow(2, OutWidth) - 1).toInt

        for(i <- 0 until math.pow(2, InWidth).toInt){
            c.io.in.poke(i.U)
            c.clock.step()
            // println(s"in = $i, out = ${c.io.out.peek().litValue}")
            c.io.out.expect(if(i > maxVal) maxVal.U else i.U)
        }
    }

    InWidth = LLRBits + 2
    OutWidth = LLRBits

    test(new SignedSaturator(InWidth, OutWidth)) { c =>
        val begin = - (1 << (InWidth - 1))
        val end   = (1 << (InWidth - 1)) - 1

        val maxVal = (1 << (OutWidth - 1)) - 1
        val minVal = - (1 << (OutWidth - 1))

        for(i <- begin to end){
            c.io.in.poke(i.S)
            c.clock.step()
            // println(s"in = $i, out = ${c.io.out.peek().litValue}")
            c.io.out.expect(if(i > maxVal) maxVal.S else if(i < minVal) minVal.S else i.S)
        }
    }

    
  }
}