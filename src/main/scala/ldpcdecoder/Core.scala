package ldpcdecoder

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config
import org.chipsalliance.cde.config.Parameters

abstract class DecModule(implicit val p: Parameters) extends Module
    with HasDecParameter

abstract class DecBundle(implicit val p: Parameters) extends Bundle
    with HasDecParameter
