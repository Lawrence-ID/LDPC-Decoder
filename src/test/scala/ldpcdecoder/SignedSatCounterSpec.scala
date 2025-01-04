package ldpcdecoder

import chisel3._
import chisel3.util._
import chiseltest._
import scala.util.Random
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import top.DefaultConfig
import top.ArgParser
import scala.collection.mutable

import ldpcdecoder._

class ScalaSignedSatCounter(len: Int) {
  // 定义计数器的最大值和最小值
  private val maxValue = (1 << (len - 1)) - 1
  private val minValue = -(1 << (len - 1))

  // 当前计数器的值
  private var value: Int = 0

  /**
   * 更新计数器值
   * @param umask     掩码 (Seq[Boolean])，控制哪些 deltaType 生效
   * @param deltaType 增量类型 (Seq[Int])，用于决定具体的增量值
   */
  def update(umask: Seq[Boolean], deltaType: Seq[Int]): Unit = {
    require(umask.length == deltaType.length, "umask 和 deltaType 长度必须相等")

    // 通过查表逻辑计算 finalDelta
    val finalDelta = deltaType.zip(umask).map { case (dt, mask) =>
      if (mask) {
        dt match {
          case 0 => 1
          case 1 => 1
          case 2 => 5
          case 3 => -50
          case 4 => 1
          case 5 => -30
          case _ => 0 // 默认值
        }
      } else {
        0 // mask 为 false 时不更新
      }
    }.sum

    // 更新计数器值并进行饱和处理
    val updated = value + finalDelta
    value = math.max(math.min(updated, maxValue), minValue)

    // 打印调试信息
    // println(s"umask: $umask, deltaType: $deltaType, finalDelta: $finalDelta, updated value: $value")
  }

  /**
   * 获取当前计数器值
   * @return 当前计数器的值
   */
  def getValue: Int = value

  /**
   * 重置计数器
   * @param newValue 新的计数器值
   */
  def reset(newValue: Int): Unit = {
    require(newValue >= minValue && newValue <= maxValue, "新值必须在有效范围内")
    value = newValue
  }
}


class SignedSatCounterSpec extends AnyFlatSpec with ChiselScalatestTester {

  "SignedSatCounterSpec" should "correctly perform signed Saturate Counter" in {
    val defaultConfig = (new DefaultConfig)
    implicit val config = defaultConfig.alterPartial({
        case DecParamsKey => defaultConfig(DecParamsKey)
    })

    val Width: Int = 10
    val numBr: Int = 2
    val goldenModel = new ScalaSignedSatCounter(Width)

    test(new SignedSatCounter(numBr, Width)) { c =>
        c.clock.setTimeout(0)
        // 定义激励
        val umaskStimuli = Seq(
          Seq(true, false), 
          Seq(false, true), 
          Seq(true, true),
        )

        val deltaTypeStimuli = Seq(
          Seq(1, 0),
          Seq(1, 1),
          Seq(1, 2),
          Seq(1, 3),
          Seq(1, 4),
          Seq(1, 5),
          Seq(1, 6),
          Seq(2, 0),
          Seq(2, 1),
          Seq(2, 2),
          Seq(2, 3),
          Seq(2, 4),
          Seq(2, 5),
          Seq(2, 6),
          Seq(3, 0),
          Seq(3, 1),
          Seq(3, 2),
          Seq(3, 3),
          Seq(3, 4),
          Seq(3, 5),
          Seq(3, 6),
          Seq(4, 0),
          Seq(4, 1),
          Seq(4, 2),
          Seq(4, 3),
          Seq(4, 4),
          Seq(4, 5),
          Seq(4, 6),
          Seq(5, 0),
          Seq(5, 1),
          Seq(5, 2),
          Seq(5, 3),
          Seq(5, 4),
          Seq(5, 5),
          Seq(5, 6),
          Seq(6, 0),
          Seq(6, 1),
          Seq(6, 2),
          Seq(6, 3),
          Seq(6, 4),
          Seq(6, 5),
          Seq(6, 6),
        )
        val countMap = mutable.Map[Int, Int]()
        for(i <- 0 until 100000){
          val umask_idx = Random.nextInt(2)
          val deltaType_idx = Random.nextInt(36)
          countMap(deltaType_idx) = countMap.getOrElse(deltaType_idx, 0) + 1
          for(w <- 0 until numBr){
            c.io.umask(w).poke(umaskStimuli(umask_idx)(w).B)
            c.io.deltaType(w).poke(deltaTypeStimuli(deltaType_idx)(w).U)
          }
          c.clock.step()
          goldenModel.update(umaskStimuli(umask_idx), deltaTypeStimuli(deltaType_idx))
          // println(s"i = $i, out = ${c.io.value.peek().litValue}, goldenModel = ${goldenModel.getValue}")
          c.io.value.expect(goldenModel.getValue.S)
        }

        // 输出统计结果
        println("Value counts:")
        countMap.foreach { case (value, count) =>
          println(s"Value $value appears $count times")
        }
    }

    
  }
}