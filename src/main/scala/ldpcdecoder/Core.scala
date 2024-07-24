package ldpcdecoder

import org.chipsalliance.cde.config
import org.chipsalliance.cde.config.Parameters
import chisel3._
import chisel3.util._


abstract class DecModule(implicit val p: Parameters) extends Module 
    with HasDecParameter

abstract class DecBundle(implicit val p: Parameters) extends Bundle 
    with HasDecParameter