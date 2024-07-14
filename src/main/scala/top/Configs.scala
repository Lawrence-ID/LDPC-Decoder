package top

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config._
import ldpcdec._

class BaseConfig(n: Int = 1) extends Config((site, here, up)=>{
    case DecParamsKey => DecParameters()
    case DebugOptionsKey => DebugOptions()
})

class DefaultConfig(n: Int = 1) extends Config(
    new BaseConfig(n)
)