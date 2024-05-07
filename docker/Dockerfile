# Copyright (c) 2024 Intel Corporation
# SPDX-License-Identifier: Apache-2.0

FROM ubuntu:22.04
ENV TZ=America/Los_Angeles
ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install --no-install-recommends -y curl openjdk-19-jdk build-essential python3 python3-dev python3-pip

RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get dist-upgrade -y && \
    apt-get autoremove -y && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

ENV PYTHONUSERBASE=/root/pythonbase
RUN pip install --no-cache-dir pytest==8.1.1 cocotb==1.8.1 cocotb-test==0.2.5

# sbt from https://github.com/sbt/sbt/
RUN curl -L -O https://github.com/sbt/sbt/releases/download/v1.9.9/sbt-1.9.9.tgz \
 && tar -xzf sbt-1.9.9.tgz \
 && rm -rf sbt-1.9.9.tgz

# Install verilator from pre-built binary distribution
# delete most unneeded files
RUN curl -L -O https://github.com/YosysHQ/oss-cad-suite-build/releases/download/2023-08-01/oss-cad-suite-linux-x64-20230801.tgz \
 && tar xzf oss-cad-suite-linux-x64-20230801.tgz \
 && rm -rf oss-cad-suite-linux-x64-20230801.tgz
WORKDIR /oss-cad-suite/bin
RUN find . ! -regex ".*verilator.*\|.*yosys.*\|.*eqy.*\|.*sby.*\|.*tabby.*\|\.\|.." -exec rm -r {} \;
WORKDIR /oss-cad-suite/libexec
RUN find . ! -regex ".*verilator.*\|.*yosys.*\|.*eqy.*\|.*sby.*\|.*tabby.*\|.*python.*\|\.\|.." -exec rm -r {} \;
WORKDIR /oss-cad-suite/lib
RUN find . -mindepth 1 -maxdepth 1 -type d -exec rm -r {} \; \
 && find . -mindepth 1 -maxdepth 1 -type f -not -regex ".*lib.*\|.*ld.*" -exec rm {} \;
WORKDIR /

# set paths
ENV PATH /oss-cad-suite/bin:/sbt/bin:$PATH
ENV LC_ALL C
ENV VERILATOR_INC_DIR /oss-cad-suite/share/verilator/include
