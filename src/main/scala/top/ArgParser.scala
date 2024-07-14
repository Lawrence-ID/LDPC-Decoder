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

package top

import chisel3._

import scala.annotation.tailrec
import scala.sys.exit
import chisel3.util.log2Up

import org.chipsalliance.cde.config.{Config, Parameters}
import ldpcdecoder._

object ArgParser {
  val usage =
    """
      |Options
      |--help                  print this help message
      |--config <ConfigClassName>
      |--llr-bits
      |--fpga-platform
      |--enable-log
      |""".stripMargin

  def getConfigByName(confString: String): Parameters = {
    var prefix = "top." // default package is 'top'
    if (confString.contains('.')) { // already a full name
      prefix = ""
    }
    val c = Class.forName(prefix + confString).getConstructor(Integer.TYPE)
    c.newInstance(1.asInstanceOf[Object]).asInstanceOf[Parameters]
  }

  def parse(args: Array[String]): (Parameters, Array[String], Array[String]) = {
    val default = new DefaultConfig(1)
    var firrtlOpts = Array[String]()
    var firtoolOpts = Array[String]()

    @tailrec
    def nextOption(config: Parameters, list: List[String]): Parameters = {
      list match {
        case Nil => config
        case "--help" :: tail =>
          if(tail == Nil) exit(0)
          nextOption(config, tail)
        case "--config" :: confString :: tail =>
          nextOption(getConfigByName(confString), tail)
        case "--llr-bits" :: value :: tail =>
          nextOption(config.alter((site, here, up) => {
            case DecParamsKey => up(DecParamsKey).copy(LLRBits = value.toInt)
          }), tail)
        case "--enable-log" :: tail =>
          nextOption(config.alter((site, here, up) => {
            case DebugOptionsKey => up(DebugOptionsKey).copy(EnableDebug = true)
          }), tail)
        case "--firtool-opt" :: option :: tail =>
          firtoolOpts ++= option.split(" ").filter(_.nonEmpty)
          nextOption(config, tail)
        case "--target-dir" :: dir :: tail =>
          firrtlOpts :+= "--target-dir"
          firrtlOpts :+= dir
          nextOption(config, tail)
        case "--target" :: target :: tail =>
          firrtlOpts :+= "--target"
          firrtlOpts :+= target
          nextOption(config, tail)
        case option :: tail =>
          // unknown option, maybe other firrtl option, skip
          firrtlOpts :+= option
          nextOption(config, tail)
      }
    }
    var config = nextOption(default, args.toList)
    (config, firrtlOpts, firtoolOpts)
  }
}