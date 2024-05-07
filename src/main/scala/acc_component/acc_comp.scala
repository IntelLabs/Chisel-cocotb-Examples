/* 
SPDX-License-Identifier: Apache-2.0

MUL, Adder, and DotProduct components are modified from https://github.com/pasqoc/incubator-tvm-vta
MemMaskedSinglePort component is modified from https://www.chisel-lang.org/docs/explanations/memories
*/


package acc_component

import chisel3._
import chisel3.util._
import scala.math.pow
import chisel3.util.experimental.BoringUtils


//Multiplier Unit; The unit is combinational
class MUL(val aBits: Int = 8, val bBits: Int = 8) extends Module {
  assert (aBits >= 2)
  assert (bBits >= 2)

  val outBits = aBits + bBits

  val io = IO(new Bundle {
    val a = Input(SInt(aBits.W))
    val b = Input(SInt(bBits.W))
    val y = Output(SInt(outBits.W))
  })

  io.y := io.a * io.b
}

// One adder unit, combinational unit
class Adder(val aBits: Int = 8, val bBits: Int = 8) extends Module {
  assert (aBits >= 2)
  assert (bBits >= 2)

  val outBits = Math.max(aBits, bBits) + 1

  val io = IO(new Bundle {
    val a = Input(SInt(aBits.W))
    val b = Input(SInt(bBits.W))
    val y = Output(SInt(outBits.W))
  })

  io.y := io.a +& io.b
}

// DotProduct module. fully combinational, the output is flopped only
class DotProduct(val aBits: Int = 8, val bBits: Int = 8, val InSize: Int) extends Module {
  val errorMsg =
    s"\n\n DotProduct InSize must be greater than 2 and a power of 2\n\n"   
  require(InSize >= 2 && isPow2(InSize), errorMsg)
  
  val b = aBits + bBits
  val outBits = b + log2Ceil(InSize) + 1

  val io = IO(new Bundle {
    val a = Input(Vec(InSize, SInt(aBits.W)))   //InSize is the length of input data sequences
    val b = Input(Vec(InSize, SInt(bBits.W)))
    val y = Output(SInt(outBits.W))
  })

  val y = Reg(SInt(outBits.W))   //flopping the final output sum only

  val s = Seq.tabulate(log2Ceil(InSize + 1))(i => pow(2, log2Ceil(InSize) - i).toInt)     // # of total stages (i.e., MUL stage + all adder tree stages)
  val p = log2Ceil(InSize / 2) + 1                                                        // # of adder stages
  //printf(p"DotProduct: value of s = $s\n")
  //printf(p"DotProduct: value of p = $p\n")

  val m = Seq.fill(s(0))(Module(new MUL(aBits, bBits)))                                   // # of total vector pairs
  
  val a = Seq.tabulate(p)(
    i => Seq.fill(s(i + 1))(Module(new Adder(aBits = (b + i + 1), bBits = (b + i + 1))))
    )        // # adders within each layer

  // Vector MULs
  for (i <- 0 until s(0)) {
    m(i).io.a := io.a(i)
    m(i).io.b := io.b(i)
  }

  // Adder tree reduction
  for (i <- 0 until p) {
    for (j <- 0 until s(i + 1)) {
      if (i == 0) {
        // First stage of Adders
        a(i)(j).io.a := m(2 * j).io.y
        a(i)(j).io.b := m(2 * j + 1).io.y
      } else {
        a(i)(j).io.a := a(i - 1)(2 * j).io.y
        a(i)(j).io.b := a(i - 1)(2 * j + 1).io.y
      }
    }
  }

  // last adder
  y := a(p - 1)(0).io.y
  io.y := y

}

//Masked memory with a single port for read and write
class MemMaskedSinglePort(val bBits: Int = 8, val InSize: Int, val neurons: Int) extends Module {
  
  val addrBW = log2Ceil(neurons)   // calculating the address bitwidth
  val maskSize = InSize + 1        // number of data to read/write using the port (+1 for bias data)

  val io = IO(new Bundle {
    val write_enable = Input(Bool())
    val mask = Input(Vec(maskSize, Bool()))
    val addr = Input(UInt(addrBW.W))
    val dataIn = Input(Vec(maskSize, SInt(bBits.W)))
    val dataOut = Output(Vec(maskSize, SInt(bBits.W)))

  })

  val mem = SyncReadMem(neurons, Vec(maskSize, SInt(bBits.W)))

  io.dataOut := DontCare

  val rwPort = mem(io.addr)

  when (io.write_enable) {
    for (i <- 0 until maskSize) {
      when (io.mask(i)) {
        rwPort(i) := io.dataIn(i)    // one cycles to perform all the write
      }
    }
  }.otherwise {
    io.dataOut := rwPort             // reading the full maskSize vector in one cycle
  }

}


// Generate Verilog
object MainMUL extends App {
  emitVerilog(new MUL(aBits = 16, bBits = 16))
}

object MainAdder extends App {
  emitVerilog(new Adder(aBits = 8, bBits = 8))
}

object MainDotProduct extends App {
  emitVerilog(new DotProduct(aBits = 8, bBits = 8, InSize = 4),
                  Array("--emission-options=disableMemRandomization,disableRegisterRandomization")
  )
}
