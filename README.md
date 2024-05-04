# Chisel and cocotb Examples

Example designs of [Chisel](https://www.chisel-lang.org/) and [cocotb](https://www.cocotb.org/) for Agile Hardware Design and Verification.

## Requirements

This repository requires sbt, g++, and verilator (see below). User can use gtkwave to view vcd files.

## Installation of verilator > 5.*
```bash
curl -L -O https://github.com/YosysHQ/oss-cad-suite-build/releases/download/2023-08-01/oss-cad-suite-linux-x64-20230801.tgz
tar xzf oss-cad-suite-linux-x64-20230801.tgz
```
Add `oss-cad-suite/bin` to your PATH.

## Install the following Python packages for cocotb
```bash
python3 -m venv .venv              # create a Python3 virtual environment (preferably > python 3.9.x)
source .venv/bin/activate          # activate virtual environment

pip install --upgrade pip          # required packages
pip install pytest
pip install cocotb
pip install cocotb-test
```

## Execute Chisel tests
- Build and test Chisel modules using sbt:
```bash
sbt compile
sbt run
sbt test
```

## Execute cocotb tests:
```bash
cd tests_cocotb/acc_component/
cocotb-clean
pytest -vv
```

# Alternate execution environment using a container:

## Build a container:
The container recipe is given in `docker/Dockerfile`. It enumerates all the required packages and libraries for this project.

Install `apptainer` on a machine where you have root access. See instructions at the following link:
https://apptainer.org/docs/admin/main/installation.html#install-ubuntu-packages

Also install Singularity Python: https://singularityhub.github.io/singularity-cli/ which will help convert Docker recipes into Singularity recipes and vice versa.

Execute the following commands to build the container:

1. `cd docker/`
2. `spython recipe Dockerfile &> Singularity.def`
3. `sudo apptainer build mycontainer.sif Singularity.def`  

The generated `mycontainer.sif` file should be between 512 MB - 1 GB in size. It can be moved to a different machine and run even without root access.

## Activate the container
Execute the following command to open a terminal using the `.sif` file:

`apptainer shell -B /path/to/this/repo/:/mnt <path-to-mycontainer.sif>`  

This will create an `Apptainer` prompt. `cd /mnt` to access and execute Chisel and cocotb tests within the container.
Run `exit` to exit from the container prompt.

Note: Inside the container environemnt, if you see a java FileSystemException issue, `unset XDG_RUNTIME_DIR`.
