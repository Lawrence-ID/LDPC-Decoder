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

class SignMagSepSpec extends AnyFlatSpec with ChiselScalatestTester {

  "SignMagSep" should "correctly perform sign && magnitude seperation" in {
    val defaultConfig = (new DefaultConfig)
    implicit val config = defaultConfig.alterPartial({
        case DecParamsKey => defaultConfig(DecParamsKey)
    })

    val LLRBits: Int = 6

    test(new SignMagSep(LLRBits)) { c =>

        for(i <- -64 to 63){
            c.io.en.poke(true.B)
            c.io.in.poke(i.S)
            c.clock.step()
            // println(s"in = $i, out = ${c.io.sign.peek().litValue}, ${c.io.magnitude.peek().litValue}")
            c.io.sign.expect(if(i < 0) 1.U else 0.U)
            c.io.magnitude.expect(if(i == -64) 63.U else if(i < 0) (-i).U else i.U)
        }
    }

    val InWidth = 8
    val OutWidth = 6
    test(new SignedSaturator(InWidth, OutWidth)){ c =>
      val InMax = (1 << (InWidth-1)) - 1
      val InMin = -(1 << (InWidth-1))
      val OutMax = (1 << (OutWidth-1)) - 1
      val OutMin = -(1 << (OutWidth-1))
      for(i <- InMin until InMax){
        c.io.in.poke(i.S)
        // println(s"Input: ${i}, Output: ${c.io.out.peek().litValue}")
        c.io.out.expect(if (i > OutMax) OutMax else if(i < OutMin) OutMin else (i))
      }
    }

    
  }
}