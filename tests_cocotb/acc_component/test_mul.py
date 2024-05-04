# Copyright (c) 2024 Intel Corporation
# SPDX-License-Identifier: Apache-2.0


from util import run, unique_test_id, generate_verilog, random_data_signed
from pathlib import Path
import json

import cocotb
from cocotb.clock import Clock
from cocotb.triggers import RisingEdge, FallingEdge, Timer, Combine, Join
import math
import os

# Top level wrapper
def test_mul_chisel():
    test_id = "MUL"
    
    # parameters of the dut
    paramdict = {'aBits': 4, 'bBits': 4}  
    paramstr = json.dumps(paramdict)
    params = {'param': paramstr}   # param values need to be string to be able to pass through run as extra_env
    
    cocotb_scala_dir = Path(__file__).parent.parent.parent / 'src' / 'main' / 'scala' / 'acc_component' / 'cocotb_scala_dir'
    print("cocotb_scala_dir = ", cocotb_scala_dir)  
    cocotb_scala_dir.mkdir(exist_ok=True)

    test_folder = test_id
    emitVer_filename = f'emitVerilog_{test_id}.scala'
    emitVer_objname = f'obj{test_id}'
    package_name = 'acc_component'

    with open(cocotb_scala_dir/emitVer_filename, 'w') as fp:
        fp.write(f'package {package_name}\n')
        fp.write('import chisel3._\n')
        fp.write('import chisel3.util._\n')
        fp.write(f'''object {emitVer_objname} extends App {{emitVerilog(
                        new MUL(aBits = {paramdict['aBits']}, bBits = {paramdict['bBits']}),
                        Array("--target-dir=test_cocotb_ver_dir/{test_folder}",
                        "--emission-options=disableMemRandomization,disableRegisterRandomization"))}}''')
    
    #Generate Verilog
    generate_verilog(package_name, emitVer_objname)
    
    # Run cocotb tester
    test_verilog_dir = Path(__file__).parent.parent.parent / 'test_cocotb_ver_dir' / test_folder
    print("test_verilog_dir = ", test_verilog_dir)

    run(verilog_sources=[test_verilog_dir / "MUL.v"],
        toplevel="MUL",
        module="test_mul",
        extra_env = params)
    

# cocotb tester
@cocotb.test()
async def mul_tester(dut):

    print(dut.__dict__)    

    clock = Clock(dut.clock, 1, units="ns")  # 1ns period clock on port clock
    cocotb.start_soon(clock.start())         # Start the clock

    #parameters of the dut
    paramstr = os.environ.get('param')
    params = json.loads(paramstr)
    aBits = params['aBits']
    bBits = params['bBits']
    print(f'parameters of the dut: aBits = {aBits}, bBits = {bBits}')

    #creating random input data for signed integers
    sample_size = 10
    a_data = random_data_signed(aBits, sample_size, 37)
    b_data = random_data_signed(bBits, sample_size, 49)
    print(f"{a_data = }")
    print(f"{b_data = }")

    #calculating reference output
    out_data = []
    for i in range(sample_size):
        ref_out = a_data[i] * b_data[i]
        out_data.append(ref_out)
    print(f"{out_data = }")

    # verification of the dut
    dut.reset.value = 1
    await FallingEdge(dut.clock)  # Synchronize with the clock

    dut.reset.value = 0
    await FallingEdge(dut.clock)

    for i in range(sample_size):
        dut.io_a.value = a_data[i]
        dut.io_b.value = b_data[i]

        await Timer(0.01, 'ns')    #The above assignments to input ports are not immediately applied. Hence, needed this timer await

        #dut._log.info(f"dut.io_a.value = {dut.io_a.value}")
        #dut._log.info(f"dut.io_b.value = {dut.io_b.value}")
        dut._log.info(f"dut.io_y.value = {dut.io_y.value.signed_integer}") 

        assert dut.io_y.value.signed_integer == out_data[i]   # io.y value is interpreted as signed integer
        await FallingEdge(dut.clock)
