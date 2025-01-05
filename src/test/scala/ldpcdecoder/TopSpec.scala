// package ldpcdecoder

// import chisel3._
// import chisel3.util._
// import chiseltest._
// import scala.util.Random
// import org.scalatest.flatspec.AnyFlatSpec
// import org.scalatest.matchers.should.Matchers
// import top.DefaultConfig
// import top.ArgParser

// import java.nio.file.{Paths, Files}
// import java.nio.charset.StandardCharsets

// import ldpcdecoder._

// class LDPCDecoderTopSpec extends AnyFlatSpec with ChiselScalatestTester {

//   "LDPCDecoderTop" should "correctly decode the llrIn" in {
//     val defaultConfig = (new DefaultConfig)
//     implicit val config = defaultConfig.alterPartial({
//         case DecParamsKey => defaultConfig(DecParamsKey)
//     })

//     val MaxZSize: Int = 384
//     val LLRBits: Int = 6

//     val filePath = "/nfs/home/pengxiao/Projects/LDPC-Decoder/llrIn.txt"
    
//     val lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8)
    
//     val llrIn = lines.asScala.map(_.toInt).toList

//     test(new LDPCDecoderTop()) { c =>
//         c.io.zSize.poke(384.U)
//         for(i <- 0 until MaxZSize){
//           c.io.llrIn(i).poke(llrIn(i).U)
//         }
        
//     }

    
//   }
// }