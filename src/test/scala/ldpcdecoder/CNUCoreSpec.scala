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

class ScalaCNUCore {
    val LLRBits = 6
    val LLRMax = (1 << (LLRBits - 1)) - 1
    val LLRMin = -(1 << (LLRBits - 1))

    var LLRSat: Int = 0
    var c2vMsgOld: Int = 0

    def calc(min0: Int, min1: Int, idx0: Int, gsgn: Int, v2cMsg: Int, counter: Int): Unit = {
        require(min0 >= 0 && min0 <= 63 && min1 >= 0 && min1 <= 63 && min0 <= min1, 
                "ScalaCNUCore: 0 <= min0 <= min1 <= 63")

        val minSatMax = (1 << (LLRBits - 1)) - 1
        var min0Sat = if(min0 > minSatMax) minSatMax else min0
        var min1Sat = if(min1 > minSatMax) minSatMax else min1
        
        this.c2vMsgOld = (idx0 << 11) | (min1Sat << 6) | (min0Sat << 1) | gsgn

        var globalSign = if(gsgn == 0) 1 else -1
        var c2vMsgSign = if(v2cMsg >= 0) globalSign else -globalSign
        var c2vMsg = (if(counter == idx0) min1 else min0) * c2vMsgSign

        var LLR = c2vMsg + v2cMsg
        
        this.LLRSat = if(LLR > LLRMax) LLRMax else if(LLR < LLRMin) LLRMin else LLR
        
    }
}

class CNUCoreSpec extends AnyFlatSpec with ChiselScalatestTester with Matchers {
    "CNUCore" should "correctly output the c1vMsg, LLRSat" in {
        val defaultConfig = (new DefaultConfig)
        implicit val config = defaultConfig.alterPartial({
            case DecParamsKey => defaultConfig(DecParamsKey)
        })

        val MaxZSize: Int = 384
        val LLRBits: Int = 6
        val MaxDegreeOfCNU: Int = 19
        val C2VMsgBits: Int = 1 + 2 * (LLRBits - 1) + log2Ceil(MaxDegreeOfCNU)

        test(new CNUCore()) { c =>
            var gsgnAtLayer = Seq(0, 0, 1)  // 0
            var min0AtLayer = Seq(1, 3, 0) // 5, 1
            var min1AtLayer = Seq(2, 7, 4) // 10, 6
            var idx0AtLayer = Seq(18,2, 1) // 15, 11

            val v2cMsgFifo: Seq[Int] = Seq(31, -22, 24, -14, -28, -27, 16, -18, -26, 25, -31, -29, -2, -9, -3, -19, -20, 19, -1,
                                        -7, 13, -3, 
                                        -24, 0, 29, -16, -32, 4, -4, -16)

            val counterSeq: Seq[Int] = Seq(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 
                                        0, 1, 2,
                                        0, 1, 2, 3, 4, 5, 6, 7)

            val goldenModel = new ScalaCNUCore

            var counter = 0
            var layer = -1
            for(i <- 0 until counterSeq.length){
                counter = counterSeq(i)
                if(counter == 0) layer = layer + 1

                goldenModel.calc(gsgn = gsgnAtLayer(layer),
                                 min0 = min0AtLayer(layer),
                                 min1 = min1AtLayer(layer),
                                 idx0 = idx0AtLayer(layer),
                                 v2cMsg = v2cMsgFifo(i),
                                 counter = counter)
                println(s"Answer: layer: ${layer}, counter: ${counter}, c2vMsgOld: ${goldenModel.c2vMsgOld}, LLRSat: ${goldenModel.LLRSat}")
                
                c.io.in.en.poke(true.B)
                c.io.in.gsgn.poke(gsgnAtLayer(layer).U)
                c.io.in.min0.poke(min0AtLayer(layer).U) 
                c.io.in.min1.poke(min1AtLayer(layer).U) 
                c.io.in.idx0.poke(idx0AtLayer(layer).U) 
                c.io.in.v2cMsg.poke(v2cMsgFifo(i).S)
                c.io.in.counter.poke(counter.U)

                println(s"Input: counter: ${counter}, gsgn: ${gsgnAtLayer(layer)}, min0: ${min0AtLayer(layer)}, min1: ${min1AtLayer(layer)}, idx0: ${idx0AtLayer(layer)}, v2cMsg: ${v2cMsgFifo(i)}")
                println(s"Output: clock: ${counter} before, c2vMsg(wen:${c.io.in.en.peek().litToBoolean && c.io.in.counter.peek().litValue == 0}): ${c.io.out.c2vMsgOld.peek().litValue}, LLR: ${c.io.out.LLR.peek().litValue}")
                c.clock.step(1)
                println(s"Output: clock: ${counter} after, c2vMsg(wen:${c.io.in.en.peek().litToBoolean && c.io.in.counter.peek().litValue == 0}): ${c.io.out.c2vMsgOld.peek().litValue}, LLR: ${c.io.out.LLR.peek().litValue}")
                println("=================================================")
            }

            c.io.in.en.poke(false.B)
            println(s"Input en false")
            println(s"Output: clock: ${counter} before, c2vMsg(wen:${c.io.in.en.peek().litToBoolean && c.io.in.counter.peek().litValue == 0}): ${c.io.out.c2vMsgOld.peek().litValue}, LLR: ${c.io.out.LLR.peek().litValue}")
            c.clock.step(1)
            println(s"Output: clock: ${counter} after, c2vMsg(wen:${c.io.in.en.peek().litToBoolean && c.io.in.counter.peek().litValue == 0}): ${c.io.out.c2vMsgOld.peek().litValue}, LLR: ${c.io.out.LLR.peek().litValue}")
        }

    }
}