package ldpcdecoder

import chisel3._
import chisel3.util._
import ldpcdecoder._
import org.chipsalliance.cde.config.Parameters

class ModLUT(implicit p: Parameters) extends DecModule {
  val io = IO(new Bundle {
    val s_mod_zPow = Input(UInt(log2Ceil(MaxZSize).W))
    val b = Input(UInt(log2Ceil(15).W))
    val out = Output(UInt(log2Ceil(15).W))
  })

  val lut: Vec[Vec[UInt]] = VecInit(
    ShiftValueModZcLUT.map(row => VecInit(row.map(_.U(log2Ceil(15).W))))
  )
  io.out := lut(io.s_mod_zPow)(io.b)
}

class ShiftValueModZc(implicit p: Parameters) extends DecModule {
  val io = IO(new Bundle {
    val s = Input(UInt(log2Ceil(MaxZSize).W))    
    val iLS = Input(UInt(log2Ceil(8).W))
    val zPow = Input(UInt(log2Ceil(8).W))
    val out = Output(UInt(log2Ceil(MaxZSize).W))
  })

  val b_table = VecInit(Seq(2.U, 3.U, 5.U, 7.U, 9.U, 11.U, 13.U, 15.U))
  val b = Wire(UInt(log2Ceil(15).W))
  b := b_table(io.iLS)

  val s_mod_zPow = Wire(UInt(log2Ceil(MaxZSize).W))
  s_mod_zPow := io.s >> io.zPow

  // ================use ModLUT================
  val modLUT = Module(new ModLUT)
  modLUT.io.s_mod_zPow := s_mod_zPow
  modLUT.io.b := b
  val s_mod_zPow_mod_b = Wire(UInt(log2Ceil(15).W))
  s_mod_zPow_mod_b := Mux(b === 2.U(log2Ceil(15).W), s_mod_zPow(0), modLUT.io.out)
  io.out := (io.s & ((1.U << io.zPow) - 1.U)) + (s_mod_zPow_mod_b << io.zPow)

  // ================use Mod operation================
  // io.out := (io.s & ((1.U << io.zPow) - 1.U)) + ((s_mod_zPow % b) << io.zPow)
}