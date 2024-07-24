package ldpcdecoder

import chisel3._
import chiseltest._
import scala.util.Random
import ldpcdecoder.QSN
import org.scalatest.flatspec.AnyFlatSpec
import top.DefaultConfig
import ldpcdecoder.DecParamsKey

class ScalaQSN(length: Int) {
  var in: String = "0" * length
  var p: Int = 0
  var c: Int = 0
  var out: String = "0" * length

  def setInput(inputStr: String, p: Int, c: Int): Unit = {
    require(inputStr.length == length, s"Input string must have length $length")
    require(p > 0 && p <= length, s"p must be greater than 0 and less than or equal to $length")
    require(c >= 0, "c must be non-negative")
    this.in = inputStr
    this.p = p
    this.c = c
  }

  private def getLastPBits(str: String, p: Int): String = str.takeRight(p)

  private def circularRightShift(str: String, shiftAmount: Int, p: Int): String = {
    val lastPBits = getLastPBits(str, p)
    val shifted = lastPBits.drop(shiftAmount) ++ lastPBits.take(shiftAmount)
    str.dropRight(p) ++ shifted
  }

  private def circularLeftShift(str: String, shiftAmount: Int, p: Int): String = {
    val lastPBits = getLastPBits(str, p)
    val shifted = lastPBits.takeRight(shiftAmount) ++ lastPBits.dropRight(shiftAmount)
    str.dropRight(p) ++ shifted
  }

  // 执行移位操作，并更新输出
  def poke(shiftLeft: Boolean): Unit = {
    out = if (shiftLeft) {
      circularLeftShift(in, c, p)
    } else {
      circularRightShift(in, c, p)
    }
  }
}

class QSNSpec extends AnyFlatSpec with ChiselScalatestTester {

  def generateRandomBinaryString(length: Int): String = {
    Seq.fill(length)(Random.nextBoolean()).map(if (_) '1' else '0').mkString
  }

  "QSN" should "correctly perform left and right circular shifts" in {
    val defaultConfig = (new DefaultConfig)
    implicit val config = defaultConfig.alterPartial({
        case DecParamsKey => defaultConfig(DecParamsKey)
    })
    val MaxZSize: Int = 384
    val goldenModel = new ScalaQSN(MaxZSize)

    test(new QSN(shiftLeft = true)) { c =>
      for(i <- 0 until 100){
        val in = generateRandomBinaryString(MaxZSize)
        val p = Random.nextInt(MaxZSize-1) + 1
        val ShiftCnt = Random.nextInt(p) + 1

        goldenModel.setInput(in, p, ShiftCnt)
        c.io.in.valid.poke(true.B)
        c.io.in.bits.srcData.poke(("b"++in).U)
        c.io.in.bits.zSize.poke(p.U)
        c.io.in.bits.shiftSize.poke(ShiftCnt.U)
        c.clock.step()
        goldenModel.poke(true)
        val goldenModelResult = goldenModel.out
        // println(s"p = $p, c = $ShiftCnt, original = $in, result = $goldenModelResult")
        c.io.out.bits.expect(("b"++goldenModelResult).U)
        c.io.in.valid.poke(false.B)
        c.clock.step()
      }
    }

    test(new QSN(shiftLeft = false)) { c =>
      for(i <- 0 until 100){
        val in = generateRandomBinaryString(MaxZSize)
        val p = Random.nextInt(MaxZSize-1) + 1
        val ShiftCnt = Random.nextInt(p) + 1

        goldenModel.setInput(in, p, ShiftCnt)
        c.io.in.valid.poke(true.B)
        c.io.in.bits.srcData.poke(("b"++in).U)
        c.io.in.bits.zSize.poke(p.U)
        c.io.in.bits.shiftSize.poke(ShiftCnt.U)
        c.clock.step()
        goldenModel.poke(false)
        val goldenModelResult = goldenModel.out
        // println(s"p = $p, c = $ShiftCnt, original = $in, result = $goldenModelResult")
        c.io.out.bits.expect(("b"++goldenModelResult).U)
        c.io.in.valid.poke(false.B)
        c.clock.step()
      }
    }
  }
}
