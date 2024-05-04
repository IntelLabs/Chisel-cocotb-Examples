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
def test_dotproduct_chisel():
    test_id = "DotProduct"
    
    # parameters of the dut
    paramdict = {'aBits': 4, 'bBits': 4, 'InSize': 8}  
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
                        new DotProduct(aBits = {paramdict['aBits']}, bBits = {paramdict['bBits']}, InSize = {paramdict['InSize']}),
                        Array("--target-dir=test_cocotb_ver_dir/{test_folder}",
                        "--emission-options=disableMemRandomization,disableRegisterRandomization"))}}''')
    
    #Generate Verilog
    generate_verilog(package_name, emitVer_objname)
    
    # Run cocotb tester
    test_verilog_dir = Path(__file__).parent.parent.parent / 'test_cocotb_ver_dir' / test_folder
    print("test_verilog_dir = ", test_verilog_dir)

    run(verilog_sources=[test_verilog_dir / "DotProduct.v"],
        toplevel="DotProduct",
        module="test_dotproduct",
        extra_env = params)
    

# cocotb tester
@cocotb.test()
async def dotproduct_tester(dut):

    print(dut.__dict__)    

    clock = Clock(dut.clock, 1, units="ns")  # 1ns period clock on port clock
    cocotb.start_soon(clock.start())         # Start the clock

    #parameters of the dut
    paramstr = os.environ.get('param')
    params = json.loads(paramstr)
    aBits = params['aBits']
    bBits = params['bBits']
    InSize = params['InSize']
    print(f'parameters of the dut: aBits = {aBits}, bBits = {bBits}, InSize = {InSize}')

    #creating random input data for signed integers
    sample_size = 10
    a_data = []
    b_data = []
    for sample in range(sample_size):
        a_vec = random_data_signed(aBits, InSize, sample+37)
        b_vec = random_data_signed(bBits, InSize, sample+49)
        a_data.append(a_vec)
        b_data.append(b_vec)
    print(f"{a_data = }")
    print(f"{b_data = }")
    
    #calculating reference output
    refsum = []
    for sample in range(sample_size):
        sum = 0
        for i in range(InSize):
            sum = sum + a_data[sample][i] * b_data[sample][i]
        refsum.append(sum)
    print(f"{refsum = }")
    
    # verification of the dut
    dut.reset.value = 1
    await FallingEdge(dut.clock)  # Synchronize with the clock

    dut.reset.value = 0
    await FallingEdge(dut.clock)

    for sample in range(sample_size):
        for i in range(InSize):
            statement1 = f'dut.io_a_{i}.value = {a_data[sample][i]}'  # to deal with the unrolled Vec I/O from Chisel to Verilog
            exec(statement1)
            statement2 = f'dut.io_b_{i}.value = {b_data[sample][i]}'
            exec(statement2)
        
        await FallingEdge(dut.clock)
    
        dut._log.info(f"dut.io_y.value = {dut.io_y.value.signed_integer}")
        assert dut.io_y.value.signed_integer == refsum[sample]
