BUILD_DIR = ./build
RTL_DIR = $(BUILD_DIR)/rtl

TOP = ldpcDecTop
SIM_TOP = SimTop

FPGATOP = top.TopMain
SIMTOP  = top.SimTop

TOP_V = $(RTL_DIR)/$(TOP).v
SIM_TOP_V = $(RTL_DIR)/$(SIM_TOP).v

SCALA_FILE = $(shell find ./src/main/scala -name '*.scala')
TEST_FILE = $(shell find ./src/test/scala -name '*.scala')

TIMELOG = $(BUILD_DIR)/time.log
TIME_CMD = time -avp -o $(TIMELOG)

MFC ?= 1

ifeq ($(MFC),1)
CHISEL_VERSION = chisel
else
CHISEL_VERSION = chisel3
endif

$(TOP_V): $(SCALA_FILE)
	mkdir -p $(@D)
	$(TIME_CMD) mill -i $(TOP)[$(CHISEL_VERSION)].runMain $(FPGATOP) \
		--target-dir $(@D) \
		--target verilog

verilog: $(TOP_V)