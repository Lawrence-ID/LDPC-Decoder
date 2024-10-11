package ldpcdecoder

import chisel3._
import chisel3.util._

import org.chipsalliance.cde.config.{Field, Parameters}

case object DecParamsKey extends Field[DecParameters]

case class DecParameters
(
  MaxZSize: Int = 384,
  LLRBits: Int = 6,
  isBG1: Boolean = true,

  MaxDegreeOfCNU: Int = 19,

  DelayOfShifter: Int = 3,
  DelayOfVNU: Int = 2,
  DelayOfCNU: Int = 2,

  BG1NumAtLayer: Seq[Int] = Seq(
    19, 19, 19, 19, 3, 8, 9, 7, 10, 9, 7, 8, 7, 6, 7, 7, 6, 6, 6, 6, 6, 6, 5, 5, 6, 5, 5, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 4, 5, 5, 4, 5, 4, 5, 5, 4
  ),

  BG2NumAtLayer: Seq[Int] = Seq(
    8, 10, 8, 10, 4, 6, 6, 6, 4, 5, 5, 5, 4, 5, 5, 4, 5, 5, 4, 4, 4, 4, 3, 4, 4, 3, 5, 3, 4, 3, 5, 3, 4, 4, 4, 4, 4, 3, 4, 4, 4, 4
  ),

  BG1ShiftValue: Seq[Int] = Seq(
    /*0*/ 307, 19, 50, 369, 181, 216, 317, 288, 109, 17, 357, 215, 106, 242, 180, 330, 346, 1, 0,
    /*1*/ 76, 76, 73, 288, 144, 331, 331, 178, 295, 342, 217, 99, 354, 114, 331, 112, 0, 0, 0,
    /*2*/ 205, 250, 328, 332, 256, 161, 267, 160, 63, 129, 200, 88, 53, 131, 240, 205, 13, 0, 0,
    /*3*/ 276, 87, 0, 275, 199, 153, 56, 132, 305, 231, 341, 212, 304, 300, 271, 39, 357, 1, 0,
    /*4*/ 332, 181, 0,
    /*5*/ 195, 14, 115, 166, 241, 51, 157, 0,
    /*6*/ 278, 257, 1, 351, 92, 253, 18, 225, 0,
    /*7*/ 9, 62, 316, 333, 290, 114, 0,
    /*8*/ 307, 179, 165, 18, 39, 224, 368, 67, 170, 0,
    /*9*/ 366, 232, 321, 133, 57, 303, 63, 82, 0,
    /*10*/ 101, 339, 274, 111, 383, 354, 0,
    /*11*/ 48, 102, 8, 47, 188, 334, 115, 0,
    /*12*/ 77, 186, 174, 232, 50, 74, 0,
    /*13*/ 313, 177, 266, 115, 370, 0,
    /*14*/ 142, 248, 137, 89, 347, 12, 0,
    /*15*/ 241, 2, 210, 318, 55, 269, 0,
    /*16*/ 13, 338, 57, 289, 57, 0,
    /*17*/ 260, 303, 81, 358, 375, 0,
    /*18*/ 130, 163, 280, 132, 4, 0,
    /*19*/ 145, 213, 344, 242, 197, 0,
    /*20*/ 187, 206, 264, 341, 59, 0,
    /*21*/ 205, 102, 328, 213, 97, 0,
    /*22*/ 30, 11, 233, 22, 0,
    /*23*/ 24, 89, 61, 27, 0,
    /*24*/ 298, 158, 235, 339, 234, 0,
    /*25*/ 72, 17, 383, 312, 0,
    /*26*/ 71, 81, 76, 136, 0,
    /*27*/ 194, 194, 101, 0,
    /*28*/ 222, 19, 244, 274, 0,
    /*29*/ 252, 5, 147, 78, 0,
    /*30*/ 159, 229, 260, 90, 0,
    /*31*/ 100, 215, 258, 256, 0,
    /*32*/ 102, 201, 175, 287, 0,
    /*33*/ 323, 8, 361, 105, 0,
    /*34*/ 230, 148, 202, 312, 0,
    /*35*/ 320, 335, 2, 266, 0,
    /*36*/ 210, 313, 297, 21, 0,
    /*37*/ 269, 82, 115, 0,
    /*38*/ 185, 177, 289, 214, 0,
    /*39*/ 258, 93, 346, 297, 0,
    /*40*/ 175, 37, 312, 0,
    /*41*/ 52, 314, 139, 288, 0,
    /*42*/ 113, 14, 218, 0,
    /*43*/ 113, 132, 114, 168, 0,
    /*44*/ 80, 78, 163, 274, 0,
    /*45*/ 135, 149, 15, 0,
  ),

  BG2ShiftValue: Seq[Int] = Seq(
    /*0*/ 174, 97, 166, 66, 71, 172, 0, 0,
    /*1*/ 27, 36, 48, 92, 31, 187, 185, 3, 0, 0,
    /*2*/ 25, 114, 117, 110, 114, 1, 0, 0,
    /*3*/ 136, 175, 113, 72, 123, 118, 28, 186, 0, 0,
    /*4*/ 72, 74, 29, 0,
    /*5*/ 10, 44, 121, 80, 48, 0,
    /*6*/ 129, 92, 100, 49, 184, 0,
    /*7*/ 80, 186, 16, 102, 143, 0,
    /*8*/ 118, 70, 152, 0,
    /*9*/ 28, 132, 185, 178, 0,
    /*10*/ 59, 104, 22, 52, 0,
    /*11*/ 32, 92, 174, 154, 0,
    /*12*/ 39, 93, 11, 0,
    /*13*/ 49, 125, 35, 166, 0,
    /*14*/ 19, 118, 21, 163, 0,
    /*15*/ 68, 63, 81, 0,
    /*16*/ 87, 177, 135, 64, 0,
    /*17*/ 158, 23, 9, 6, 0,
    /*18*/ 186, 6, 46, 0,
    /*19*/ 58, 42, 156, 0,
    /*20*/ 76, 61, 153, 0,
    /*21*/ 157, 175, 67, 0,
    /*22*/ 20, 52, 0,
    /*23*/ 106, 86, 95, 0,
    /*24*/ 182, 153, 64, 0,
    /*25*/ 45, 21, 0,
    /*26*/ 67, 137, 55, 85, 0,
    /*27*/ 103, 50, 0,
    /*28*/ 70, 111, 168, 0,
    /*29*/ 110, 17, 0,
    /*30*/ 120, 154, 52, 56, 0,
    /*31*/ 3, 170, 0,
    /*32*/ 84, 8, 17, 0,
    /*33*/ 165, 179, 124, 0,
    /*34*/ 173, 177, 12, 0,
    /*35*/ 77, 184, 18, 0,
    /*36*/ 25, 151, 170, 0,
    /*37*/ 37, 31, 0,
    /*38*/ 84, 151, 190, 0,
    /*39*/ 93, 132, 57, 0,
    /*40*/ 103, 107, 163, 0,
    /*41*/ 147, 7, 60, 0,
  ),

  BG1ColIdx: Seq[Int] = Seq(
    /*0 */ 0, 1, 2, 3, 5, 6, 9, 10, 11, 12, 13, 15, 16, 18, 19, 20, 21, 22, 23,
    /*1 */ 0, 2, 3, 4, 5, 7, 8, 9, 11, 12, 14, 15, 16, 17, 19, 21, 22, 23, 24,
    /*2 */ 0, 1, 2, 4, 5, 6, 7, 8, 9, 10, 13, 14, 15, 17, 18, 19, 20, 24, 25,
    /*3 */ 0, 1, 3, 4, 6, 7, 8, 10, 11, 12, 13, 14, 16, 17, 18, 20, 21, 22, 25,
    /*4 */ 0, 1, 26,
    /*5 */ 0, 1, 3, 12, 16, 21, 22, 27,
    /*6 */ 0, 6, 10, 11, 13, 17, 18, 20, 28,
    /*7 */ 0, 1, 4, 7, 8, 14, 29,
    /*8 */ 0, 1, 3, 12, 16, 19, 21, 22, 24, 30,
    /*9 */ 0, 1, 10, 11, 13, 17, 18, 20, 31,
    /*10*/ 1, 2, 4, 7, 8, 14, 32,
    /*11*/ 0, 1, 12, 16, 21, 22, 23, 33,
    /*12*/ 0, 1, 10, 11, 13, 18, 34,
    /*13*/ 0, 3, 7, 20, 23, 35,
    /*14*/ 0, 12, 15, 16, 17, 21, 36,
    /*15*/ 0, 1, 10, 13, 18, 25, 37,
    /*16*/ 1, 3, 11, 20, 22, 38,
    /*17*/ 0, 14, 16, 17, 21, 39,
    /*18*/ 1, 12, 13, 18, 19, 40,
    /*19*/ 0, 1, 7, 8, 10, 41,
    /*20*/ 0, 3, 9, 11, 22, 42,
    /*21*/ 1, 5, 16, 20, 21, 43,
    /*22*/ 0, 12, 13, 17, 44,
    /*23*/ 1, 2, 10, 18, 45,
    /*24*/ 0, 3, 4, 11, 22, 46,
    /*25*/ 1, 6, 7, 14, 47,
    /*26*/ 0, 2, 4, 15, 48,
    /*27*/ 1, 6, 8, 49,
    /*28*/ 0, 4, 19, 21, 50,
    /*29*/ 1, 14, 18, 25, 51,
    /*30*/ 0, 10, 13, 24, 52,
    /*31*/ 1, 7, 22, 25, 53,
    /*32*/ 0, 12, 14, 24, 54,
    /*33*/ 1, 2, 11, 21, 55,
    /*34*/ 0, 7, 15, 17, 56,
    /*35*/ 1, 6, 12, 22, 57,
    /*36*/ 0, 14, 15, 18, 58,
    /*37*/ 1, 13, 23, 59,
    /*38*/ 0, 9, 10, 12, 60,
    /*39*/ 1, 3, 7, 19, 61,
    /*40*/ 0, 8, 17, 62,
    /*41*/ 1, 3, 9, 18, 63,
    /*42*/ 0, 4, 24, 64,
    /*43*/ 1, 16, 18, 25, 65,
    /*44*/ 0, 7, 9, 22, 66,
    /*45*/ 1, 6, 10, 67,
  ),
  val BG1isLastCol: Seq[Boolean] = Seq(
    /* 0*/ false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true,
    /* 1*/ false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true,
    /* 2*/ false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true,
    /* 3*/ false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true,
    /* 4*/ false, false, true,
    /* 5*/ false, false, false, false, false, false, false, true,
    /* 6*/ false, false, false, false, false, false, false, false, true,
    /* 7*/ false, false, false, false, false, false, true,
    /* 8*/ false, false, false, false, false, false, false, false, false, true,
    /* 9*/ false, false, false, false, false, false, false, false, true,
    /*10*/ false, false, false, false, false, false, true,
    /*11*/ false, false, false, false, false, false, false, true,
    /*12*/ false, false, false, false, false, false, true,
    /*13*/ false, false, false, false, false, true,
    /*14*/ false, false, false, false, false, false, true,
    /*15*/ false, false, false, false, false, false, true,
    /*16*/ false, false, false, false, false, true,
    /*17*/ false, false, false, false, false, true,
    /*18*/ false, false, false, false, false, true,
    /*19*/ false, false, false, false, false, true,
    /*20*/ false, false, false, false, false, true,
    /*21*/ false, false, false, false, false, true,
    /*22*/ false, false, false, false, true,
    /*23*/ false, false, false, false, true,
    /*24*/ false, false, false, false, false, true,
    /*25*/ false, false, false, false, true,
    /*26*/ false, false, false, false, true,
    /*27*/ false, false, false, true,
    /*28*/ false, false, false, false, true,
    /*29*/ false, false, false, false, true,
    /*30*/ false, false, false, false, true,
    /*31*/ false, false, false, false, true,
    /*32*/ false, false, false, false, true,
    /*33*/ false, false, false, false, true,
    /*34*/ false, false, false, false, true,
    /*35*/ false, false, false, false, true,
    /*36*/ false, false, false, false, true,
    /*37*/ false, false, false, true,
    /*38*/ false, false, false, false, true,
    /*39*/ false, false, false, false, true,
    /*40*/ false, false, false, true,
    /*41*/ false, false, false, false, true,
    /*42*/ false, false, false, true,
    /*43*/ false, false, false, false, true,
    /*44*/ false, false, false, false, true,
    /*45*/ false, false, false, true,
  ),
  BG2ColIdx: Seq[Int] = Seq(
    /*0 */ 0, 1, 2, 3, 6, 9, 10, 11,
    /*1 */ 0, 3, 4, 5, 6, 7, 8, 9, 11, 12,
    /*2 */ 0, 1, 3, 4, 8, 10, 12, 13,
    /*3 */ 1, 2, 4, 5, 6, 7, 8, 9, 10, 13,
    /*4 */ 0, 1, 11, 14,
    /*5 */ 0, 1, 5, 7, 11, 15,
    /*6 */ 0, 5, 7, 9, 11, 16,
    /*7 */ 1, 5, 7, 11, 13, 17,
    /*8 */ 0, 1, 12, 18,
    /*9 */ 1, 8, 10, 11, 19,
    /*10*/ 0, 1, 6, 7, 20,
    /*11*/ 0, 7, 9, 13, 21,
    /*12*/ 1, 3, 11, 22,
    /*13*/ 0, 1, 8, 13, 23,
    /*14*/ 1, 6, 11, 13, 24,
    /*15*/ 0, 10, 11, 25,
    /*16*/ 1, 9, 11, 12, 26,
    /*17*/ 1, 5, 11, 12, 27,
    /*18*/ 0, 6, 7, 28,
    /*19*/ 0, 1, 10, 29,
    /*20*/ 1, 4, 11, 30,
    /*21*/ 0, 8, 13, 31,
    /*22*/ 1, 2, 32,
    /*23*/ 0, 3, 5, 33,
    /*24*/ 1, 2, 9, 34,
    /*25*/ 0, 5, 35,
    /*26*/ 2, 7, 12, 13, 36,
    /*27*/ 0, 6, 37,
    /*28*/ 1, 2, 5, 38,
    /*29*/ 0, 4, 39,
    /*30*/ 2, 5, 7, 9, 40,
    /*31*/ 1, 13, 41,
    /*32*/ 0, 5, 12, 42,
    /*33*/ 2, 7, 10, 43,
    /*34*/ 0, 12, 13, 44,
    /*35*/ 1, 5, 11, 45,
    /*36*/ 0, 2, 7, 46,
    /*37*/ 10, 13, 47,
    /*38*/ 1, 5, 11, 48,
    /*39*/ 0, 7, 12, 49,
    /*40*/ 2, 10, 13, 50,
    /*41*/ 1, 5, 11, 51,
  ),
  val BG2isLastCol: Seq[Boolean] = Seq(
    /* 0*/ false, false, false, false, false, false, false, true,
    /* 1*/ false, false, false, false, false, false, false, false, false, true,
    /* 2*/ false, false, false, false, false, false, false, true,
    /* 3*/ false, false, false, false, false, false, false, false, false, true,
    /* 4*/ false, false, false, true,
    /* 5*/ false, false, false, false, false, true,
    /* 6*/ false, false, false, false, false, true,
    /* 7*/ false, false, false, false, false, true,
    /* 8*/ false, false, false, true,
    /* 9*/ false, false, false, false, true,
    /*10*/ false, false, false, false, true,
    /*11*/ false, false, false, false, true,
    /*12*/ false, false, false, true,
    /*13*/ false, false, false, false, true,
    /*14*/ false, false, false, false, true,
    /*15*/ false, false, false, true,
    /*16*/ false, false, false, false, true,
    /*17*/ false, false, false, false, true,
    /*18*/ false, false, false, true,
    /*19*/ false, false, false, true,
    /*20*/ false, false, false, true,
    /*21*/ false, false, false, true,
    /*22*/ false, false, true,
    /*23*/ false, false, false, true,
    /*24*/ false, false, false, true,
    /*25*/ false, false, true,
    /*26*/ false, false, false, false, true,
    /*27*/ false, false, true,
    /*28*/ false, false, false, true,
    /*29*/ false, false, true,
    /*30*/ false, false, false, false, true,
    /*31*/ false, false, true,
    /*32*/ false, false, false, true,
    /*33*/ false, false, false, true,
    /*34*/ false, false, false, true,
    /*35*/ false, false, false, true,
    /*36*/ false, false, false, true,
    /*37*/ false, false, true,
    /*38*/ false, false, false, true,
    /*39*/ false, false, false, true,
    /*40*/ false, false, false, true,
    /*41*/ false, false, false, true,
  )
)

case object DebugOptionsKey extends Field[DebugOptions]

case class DebugOptions
(
  FPGAPlatform: Boolean = false,
  EnableDebug: Boolean = false,
  EnablePerfDebug: Boolean = true,
)

trait HasDecParameter{
  implicit val p: Parameters
  val BG1RowNum = 46
  val BG1ColNum = 68
  val BG2RowNum = 42
  val BG2ColNum = 52
  def isBG1 = p(DecParamsKey).isBG1
  def MaxZSize = p(DecParamsKey).MaxZSize
  def LLRBits = p(DecParamsKey).LLRBits
  def LayerNum = if(isBG1) BG1RowNum else BG2RowNum
  def ColNum = if(isBG1) BG1ColNum else BG2ColNum
  def ColIdxOrder = if(isBG1) p(DecParamsKey).BG1ColIdx else p(DecParamsKey).BG2ColIdx
  def IsLastCol = if(isBG1) p(DecParamsKey).BG1isLastCol else p(DecParamsKey).BG2isLastCol
  def DelayOfShifter = p(DecParamsKey).DelayOfShifter
  def DelayOfVNU = p(DecParamsKey).DelayOfVNU
  def DelayOfCNU = p(DecParamsKey).DelayOfCNU
  def NumAtLayer = if(isBG1) p(DecParamsKey).BG1NumAtLayer else p(DecParamsKey).BG2NumAtLayer
  def ShiftValue = if(isBG1) p(DecParamsKey).BG1ShiftValue else p(DecParamsKey).BG2ShiftValue
  def MaxDegreeOfCNU = p(DecParamsKey).MaxDegreeOfCNU
  def C2VRowMsgBits = 1 + 2 * (LLRBits-1) + log2Ceil(MaxDegreeOfCNU)
}