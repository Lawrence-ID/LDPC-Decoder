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
import utility._

object SixBitArithmetic {
  // 定义6位有符号数的范围
  val Min6Bit: Int = -32  // 6-bit signed minimum value
  val Max6Bit: Int = 31   // 6-bit signed maximum value

  // 将6位数转换为7位数
  private def to7Bit(value: Int): Int = {
    value & 0x7F // 0x7F = 01111111，保留7位
  }

  // 加法操作
  def add(a: Int, b: Int): Int = {
    require(a >= Min6Bit && a <= Max6Bit, "a must be a 6-bit signed integer")
    require(b >= Min6Bit && b <= Max6Bit, "b must be a 6-bit signed integer")
    val sum = a + b
    to7Bit(sum) // 返回7位结果
  }

  // 减法操作
  def sub(a: Int, b: Int): Int = {
    require(a >= Min6Bit && a <= Max6Bit, "a must be a 6-bit signed integer")
    require(b >= Min6Bit && b <= Max6Bit, "b must be a 6-bit signed integer")
    val difference = a - b
    to7Bit(difference) // 返回7位结果
  }
}

class ScalaSignMagSep(val LLRBits: Int) {

  def separation(in: Int): (Int, Int) = {
    val maxNegativeValue = -math.pow(2, LLRBits).toInt
    val signBit = (in >> LLRBits) & 1 // 取最高有效位（MSB）
    val magnitudeMask = (1 << LLRBits) - 1 // 用于取低 LLRBits 位的掩码

    val (sign, magnitude) = if (signBit == 1) {
      if (in == maxNegativeValue) {
        (1, magnitudeMask) // 特例：当输入为 -64 时，输出 -63 的表示
      } else {
        (1, (~in & magnitudeMask) + 1) // 负数的绝对值
      }
    } else {
      (0, in & magnitudeMask) // 正数
    }

    (sign, magnitude)
  }
}


class ScalaVNUCore {

  val LLRBits = 6
  var min0: Int = (1 << LLRBits) - 1
  var min1: Int = (1 << LLRBits) - 1
  var gsgn: Int = 0
  var idx0: Int = 0
  val scalaSignMagSepModule = new ScalaSignMagSep(LLRBits)

  def calculate(min0Old: Int, min1Old: Int, idx0Old: Int, gsgnOld: Int, counter: Int, shiftedLLR: Int): Unit = {
    require(shiftedLLR >= -32 && shiftedLLR <= 31)

    // 计算上一次迭代的c2vMsg
    val c2vMsgPrevIter = if (idx0Old == counter) {
      (gsgnOld << LLRBits) | min1Old // Cat(gsgnOld, min1Old)
    } else {
      (gsgnOld << LLRBits) | min0Old // Cat(gsgnOld, min0Old)
    }

    val v2cMsg: Int = SixBitArithmetic.sub(shiftedLLR, c2vMsgPrevIter) // 7 bits
    val (v2cSign, v2cMagnitude) = scalaSignMagSepModule.separation(v2cMsg)

    // 计算 gsgn
    if(counter == 0){
      this.gsgn = 0
      this.min0 = (1 << LLRBits) - 1
      this.min1 = (1 << LLRBits) - 1
      this.idx0 = 0
    }

    this.gsgn = this.gsgn ^ v2cSign

    if(v2cMagnitude < this.min0){
      this.min1 = this.min0
      this.min0 = v2cMagnitude
      this.idx0 = counter
    }else if(v2cMagnitude < this.min1){
      this.min1 = v2cMagnitude
    }
    println(s"counter ${counter} Answer gsgn: ${this.gsgn}, min0: ${this.min0}, min1: ${this.min1}, idx0: ${this.idx0}")
  }
}

class VNUCoreSpec extends AnyFlatSpec with ChiselScalatestTester with Matchers {
    "VNUCore" should "correctly output the min0, min1, idx0, gsgn" in {
      val defaultConfig = (new DefaultConfig)
      implicit val config = defaultConfig.alterPartial({
          case DecParamsKey => defaultConfig(DecParamsKey)
      })

      val MaxZSize: Int = 384
      val LLRBits: Int = 6
      val MaxDegreeOfCNU: Int = 19
      val C2VMsgBits: Int = 1 + 2 * (LLRBits - 1) + log2Ceil(MaxDegreeOfCNU)
      
      test(new MaskGenerator(MaxZSize)){ c => 
        for(testIn <- 0 to MaxZSize){
          c.io.in.poke(testIn.U)
          for (i <- 0 until MaxZSize) {
              c.io.out(i).expect((i < testIn).B)
              // println(s"out($i): ${c.io.out(i).peek().litToBoolean}")
          }
        }
      }
      
      test(new VNUCore()) { c =>
        // Generate random input values
        // val random = new Random()
        // val c2vMsg = random.nextInt((1 << C2VMsgBits) - 1)
        // val counter = random.nextInt((1 << log2Ceil(MaxDegreeOfCNU)) - 1)
        // val shiftedLLR = random.nextInt((1 << (LLRBits-1)) - 1)
        var gsgnOld = 0 // 0
        var min0Old = 0 // 5, 1
        var min1Old = 0 // 10, 6
        var idx0Old = 0 // 15, 11
        println(s"gsgnOld: ${gsgnOld}, min0Old: ${min0Old}, min1Old: ${min1Old}, idx0Old: ${idx0Old}")
        var c2vMsg = (idx0Old << 11) | (min1Old << 6) | (min0Old << 1) | gsgnOld
        c.io.in.c2vMsg.poke(c2vMsg.U)
        val shiftedLLRSeq: Seq[Int] = Seq(31, -22, 24, -14, -28, -27, 16, -18, -26, 25, -31, -29, -2, -9, -3, -19, -20, 19, -1,
                                          -7, 13, -3, 
                                          -24, 0, 29, -16, -32, 4, -4, -16)
        val isLastColSeq: Seq[Boolean] = Seq(false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true,
                                          false, false, true,
                                          false, false, false, false, false, false, false, true)
        val counterSeq: Seq[Int] = Seq(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 
                                       0, 1, 2,
                                       0, 1, 2, 3, 4, 5, 6, 7)
        val goldenModel = new ScalaVNUCore
        
        var counter = 0
        for(i <- 0 until counterSeq.length){
          goldenModel.calculate(min0Old=min0Old, min1Old=min1Old, idx0Old=idx0Old, gsgnOld=gsgnOld, counter=counterSeq(i), shiftedLLR = shiftedLLRSeq(i))
          
          counter = counterSeq(i)
          var shiftedLLR = shiftedLLRSeq(i)
          c.io.in.en.poke(true.B)
          // c.io.in.isLastCol.poke(isLastColSeq(counter).B)
          c.io.in.counter.poke(counter.U)
          c.io.in.shiftedLLR.poke(shiftedLLR.S)
          println(s"Input: counter: ${counter}, shiftedLLR: ${shiftedLLR}")
          println(s"Output: clock: ${counter} before, gsgn: ${c.io.out.gsgn.peek().litValue}, min0: ${c.io.out.min0.peek().litValue}, min1: ${c.io.out.min1.peek().litValue}, idx0: ${c.io.out.idx0.peek().litValue}, v2cMsg: ${c.io.out.v2cMsg.peek().litValue}, v2cMsg.valid: ${c.io.in.en.peek().litValue}")
          c.clock.step(1)
          println(s"Output: clock: ${counter} after, gsgn: ${c.io.out.gsgn.peek().litValue}, min0: ${c.io.out.min0.peek().litValue}, min1: ${c.io.out.min1.peek().litValue}, idx0: ${c.io.out.idx0.peek().litValue}, v2cMsg: ${c.io.out.v2cMsg.peek().litValue}, v2cMsg.valid: ${c.io.in.en.peek().litValue}")
          c.io.out.gsgn.expect(goldenModel.gsgn)
          c.io.out.min0.expect(goldenModel.min0)
          c.io.out.min1.expect(goldenModel.min1)
          c.io.out.idx0.expect(goldenModel.idx0)
          println("=================================================")
        }
        
        c.io.in.en.poke(false.B)
        c.clock.step(1)
        println(s"Output: clock: ${counter + 1}, gsgn: ${c.io.out.gsgn.peek().litValue}, min0: ${c.io.out.min0.peek().litValue}, min1: ${c.io.out.min1.peek().litValue}, idx0: ${c.io.out.idx0.peek().litValue}, v2cMsg: ${c.io.out.v2cMsg.peek().litValue}")
        c.io.out.gsgn.expect(goldenModel.gsgn)
        c.io.out.min0.expect(goldenModel.min0)
        c.io.out.min1.expect(goldenModel.min1)
        c.io.out.idx0.expect(goldenModel.idx0)
      }
    }
}