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

object ScalaSignedSaturator{
    def apply(outWidth: Int, value: Int): Int = {
        require(6 <= outWidth && outWidth <= 8)
        val maxVal = (1 << (outWidth - 1)) - 1
        val minVal = - (1 << (outWidth - 1))
        if(value > maxVal) maxVal else if(value < minVal) minVal else value
    }
}

object ScalaUnsignedSaturator{
    def apply(outWidth: Int, value: Int): Int = {
        require(6 <= outWidth && outWidth <= 8)
        require(value >= 0)
        val maxVal = (1 << outWidth) - 1
        if(value > maxVal) maxVal else value
    }
}

class ScalaCNUCore {
    val LLRBits: Int = 6

    var c2vMsg_en: Int = 0
    var c2vMsg_gsgn: Int = 0
    var c2vMsg_min0: Int = 0
    var c2vMsg_min1: Int = 0
    var c2vMsg_idx0: Int = 0
    var unshiftedLLR: Int = 0

    val scalaSignMagModule = new ScalaSignMag(LLRBits)

    def calculate(gsgn: Int, min0: Int, min1: Int, idx0: Int, v2cMsg: Int, counter: Int): Unit = {
        require(0 <= min0 && min0 <= (1 << LLRBits) - 1)
        require(0 <= min1 && min1 <= (1 << LLRBits) - 1)
        require(gsgn == 0 || gsgn == 1)
        require(-(1 << LLRBits) <= v2cMsg && v2cMsg <= (1 << LLRBits) - 1) // LLRBits + 1

        var uSatMin0 = if(min0 > (1 << (LLRBits - 1)) - 1) (1 << (LLRBits - 1)) - 1 else min0
        var uSatMin1 = if(min0 > (1 << (LLRBits - 1)) - 1) (1 << (LLRBits - 1)) - 1 else min1

        this.c2vMsg_gsgn = gsgn
        this.c2vMsg_min0 = uSatMin0
        this.c2vMsg_min1 = uSatMin1
        this.c2vMsg_idx0 = idx0

        val v2cMsgSign = if(v2cMsg < 0) 1 else 0
        val c2vMsg = scalaSignMagModule.combination(v2cMsgSign ^ gsgn, if(counter == idx0) min1 else min0) // LLRBits + 1

        val LLR = c2vMsg + v2cMsg // LLRBits + 2

        val satLLR = ScalaSignedSaturator(outWidth=LLRBits, value=LLR)
        this.unshiftedLLR = satLLR
        println(s"counter ${counter} Answer unshiftedLLR: ${this.unshiftedLLR}")

    }
}

class CNUCoreSpec extends AnyFlatSpec with ChiselScalatestTester with Matchers {
    "CNUCore" should "correctly output the c2vMsg, unshiftedLLR" in {
        val defaultConfig = (new DefaultConfig)
        implicit val config = defaultConfig.alterPartial({
            case DecParamsKey => defaultConfig(DecParamsKey)
        })

        test(new CNUCore()) { c =>
            // Row 0
            var counter = 0
            var row = 0
            var gsgn = 0 // 0
            var min0 = 5 // 5, 1
            var min1 = 15 // 10, 6
            var idx0 = 0 // 15, 11

            val v2cMsgSeq: Seq[Int] = Seq(5, 15, -22, 23)
            val goldenModel = new ScalaCNUCore
            for(i <- 0 until v2cMsgSeq.length){
                counter = i
                goldenModel.calculate(gsgn=gsgn, min0=min0, min1=min1, idx0=idx0, v2cMsg=v2cMsgSeq(counter), counter=counter)
            }

            for(i <- 0 until v2cMsgSeq.length){
                counter = i
                c.io.in.en.poke(true.B)
                c.io.in.counter.poke(counter.U)
                c.io.in.v2cMsg.poke(v2cMsgSeq(counter).S)
                c.io.in.gsgn.poke(gsgn.U)
                c.io.in.min0.poke(min0.U)
                c.io.in.min1.poke(min1.U)
                c.io.in.idx0.poke(idx0.U)
                c.clock.step(1)
                println(s"Output: clock: ${counter}, unshiftedLLR: ${c.io.out.unshiftedLLR.peek().litValue}")
            }

            c.io.in.en.poke(false.B)
            c.clock.step(1)
            println(s"Output: clock: ${counter + 1}, unshiftedLLR: ${c.io.out.unshiftedLLR.peek().litValue}")

        }

    }
}