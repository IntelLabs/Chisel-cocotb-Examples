/* 
SPDX-License-Identifier: Apache-2.0

MUL, Adder, and DotProduct components are modified from https://github.com/pasqoc/incubator-tvm-vta
MemMaskedSinglePort component is modified from https://www.chisel-lang.org/docs/explanations/memories
*/


package acc_component

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

import testutil._

import scala.collection.mutable.Set
import scala.util.Random

class MULTester(tag: String, factory : () => MUL) extends AnyFreeSpec with ChiselScalatestTester with TestParams {

    s"$tag MUL testing" in {
        test(factory()).withAnnotations(annons) { dut =>

            dut.io.a.poke(10.S)
            dut.io.b.poke(-2.S)
            dut.io.y.expect(-20.S)
            dut.clock.step()

            dut.io.a.poke(512.S)
            dut.io.b.poke(128.S)
            dut.io.y.expect(65536.S)
            dut.clock.step()

            dut.io.a.poke(-7.S)
            dut.io.b.poke(-3.S)
            dut.io.y.expect(21.S)
            

        }
    }
}

class AdderTester(tag: String, factory : () => Adder) extends AnyFreeSpec with ChiselScalatestTester with TestParams {

    s"$tag Adder testing" in {
        test(factory()).withAnnotations(annons) { dut =>
            
            dut.io.a.poke(10.S)
            dut.io.b.poke(-2.S)
            dut.io.y.expect(8.S)
            dut.clock.step()

            dut.io.a.poke(512.S)
            dut.io.b.poke(128.S)
            dut.io.y.expect(640.S)
            dut.clock.step()

            dut.io.a.poke(-7.S)
            dut.io.b.poke(-3.S)
            dut.io.y.expect(-10.S)

        }
    }
}

class DotProductTester(tag: String, factory : () => DotProduct) extends AnyFreeSpec with ChiselScalatestTester with TestParams {

    s"$tag DotProduct testing" in {
        test(factory()).withAnnotations(annons) { dut =>
            
            val rnd = new scala.util.Random()   
            rnd.setSeed(47L)
                 
	        val a_min = (math.pow(2, dut.aBits-1)).toInt  //min of SInt data
            println(s"print during testing: a_min = $a_min\n")
            val a_array = Seq.fill(dut.InSize)(BigInt(dut.io.a(0).getWidth, rnd) - a_min)  // creating random numbers between (2^(aBits-1) - 1) to (- a_min)
            println(s"a_array = $a_array\n")

            val b_min = (math.pow(2, dut.bBits-1)).toInt  //min of SInt data
            val b_array = Seq.fill(dut.InSize)(BigInt(dut.io.b(0).getWidth, rnd) - b_min)
            println(s"b_array = $b_array\n")

            var refsum:BigInt = 0
            for (i <- 0 until dut.InSize){
                refsum = refsum + a_array(i) * b_array(i)
            }
            println(s"refsum = $refsum\n")

            for (i <- 0 until dut.InSize){
                dut.io.a(i).poke(a_array(i).S)
                dut.io.b(i).poke(b_array(i).S)
            }
            dut.clock.step()
            dut.io.y.expect(refsum.S)
            println(s" io.y = ${dut.io.y.peek()}")
            
        }
    }
}


class MemMaskedSinglePortTester(tag: String, factory : () => MemMaskedSinglePort) extends AnyFreeSpec with ChiselScalatestTester with TestParams {

    s"$tag MemMaskedSinglePort testing one write one read" in {
        test(factory()).withAnnotations(annons) { dut =>
            
            println(s"testing one write one read")
            val rnd = new scala.util.Random()   
            rnd.setSeed(47L)

            val maskSize = dut.InSize + 1

            // initializing all the ports
            dut.reset.poke(true.B)
            dut.io.write_enable.poke(false.B)
            for (i<-0 until maskSize) {
                dut.io.mask(i).poke(false.B)
            }
            dut.clock.step()

            dut.reset.poke(false.B)
            dut.clock.step()

            // performing write
            dut.io.write_enable.poke(true.B)
            //dut.clock.step()
            for (i<-0 until maskSize){
                dut.io.mask(i).poke(true.B)    //setting all mask bit true at once
            }
            
            val addr_wr = 2
            dut.io.addr.poke(addr_wr.U)        //writing in the specified address

            val b_min = (math.pow(2, dut.bBits-1)).toInt  //min of SInt data
            val b_array = Seq.fill(maskSize)(BigInt(dut.io.dataIn(0).getWidth, rnd) - b_min)  // creating random numbers between (2^(bBits-1) - 1) to (- b_min)
            println(s"b_array = $b_array\n")

            for (i<-0 until maskSize){
                dut.io.dataIn(i).poke(b_array(i).S)
            }
            dut.clock.step()                   //write is done by this point

            dut.io.write_enable.poke(false.B)
            dut.clock.step()

            //testing read
            dut.io.addr.poke(addr_wr.U)       //reading from the specified address
            dut.clock.step()

            println(s" io.dataOut = ${dut.io.dataOut.peek()}")
            for (i<-0 until maskSize) {
                dut.io.dataOut(i).expect(b_array(i).S)
                
            }

        }
    }

    
    s"$tag MemMaskedSinglePort testing write and read for all addresses" in {
        test(factory()).withAnnotations(annons) { dut =>
            
            println(s"testing write and read for all addresses")
            val rnd = new scala.util.Random()   
            rnd.setSeed(47L)

            val maskSize = dut.InSize + 1

            // initializing all the ports
            dut.reset.poke(true.B)
            dut.io.write_enable.poke(false.B)
            for (i<-0 until maskSize) {
                dut.io.mask(i).poke(false.B)
            }
            dut.clock.step()

            dut.reset.poke(false.B)
            dut.clock.step()

            // performing write
            dut.io.write_enable.poke(true.B)
            for (i<-0 until maskSize){
                dut.io.mask(i).poke(true.B)    //setting all mask bit true at once
            }

            // creating the 2D weight matrix
            val b_min = (math.pow(2, dut.bBits-1)).toInt  //min of SInt data
            val b_mat = Seq.fill(dut.neurons)(Seq.fill(maskSize)(BigInt(dut.io.dataIn(0).getWidth, rnd) - b_min))  // creating random numbers between (2^(bBits-1) - 1) to (- b_min)
            println(s"b_mat = $b_mat\n")


            for (j<-0 until dut.neurons) {
                println(s"writing at address = $j\n")
                dut.io.addr.poke(j.U)  

                for (i<-0 until maskSize){
                    dut.io.dataIn(i).poke(b_mat(j)(i).S)
                }

                dut.clock.step()            
            }

            // write_enable becomes fasle at the negative clock edge. On the same cycle, read occurs at the positive clock edge. 
            dut.io.write_enable.poke(false.B)
            dut.clock.step()

            //testing read
            for (j<-0 until dut.neurons) {
                println(s"reading from address = $j\n")
                dut.io.addr.poke(j.U)
                dut.clock.step()       // for SycnReadMem the address become avilable after a clock step

                println(s" io.dataOut = ${dut.io.dataOut.peek()}")
                for (i<-0 until maskSize) {
                    dut.io.dataOut(i).expect(b_mat(j)(i).S)                
                }                
            }            

        }
    }
    

}


// Manual testers: The numbers in the tester may need to be changed depending on the passed bitwidth parameters
class MULTest extends MULTester("MUL", () => new MUL(aBits = 16, bBits = 16))
class AdderTest extends AdderTester("Adder", () => new Adder(aBits = 16, bBits = 16))
// Generic random testers
class DotProductTest extends DotProductTester("DotProduct", () => new DotProduct(aBits = 16, bBits = 16, InSize = 32))
class MemMaskedSinglePortTest extends MemMaskedSinglePortTester("MemMaskedSinglePort", () => new MemMaskedSinglePort(bBits = 8, InSize = 4, neurons = 4))
