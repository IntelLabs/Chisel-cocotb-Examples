# Copyright (c) 2024 Intel Corporation
# SPDX-License-Identifier: Apache-2.0


from pathlib import Path
import cocotb_test.simulator
import os
import subprocess
import random

def run(**kwargs):

    sim = os.getenv('SIM', default='verilator')

    waves = None
    extra_args = []
    if sim is None:
        pass
    elif sim == "vcs":
        extra_args.append("-timescale=1ps/1ps")
        waves = True
    elif sim == "verilator":
        os.environ['SIM'] = 'verilator'
        extra_args.extend(["--trace", "--trace-structs", "--coverage"])
    elif sim == "icarus":
        os.environ['SIM'] = 'icarus'   # with icarus, use 2 sec clock inside the tests [clock = Clock(dut.clk, 2, units="sec"]
        os.environ['WAVES'] = '1'
    else:
        assert False, 'Simulator (SIM) not defined'

    if 'extra_args' not in kwargs:
        kwargs['extra_args'] = []

    #extra_args.extend(['-O3', '--autoflush'])

    kwargs['extra_args'] += extra_args

    if waves is not None:
        kwargs['waves'] = waves

    print(kwargs)

    cocotb_test.simulator.run(**kwargs)


def unique_test_id():
    t = os.environ.get('PYTEST_CURRENT_TEST')
    t = t.split(' ')[0].split(':')[-1]
    t = t.replace('[', '_').replace(']', '').replace('-', '_')
    return t


# Generate Verilog from Chisel
def generate_verilog(package_name, emitVer_objname):
    chisel_sbt_dir = Path(__file__).parent.parent.parent
    print("chisel_sbt_dir = ", chisel_sbt_dir)
    cwd = os.getcwd()
    print("cwd = ", cwd)

    os.chdir(chisel_sbt_dir)
    try:
        cmd = f"sbt 'runMain {package_name}.{emitVer_objname}'"    
        print("cmd = ", cmd)
        subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, shell=True, check=True)
    except:
        assert False, "sbt runMain failed"    
    os.chdir(cwd)

# Create random signed integers
def random_data_signed(bitwidth, sample_size, seed):
    datamax = pow(2, (bitwidth-1)) - 1
    datamin = - datamax - 1
    print(f"{datamax = }, {datamin = }")

    data_list = []
    random.seed(seed)
    for i in range(sample_size):
        data_list.append(random.randint(datamin, datamax))
    
    return data_list
