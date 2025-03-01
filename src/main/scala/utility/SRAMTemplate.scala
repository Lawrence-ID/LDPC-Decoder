/***************************************************************************************
* Copyright (c) 2020-2021 Institute of Computing Technology, Chinese Academy of Sciences
* Copyright (c) 2020-2021 Peng Cheng Laboratory
*
* XiangShan is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

/**************************************************************************************
* Copyright (c) 2020 Institute of Computing Technology, CAS
* Copyright (c) 2020 University of Chinese Academy of Sciences
*
* NutShell is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*             http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER
* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR
* FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

package utility

import chisel3._
import chisel3.util._

class SRAMBundleA(val set: Int) extends Bundle {
  val setIdx = Output(UInt(log2Up(set).W))

  def apply(setIdx: UInt) = {
    this.setIdx := setIdx
    this
  }
}

class SRAMBundleAW[T <: Data](private val gen: T, set: Int, val way: Int = 1, val useBitmask: Boolean = false)
    extends SRAMBundleA(set) {

  private val dataWidth = gen.getWidth

  val data: Vec[T]          = Output(Vec(way, gen))
  val waymask: Option[UInt] = if (way > 1) Some(Output(UInt(way.W))) else None
  // flattened_bitmask is the flattened form of [waymask, bitmask], can be use directly to mask memory
  val flattened_bitmask: Option[UInt] = if (useBitmask) Some(Output(UInt((way * dataWidth).W))) else None
  // bitmask is the original bitmask passed from parameter
  val bitmask: Option[UInt] = if (useBitmask) Some(Output(UInt(dataWidth.W))) else None

  def apply(data: Vec[T], setIdx: UInt, waymask: UInt): SRAMBundleAW[T] = {
    require(
      waymask.getWidth == way,
      s"waymask width does not equal nWays, waymask width: ${waymask.getWidth}, nWays: ${way}"
    )
    super.apply(setIdx)
    this.data := data
    this.waymask.foreach(_ := waymask)
    this
  }

  def apply(data: Vec[T], setIdx: UInt, waymask: UInt, bitmask: UInt): SRAMBundleAW[T] = {
    require(useBitmask, "passing bitmask when not using bitmask")
    require(
      bitmask.getWidth == dataWidth,
      s"bitmask width does not equal data width, bitmask width: ${bitmask.getWidth}, data width: ${dataWidth}"
    )
    apply(data, setIdx, waymask)
    this.flattened_bitmask.foreach(_ :=
      VecInit.tabulate(way * dataWidth)(n => waymask(n / dataWidth) && bitmask(n % dataWidth)).asUInt)
    this.bitmask.foreach(_ := bitmask)
    this
  }

  // this could only be used when waymask is onehot or nway is 1
  def apply(data: T, setIdx: UInt, waymask: UInt): SRAMBundleAW[T] = {
    apply(VecInit(Seq.fill(way)(data)), setIdx, waymask)
    this
  }

  def apply(data: T, setIdx: UInt, waymask: UInt, bitmask: UInt): SRAMBundleAW[T] = {
    apply(VecInit(Seq.fill(way)(data)), setIdx, waymask, bitmask)
    this
  }
}

class SRAMBundleR[T <: Data](private val gen: T, val way: Int = 1) extends Bundle {
  val data = Output(Vec(way, gen))
}

class SRAMReadBus[T <: Data](private val gen: T, val set: Int, val way: Int = 1) extends Bundle {
  val req  = Decoupled(new SRAMBundleA(set))
  val resp = Flipped(new SRAMBundleR(gen, way))

  def apply(valid: Bool, setIdx: UInt) = {
    this.req.bits.apply(setIdx)
    this.req.valid := valid
    this
  }
}

class SRAMWriteBus[T <: Data](
    private val gen: T,
    val set: Int,
    val way: Int = 1,
    val useBitmask: Boolean = false
) extends Bundle {
  val req = Decoupled(new SRAMBundleAW(gen, set, way, useBitmask))

  def apply(valid: Bool, data: Vec[T], setIdx: UInt, waymask: UInt): SRAMWriteBus[T] = {
    this.req.bits.apply(data = data, setIdx = setIdx, waymask = waymask)
    this.req.valid := valid
    this
  }

  def apply(valid: Bool, data: Vec[T], setIdx: UInt, waymask: UInt, bitmask: UInt): SRAMWriteBus[T] = {
    this.req.bits.apply(data = data, setIdx = setIdx, waymask = waymask, bitmask = bitmask)
    this.req.valid := valid
    this
  }

  def apply(valid: Bool, data: T, setIdx: UInt, waymask: UInt): SRAMWriteBus[T] = {
    apply(valid, VecInit(Seq.fill(way)(data)), setIdx, waymask)
    this
  }

  def apply(valid: Bool, data: T, setIdx: UInt, waymask: UInt, bitmask: UInt): SRAMWriteBus[T] = {
    apply(valid, VecInit(Seq.fill(way)(data)), setIdx, waymask, bitmask)
    this
  }
}

class SRAMTemplate[T <: Data](
    gen: T,
    set: Int,
    way: Int = 1,
    singlePort: Boolean = false,
    shouldReset: Boolean = false,
    extraReset: Boolean = false,
    holdRead: Boolean = false,
    bypassWrite: Boolean = false,
    useBitmask: Boolean = false,
    withClockGate: Boolean = false
) extends Module {
  val io = IO(new Bundle {
    val r = Flipped(new SRAMReadBus(gen, set, way))
    val w = Flipped(new SRAMWriteBus(gen, set, way, useBitmask))
  })
  val extra_reset = if (extraReset) Some(IO(Input(Bool()))) else None

  val wordType               = UInt(gen.getWidth.W)
  val arrayWidth             = if (useBitmask) 1 else gen.getWidth
  val arrayType              = UInt(arrayWidth.W)
  val arrayPortSize          = if (useBitmask) way * gen.getWidth else way
  val array                  = SyncReadMem(set, Vec(arrayPortSize, arrayType))
  val (resetState, resetSet) = (WireInit(false.B), WireInit(0.U))

  if (shouldReset) {
    val _resetState              = RegInit(true.B)
    val (_resetSet, resetFinish) = Counter(_resetState, set)
    when(resetFinish)(_resetState := false.B)
    if (extra_reset.isDefined) {
      when(extra_reset.get) {
        _resetState := true.B
      }
    }

    resetState := _resetState
    resetSet   := _resetSet
  }

  val (ren, wen) = (io.r.req.valid, io.w.req.valid || resetState)
  val realRen    = if (singlePort) ren && !wen else ren

  val maskedClock = ClockGate(false.B, ren || wen, clock)

  val setIdx = Mux(resetState, resetSet, io.w.req.bits.setIdx)
  val wdata  = Mux(resetState, 0.U.asTypeOf(Vec(way, gen)), io.w.req.bits.data).asTypeOf(Vec(arrayPortSize, arrayType))

  // Memeory write
  if (!useBitmask) {
    val waymask = Mux(resetState, Fill(way, "b1".U), io.w.req.bits.waymask.getOrElse("b1".U))
    when(wen) {
      if (withClockGate) {
        array.write(setIdx, wdata, waymask.asBools, maskedClock)
      } else {
        array.write(setIdx, wdata, waymask.asBools)
      }
    }
  } else {
    val bitmask = Mux(resetState, Fill(way * gen.getWidth, "b1".U), io.w.req.bits.flattened_bitmask.getOrElse("b1".U))
    when(wen) {
      if (withClockGate) {
        array.write(setIdx, wdata, bitmask.asBools, maskedClock)
      } else {
        array.write(setIdx, wdata, bitmask.asBools)
      }
    }
  }

  // Memory read
  val raw_rdata = if (withClockGate) {
    array.read(io.r.req.bits.setIdx, realRen, maskedClock).asTypeOf(Vec(way, wordType))
  } else {
    array.read(io.r.req.bits.setIdx, realRen).asTypeOf(Vec(way, wordType))
  }
  require(wdata.getWidth == raw_rdata.getWidth)

  // bypass for dual-port SRAMs
  require(!bypassWrite || bypassWrite && !singlePort)
  def need_bypass(wen: Bool, waddr: UInt, wmask: UInt, ren: Bool, raddr: UInt): UInt = {
    val need_check     = ren && wen
    val need_check_reg = GatedValidRegNext(need_check)
    val waddr_reg      = RegEnable(waddr, need_check)
    val raddr_reg      = RegEnable(raddr, need_check)
    val wmask_reg      = RegEnable(wmask, need_check)
    require(wmask.getWidth == way)
    val bypass = Fill(way, need_check_reg && waddr_reg === raddr_reg) & wmask_reg
    bypass.asTypeOf(UInt(way.W))
  }
  val bypass_wdata = if (bypassWrite)
    VecInit(RegEnable(io.w.req.bits.data, io.w.req.valid && io.r.req.valid).map(_.asTypeOf(wordType)))
  else VecInit((0 until way).map(_ => LFSR64().asTypeOf(wordType)))
  val bypass_mask = need_bypass(
    io.w.req.valid,
    io.w.req.bits.setIdx,
    io.w.req.bits.waymask.getOrElse("b1".U),
    io.r.req.valid,
    io.r.req.bits.setIdx
  )
  val mem_rdata =
    if (singlePort) raw_rdata
    else VecInit(bypass_mask.asBools.zip(raw_rdata).zip(bypass_wdata).map {
      case ((m, r), w) => Mux(m, w, r)
    })

  // hold read data for SRAMs
  val rdata = (if (holdRead) HoldUnless(mem_rdata, GatedValidRegNext(realRen))
               else mem_rdata).map(_.asTypeOf(gen))

  io.r.resp.data := VecInit(rdata)
  io.r.req.ready := !resetState && (if (singlePort) !wen else true.B)
  io.w.req.ready := true.B

}

class FoldedSRAMTemplate[T <: Data](
    gen: T,
    set: Int,
    width: Int = 4,
    way: Int = 1,
    shouldReset: Boolean = false,
    extraReset: Boolean = false,
    holdRead: Boolean = false,
    singlePort: Boolean = false,
    bypassWrite: Boolean = false,
    useBitmask: Boolean = false,
    withClockGate: Boolean = false
) extends Module {
  val io = IO(new Bundle {
    val r = Flipped(new SRAMReadBus(gen, set, way))
    val w = Flipped(new SRAMWriteBus(gen, set, way, useBitmask))
  })
  val extra_reset = if (extraReset) Some(IO(Input(Bool()))) else None
  //   |<----- setIdx ----->|
  //   | ridx | width | way |

  require(width > 0 && isPow2(width))
  require(way > 0 && isPow2(way))
  require(set % width == 0)

  val nRows = set / width

  val array = Module(new SRAMTemplate(
    gen,
    set = nRows,
    way = width * way,
    shouldReset = shouldReset,
    extraReset = extraReset,
    holdRead = holdRead,
    singlePort = singlePort,
    bypassWrite = bypassWrite,
    useBitmask = useBitmask,
    withClockGate = withClockGate
  ))
  if (array.extra_reset.isDefined) {
    array.extra_reset.get := extra_reset.get
  }

  io.r.req.ready := array.io.r.req.ready
  io.w.req.ready := array.io.w.req.ready

  val raddr = io.r.req.bits.setIdx >> log2Ceil(width)
  val ridx  = RegEnable(if (width != 1) io.r.req.bits.setIdx(log2Ceil(width) - 1, 0) else 0.U(1.W), io.r.req.valid)
  val ren   = io.r.req.valid

  array.io.r.req.valid       := ren
  array.io.r.req.bits.setIdx := raddr

  val rdata = array.io.r.resp.data
  for (w <- 0 until way) {
    val wayData  = VecInit(rdata.indices.filter(_ % way == w).map(rdata(_)))
    val holdRidx = HoldUnless(ridx, GatedValidRegNext(io.r.req.valid))
    val realRidx = if (holdRead) holdRidx else ridx
    io.r.resp.data(w) := Mux1H(UIntToOH(realRidx, width), wayData)
  }

  val wen      = io.w.req.valid
  val wdata    = VecInit(Seq.fill(width)(io.w.req.bits.data).flatten)
  val waddr    = io.w.req.bits.setIdx >> log2Ceil(width)
  val widthIdx = if (width != 1) io.w.req.bits.setIdx(log2Ceil(width) - 1, 0) else 0.U
  val wmask = (width, way) match {
    case (1, 1) => 1.U(1.W)
    case (x, 1) => UIntToOH(widthIdx)
    case _ =>
      VecInit(Seq.tabulate(width * way)(n => (n / way).U === widthIdx && io.w.req.bits.waymask.get(n % way))).asUInt
  }
  require(wmask.getWidth == way * width)

  if (useBitmask) {
    array.io.w.apply(wen, wdata, waddr, wmask, io.w.req.bits.bitmask.get)
  } else {
    array.io.w.apply(wen, wdata, waddr, wmask)
  }
}
class SRAMTemplateWithArbiter[T <: Data](nRead: Int, gen: T, set: Int, way: Int = 1, shouldReset: Boolean = false)
    extends Module {
  val io = IO(new Bundle {
    val r = Flipped(Vec(nRead, new SRAMReadBus(gen, set, way)))
    val w = Flipped(new SRAMWriteBus(gen, set, way))
  })

  val ram = Module(new SRAMTemplate(gen, set, way, shouldReset = shouldReset, holdRead = false, singlePort = true))
  ram.io.w <> io.w

  val readArb = Module(new Arbiter(chiselTypeOf(io.r(0).req.bits), nRead))
  readArb.io.in <> io.r.map(_.req)
  ram.io.r.req <> readArb.io.out

  // latch read results
  io.r.map {
    case r => {
      r.resp.data := HoldUnless(ram.io.r.resp.data, GatedValidRegNext(r.req.fire))
    }
  }
}
