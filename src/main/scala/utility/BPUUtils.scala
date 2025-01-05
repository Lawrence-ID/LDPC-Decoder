package utility

import chisel3._
import chisel3.util._

trait BPUUtils {
  // circular shifting
  def circularShiftLeft(source: UInt, len: Int, shamt: UInt): UInt = {
    val res    = Wire(UInt(len.W))
    val higher = source << shamt
    val lower  = source >> (len.U - shamt)
    res := higher | lower
    res
  }

  def circularShiftRight(source: UInt, len: Int, shamt: UInt): UInt = {
    val res    = Wire(UInt(len.W))
    val higher = source << (len.U - shamt)
    val lower  = source >> shamt
    res := higher | lower
    res
  }

  // To be verified
  def satUpdate(old: UInt, len: Int, taken: Bool): UInt = {
    val oldSatTaken    = old === ((1 << len) - 1).U
    val oldSatNotTaken = old === 0.U
    Mux(oldSatTaken && taken, ((1 << len) - 1).U, Mux(oldSatNotTaken && !taken, 0.U, Mux(taken, old + 1.U, old - 1.U)))
  }

  def signedSatUpdate(old: SInt, len: Int, taken: Bool): SInt = {
    val oldSatTaken    = old === ((1 << (len - 1)) - 1).S
    val oldSatNotTaken = old === (-(1 << (len - 1))).S
    Mux(
      oldSatTaken && taken,
      ((1 << (len - 1)) - 1).S,
      Mux(oldSatNotTaken && !taken, (-(1 << (len - 1))).S, Mux(taken, old + 1.S, old - 1.S))
    )
  }

  def signedSatUpdate(uMask: Vec[Bool], old: SInt, len: Int, deltaType: Vec[UInt]): SInt = {
    val maxValue = ((1 << (len - 1)) - 1).S(len.W)
    val minValue = (-(1 << (len - 1))).S(len.W)

    val uIdx = PriorityEncoder(uMask)

    val finalDelta = MuxLookup(deltaType(uIdx), 0.S)(
      Seq(
        0.U -> 1.S,   // SC opened, SC agree and correct, can be closed
        1.U -> 1.S,   // SC opened, SC agree but wrong, should be closed more
        2.U -> 5.S,   // SC opened, SC disagree and wrong, should definitely be closed
        3.U -> -50.S, // SC opened, SC disagree but correct, should stay open
        4.U -> 1.S,   // SC closed, TAGE pred correct, SC can be closed
        5.U -> -30.S  // SC closed, TAGE pred wrong, SC should be opened
      )
    )

    // Update the value and perform saturation
    val updated = old +& finalDelta

    // printf(p"umask: ${uMask}, old: ${old}, deltaType: ${deltaType}, finalDelta: ${finalDelta} \n")

    val saturated = MuxCase(
      updated,
      Seq(
        (updated > maxValue) -> maxValue,
        (updated < minValue) -> minValue
      )
    )
    saturated(len - 1, 0).asSInt
  }
}
