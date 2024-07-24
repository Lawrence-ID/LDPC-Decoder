package ldpcdecoder

import chisel3._
import chiseltest._
import scala.util.Random
import ldpcdecoder.{QSN, CyclicShifter}
import org.scalatest.flatspec.AnyFlatSpec
import top.DefaultConfig
import ldpcdecoder.DecParamsKey
import org.scalatest.matchers.should.Matchers

class ScalaCyclicShifter(){
    var llr: Seq[Int] = Seq.empty
    var zSize: Int = 0
    var shiftSize: Int = 0
    var out: Seq[Int] = Seq.empty

    def setInput(inputLLR: Seq[Int], inputZSize: Int, inputShiftSize: Int): Unit = {
        require(inputLLR.length <= 384, s"Input LLRs num must less than MaxZSize=384")
        require(inputShiftSize > 0 && inputShiftSize <= inputZSize, s"0 < shiftSize <= zSize < MaxZSize")
        this.llr = inputLLR
        this.zSize = inputZSize
        this.shiftSize = inputShiftSize
    }

    def rotateRight[T](seq: Seq[T], zSize: Int, shiftSize: Int): Seq[T] = {
        val (partToRotate, remainingPart) = seq.splitAt(zSize)
        val shift = shiftSize % zSize
        val rotatedPart = partToRotate.drop(zSize - shift) ++ partToRotate.take(zSize - shift)
        rotatedPart ++ remainingPart
    }

    def rotateLeft[T](seq: Seq[T], zSize: Int, shiftSize: Int): Seq[T] = {
        val (partToRotate, remainingPart) = seq.splitAt(zSize)
        val shift = shiftSize % zSize
        val rotatedPart = partToRotate.drop(shift) ++ partToRotate.take(shift)
        rotatedPart ++ remainingPart
    }
    
    def poke(shiftLeft: Boolean): Unit = {
        out = if (shiftLeft) {
            rotateLeft(llr, zSize, shiftSize)
        } else {
            rotateRight(llr, zSize, shiftSize)
        }
    }
}

class CyclicShifterSpec extends AnyFlatSpec with ChiselScalatestTester with Matchers {
    "CyclicShifter" should "correctly perform left and right circular num shifts" in {
        val defaultConfig = (new DefaultConfig)
        implicit val config = defaultConfig.alterPartial({
            case DecParamsKey => defaultConfig(DecParamsKey)
        })
        val goldenModel = new ScalaCyclicShifter()
        val MaxZSize: Int = 384
        val LLRBits: Int = 6

        test(new CyclicShifter(shiftLeft=true)) { c =>
            val inSeq = Seq(10, 35, 19, 20, 7, 55, 23)
            val zSize = 7
            val shiftSize = 4

            goldenModel.setInput(inSeq, zSize, shiftSize)
            goldenModel.poke(true)

            c.io.in.valid.poke(true.B)
            c.io.out.ready.poke(true.B)

            val inSeq_fill = inSeq ++ Seq.fill(MaxZSize - inSeq.length)(0)
            for(i <- 0 until MaxZSize){
                c.io.in.bits.llr(i).poke(inSeq_fill(i).U(LLRBits.W))
            }
            c.io.in.bits.zSize.poke(zSize.U)
            c.io.in.bits.shiftSize.poke(shiftSize.U)
            c.clock.step()

            val resultVec = (0 until inSeq.length).map { i =>
                c.io.out.bits(i).peek().litValue
            }
            println(s"result = List(${resultVec.mkString(", ")})")
            println(s"answer = ${goldenModel.out}")

            for(i <- 0 until inSeq.length){
                c.io.out.bits(i).expect(goldenModel.out(i).U)
            }

            c.io.in.valid.poke(false.B)
            c.io.out.ready.poke(false.B)
            c.clock.step()
        }

        test(new CyclicShifter(shiftLeft=false)) { c =>
            val inSeq = Seq(10, 35, 19, 20, 7, 55, 23)
            val zSize = 7
            val shiftSize = 4

            goldenModel.setInput(inSeq, zSize, shiftSize)
            goldenModel.poke(false)

            c.io.in.valid.poke(true.B)
            c.io.out.ready.poke(true.B)

            val inSeq_fill = inSeq ++ Seq.fill(MaxZSize - inSeq.length)(0)
            for(i <- 0 until MaxZSize){
                c.io.in.bits.llr(i).poke(inSeq_fill(i).U(LLRBits.W))
            }
            c.io.in.bits.zSize.poke(zSize.U)
            c.io.in.bits.shiftSize.poke(shiftSize.U)
            c.clock.step()

            val resultVec = (0 until inSeq.length).map { i =>
                c.io.out.bits(i).peek().litValue
            }
            println(s"result = List(${resultVec.mkString(", ")})")
            println(s"answer = ${goldenModel.out}")

            for(i <- 0 until inSeq.length){
                c.io.out.bits(i).expect(goldenModel.out(i).U)
            }

            c.io.in.valid.poke(false.B)
            c.io.out.ready.poke(false.B)
            c.clock.step()
        }
    }
}