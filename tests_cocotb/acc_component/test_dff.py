# This file is public domain, it can be freely copied without restrictions.
# SPDX-License-Identifier: CC0-1.0

# Adapted from https://github.com/themperek/cocotb-test


from util import run
from pathlib import Path
import cocotb
from cocotb.clock import Clock
from cocotb.triggers import FallingEdge

# calling the tester
tests_dir = Path(__file__).parent
def test_dff_verilog():
    run(verilog_sources=[tests_dir / "dff.sv"],
        toplevel="dff_test",
        module="test_dff")


# cocotb tester
@cocotb.test()
async def dff_test(dut):
    """ Test that d propagates to q """

    clock = Clock(dut.clk, 1, units="ns")  # Create a 1ns period clock on port clk
    cocotb.start_soon(clock.start())       # Start the clock

    dut.reset.value = 1
    await FallingEdge(dut.clk)  # Synchronize with the clock

    for i in range(1,11):
        dut.reset.value = 0
        await FallingEdge(dut.clk)
        assert dut.q.value == i, f"output q was incorrect on the {i}th cycle"