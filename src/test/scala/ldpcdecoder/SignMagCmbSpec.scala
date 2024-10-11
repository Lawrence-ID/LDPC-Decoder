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

class SignMagCmbSpec extends AnyFlatSpec with ChiselScalatestTester {

  "SignMagCmb" should "correctly perform sign && magnitude combination" in {
    val defaultConfig = (new DefaultConfig)
    implicit val config = defaultConfig.alterPartial({
        case DecParamsKey => defaultConfig(DecParamsKey)
    })

    val LLRBits: Int = 6

    test(new SignMagCmb(LLRBits - 1)) { c =>

        for(i <- 0 to 31){
            c.io.en.poke(true.B)
            c.io.sign.poke(1.U)
            c.io.magnitude.poke(i.U)
            c.clock.step()
            // println(s"sign = 1, magtitude = $i, out = ${c.io.out.peek().litValue}")
            c.io.out.expect((-i).S)
        }

        for(i <- 0 to 31){
            c.io.en.poke(true.B)
            c.io.sign.poke(0.U)
            c.io.magnitude.poke(i.U)
            c.clock.step()
            // println(s"sign = 0, magtitude = $i, out = ${c.io.out.peek().litValue}")
            c.io.out.expect(i.S)
        }
    }

    
  }
}