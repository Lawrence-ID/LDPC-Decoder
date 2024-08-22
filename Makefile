BUILD_DIR = ./build
SIM_DIR = ./sim
RTL_DIR = $(BUILD_DIR)/rtl

TOP = LDPCDecoderTop
SIM_TOP = SimTop

FPGATOP = top.TopMain
SIMTOP  = top.SimTop

TOP_V = $(RTL_DIR)/$(TOP).v
SIM_TOP_V = $(RTL_DIR)/$(SIM_TOP).v

SCALA_FILE = $(shell find $(abspath ./src/main/scala) -name '*.scala')
VFILE = $(shell find $(abspath ./src/main/resources) -name '*.v')
TEST_FILE = $(shell find $(abspath ./src/test/scala) -name '*.scala')

TIMELOG = $(BUILD_DIR)/time.log
TIME_CMD = time -avp -o $(TIMELOG)

MFC ?= 1

ifeq ($(MFC),1)
CHISEL_VERSION = chisel
else
CHISEL_VERSION = chisel3
endif

$(TOP_V): $(SCALA_FILE) $(VFILE)
	mkdir -p $(@D)
	$(TIME_CMD) mill -i $(TOP)[$(CHISEL_VERSION)].runMain $(FPGATOP) \
		--target-dir $(@D) \
		--target verilog

verilog: $(TOP_V)

sim: $(TOP_V)
	make -j -C $(SIM_DIR) sim
