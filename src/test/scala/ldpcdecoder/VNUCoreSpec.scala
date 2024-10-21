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

class ScalaSignMag(val LLRBits: Int) {

  def combination(sign: Int, mag: Int): Int = {
    require(sign == 0 || sign == 1)
    require(mag >= -math.pow(2, LLRBits - 1) && mag <= math.pow(2, LLRBits - 1) - 1)
    val value: Int = if(mag == 0) 0 else if(sign == 1) -mag else mag
    value
  }
  
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
  val scalaSignMagModule = new ScalaSignMag(LLRBits)

  def calculate(v2cSignOld: Int, min0Old: Int, min1Old: Int, idx0Old: Int, gsgnOld: Int, counter: Int, shiftedLLR: Int): Unit = {
    require((v2cSignOld == 0 || v2cSignOld == 1) && (gsgnOld == 0 || gsgnOld == 1))
    require(shiftedLLR >= -32 && shiftedLLR <= 31)
    require((0 <= min0Old && min0Old <= 31) && (0 <= min1Old && min1Old <= 31))

    if(counter == 0){
      this.min0 = (1 << LLRBits) - 1
      this.min1 = (1 << LLRBits) - 1
      this.gsgn = 0
      this.idx0 = 0
    }

    // 计算上一次迭代的c2vMsg
    val c2vMsgPrevIter = scalaSignMagModule.combination(gsgnOld ^ v2cSignOld, if(idx0Old == counter) min1Old else min0Old)

    val v2cMsg: Int = SixBitArithmetic.sub(shiftedLLR, c2vMsgPrevIter) // 7 bits
    val (v2cSign, v2cMagnitude) = scalaSignMagModule.separation(v2cMsg)

    // 计算 gsgn
    this.gsgn = this.gsgn ^ v2cSign

    if(v2cMagnitude < this.min0){
      this.min1 = this.min0
      this.min0 = v2cMagnitude
      this.idx0 = counter
    }else if(v2cMagnitude < this.min1){
      this.min1 = v2cMagnitude
    }
    println(s"shiftedLLR = ${shiftedLLR}, c2vMsgPrevIter = ${c2vMsgPrevIter}, v2cMsg: (${v2cSign}, ${v2cMagnitude})")
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
        val C2VRowMsgBits: Int = 1 + 2 * (LLRBits - 1) + log2Ceil(MaxDegreeOfCNU)

        test(new VNUCore()) { c =>
            // Generate random input values
            // val random = new Random()
            // val c2vMsg = random.nextInt((1 << C2VRowMsgBits) - 1)
            // val counter = random.nextInt((1 << log2Ceil(MaxDegreeOfCNU)) - 1)
            // val shiftedLLR = random.nextInt((1 << (LLRBits-1)) - 1)


            // Row 0
            var row = 0
            var gsgnOld = 1 // 0
            var min0Old = 14 // 5, 1
            var min1Old = 3 // 10, 6
            var idx0Old = 0 // 15, 11
            println(s"gsgnOld: ${gsgnOld}, min0Old: ${min0Old}, min1Old: ${min1Old}, idx0Old: ${idx0Old}")

            var c2vMsg = (idx0Old << 11) | (min1Old << 6) | (min0Old << 1) | gsgnOld
            c.io.in.c2vRowMsgOld.poke(c2vMsg.U)

            val shiftedLLRSeq: Seq[Seq[Int]] = Seq(Seq(1, -32, 12, -1), Seq(0, 20, -17, 1))
            val v2cSignOldSeq: Seq[Seq[Int]] = Seq(Seq(1, 0, 0, 1), Seq(1, 0, 0, 1))

            val goldenModel = new ScalaVNUCore
            goldenModel.calculate(v2cSignOld=v2cSignOldSeq(row)(0), min0Old=min0Old, min1Old=min1Old, idx0Old=idx0Old, gsgnOld=gsgnOld, counter=0, shiftedLLR=shiftedLLRSeq(row)(0))
            goldenModel.calculate(v2cSignOld=v2cSignOldSeq(row)(1), min0Old=min0Old, min1Old=min1Old, idx0Old=idx0Old, gsgnOld=gsgnOld, counter=1, shiftedLLR=shiftedLLRSeq(row)(1))
            goldenModel.calculate(v2cSignOld=v2cSignOldSeq(row)(2), min0Old=min0Old, min1Old=min1Old, idx0Old=idx0Old, gsgnOld=gsgnOld, counter=2, shiftedLLR=shiftedLLRSeq(row)(2))
            goldenModel.calculate(v2cSignOld=v2cSignOldSeq(row)(3), min0Old=min0Old, min1Old=min1Old, idx0Old=idx0Old, gsgnOld=gsgnOld, counter=3, shiftedLLR=shiftedLLRSeq(row)(3))

            var counter = 0
            var shiftedLLR = shiftedLLRSeq(row)(counter)
            var v2cSignOld = v2cSignOldSeq(row)(counter)

            for(i <- 0 to 3){
              counter = i
              shiftedLLR = shiftedLLRSeq(row)(counter)
              v2cSignOld = v2cSignOldSeq(row)(counter)

              c.io.in.en.poke(true.B)
              c.io.in.counter.poke(counter.U)
              c.io.in.shiftedLLR.poke(shiftedLLR.S)
              c.io.in.v2cSignOld.poke(v2cSignOld.U)
              println(s"Input: counter: ${counter}, shiftedLLR: ${shiftedLLR}")
              c.clock.step(1)
              println(s"Output: clock: ${counter}, gsgn: ${c.io.out.gsgn.peek().litValue}, min0: ${c.io.out.min0.peek().litValue}, min1: ${c.io.out.min1.peek().litValue}, idx0: ${c.io.out.idx0.peek().litValue}, v2cMsg: ${c.io.out.v2cMsg.peek().litValue}")
            }

            // Row 1
            println("-----------------------------------------------------")
            row = 1
            gsgnOld = 0 // 0
            min0Old = 5 // 5, 1
            min1Old = 22 // 10, 6
            idx0Old = 3 // 15, 11
            println(s"gsgnOld: ${gsgnOld}, min0Old: ${min0Old}, min1Old: ${min1Old}, idx0Old: ${idx0Old}")

            c2vMsg = (idx0Old << 11) | (min1Old << 6) | (min0Old << 1) | gsgnOld
            c.io.in.c2vRowMsgOld.poke(c2vMsg.U)

            goldenModel.calculate(v2cSignOld=v2cSignOldSeq(row)(0), min0Old=min0Old, min1Old=min1Old, idx0Old=idx0Old, gsgnOld=gsgnOld, counter=0, shiftedLLR=shiftedLLRSeq(row)(0))
            goldenModel.calculate(v2cSignOld=v2cSignOldSeq(row)(1), min0Old=min0Old, min1Old=min1Old, idx0Old=idx0Old, gsgnOld=gsgnOld, counter=1, shiftedLLR=shiftedLLRSeq(row)(1))
            goldenModel.calculate(v2cSignOld=v2cSignOldSeq(row)(2), min0Old=min0Old, min1Old=min1Old, idx0Old=idx0Old, gsgnOld=gsgnOld, counter=2, shiftedLLR=shiftedLLRSeq(row)(2))
            goldenModel.calculate(v2cSignOld=v2cSignOldSeq(row)(3), min0Old=min0Old, min1Old=min1Old, idx0Old=idx0Old, gsgnOld=gsgnOld, counter=3, shiftedLLR=shiftedLLRSeq(row)(3))

            for(i <- 0 to 3){
              counter = i
              shiftedLLR = shiftedLLRSeq(row)(counter)
              v2cSignOld = v2cSignOldSeq(row)(counter)

              c.io.in.en.poke(true.B)
              c.io.in.counter.poke(counter.U)
              c.io.in.shiftedLLR.poke(shiftedLLR.S)
              c.io.in.v2cSignOld.poke(v2cSignOld.U)
              println(s"Input: counter: ${counter}, shiftedLLR: ${shiftedLLR}")
              c.clock.step(1)
              println(s"Output: clock: ${counter}, gsgn: ${c.io.out.gsgn.peek().litValue}, min0: ${c.io.out.min0.peek().litValue}, min1: ${c.io.out.min1.peek().litValue}, idx0: ${c.io.out.idx0.peek().litValue}, v2cMsg: ${c.io.out.v2cMsg.peek().litValue}")
            }
            
            c.io.in.en.poke(false.B)
            c.clock.step(1)
            println(s"Output: clock: ${counter + 1}, gsgn: ${c.io.out.gsgn.peek().litValue}, min0: ${c.io.out.min0.peek().litValue}, min1: ${c.io.out.min1.peek().litValue}, idx0: ${c.io.out.idx0.peek().litValue}, v2cMsg: ${c.io.out.v2cMsg.peek().litValue}")

        }
    }
}