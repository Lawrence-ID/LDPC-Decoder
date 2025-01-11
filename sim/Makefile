TOP_MODULE_NAME = LDPCDecoderTop

# Check for sanity to avoid later confusion

ifneq ($(words $(CURDIR)),1)
 $(error Unsupported: GNU Make cannot build in directories containing spaces, build elsewhere: '$(CURDIR)')
endif

# Set up variables

# If $VERILATOR_ROOT isn't in the environment, we assume it is part of a
# package install, and verilator is in your path. Otherwise find the
# binary relative to $VERILATOR_ROOT (such as when inside the git sources).
ifeq ($(VERILATOR_ROOT),)
VERILATOR = verilator
VERILATOR_COVERAGE = verilator_coverage
else
export VERILATOR_ROOT
VERILATOR = $(VERILATOR_ROOT)/bin/verilator
VERILATOR_COVERAGE = $(VERILATOR_ROOT)/bin/verilator_coverage
endif

VSRC = $(shell find $(abspath ../build/rtl) -name "*.v" -or -name "*.sv")
CSRC = $(shell find $(abspath ./csrc) -name "*.c" -or -name "*.cc" -or -name "*.cpp")
LIBS += $(shell llvm-config --libs --ldflags --system-libs)

SIM_TRACE ?= VCD

SIM_BUILD_DIR = $(abspath ./build)
SIM_BIN = $(abspath $(SIM_BUILD_DIR)/SIM_$(TOP_MODULE_NAME))
OBJ_DIR = $(abspath $(SIM_BUILD_DIR)/obj_dir)

# Generate C++ in executable form
VERILATOR_FLAGS += -cc -exe
# Generate makefile dependencies (not shown as complicates the Makefile)
VERILATOR_FLAGS += -MMD
# Optimize
VERILATOR_FLAGS += -x-assign fast
# Warn abount lint issues; may not want this on less solid designs
VERILATOR_FLAGS += -Wall
VERILATOR_FLAGS += --top-module $(TOP_MODULE_NAME)
# Include .v files
VERILATOR_FLAGS +=
# Timescale and timing
VERILATOR_FLAGS += --timescale "1ns/1ns" --no-timing
# Make waveforms
ifeq ($(SIM_TRACE), VCD)
VERILATOR_FLAGS += --trace
WAVE_FILE = wave.vcd
else ifeq ($(SIM_TRACE), FST)
VERILATOR_FLAGS += --trace-fst
CFLAGS += -DTRACE_AS_FST
WAVE_FILE = wave.fst
endif
# Check SystemVerilog assertions
# VERILATOR_FLAGS += --assert
# Generate coverage analysis
# VERILATOR_FLAGS += --coverage
# Run Verilator in debug mode
#VERILATOR_FLAGS += --debug
# Add this trace to get a backtrace in gdb
#VERILATOR_FLAGS += --gdbbt

# Input files for Verilator SIM
# VERILATOR_SIM_INPUT = -f input.vc $(VSRC) $(MAIN_CSRC)
VERILATOR_SIM_INPUT = $(VSRC) $(CSRC)
VERILATOR_SIM_OUTPUT = --Mdir $(OBJ_DIR) -o $(abspath $(SIM_BIN))

$(shell mkdir -p $(OBJ_DIR))

# rules for verilator
INCFLAGS = $(addprefix -I, $(INC_PATH))
CFLAGS += $(INCFLAGS) -DTOP_NAME="\"V$(TOP_MODULE_NAME)\""
LDFLAGS += $(LIBS) -fPIC

sim:
	@echo "-- VERILATE ----------------"
	rm -rf $(OBJ_DIR)
	$(VERILATOR) $(VERILATOR_FLAGS) -Wno-fatal $(VERILATOR_FLAGS) \
		--top-module $(TOP_MODULE_NAME) $(VERILATOR_SIM_INPUT) \
	    $(addprefix -CFLAGS , $(CFLAGS)) $(addprefix -LDFLAGS , $(LDFLAGS)) \
		 $(VERILATOR_SIM_OUTPUT)
	
	@echo
	@echo "-- BUILD -------------------"
# To compile, we can either
# 1. Pass --build to Verilator by editing VERILATOR_FLAGS above.
# 2. Or, run the make rules Verilator does:
#	$(MAKE) -j -C obj_dir -f Vtop.mk
# 3. Or, call a submakefile where we can override the rules ourselves:
	@echo $(SIM_BIN)
	$(MAKE) -j -C $(OBJ_DIR) -f V$(TOP_MODULE_NAME).mk $(SIM_BIN)

	@echo
	@echo "-- RUN ---------------------"
	# @rm -rf logs
	# @mkdir -p logs
	$(SIM_BIN) $(ARGS) $(IMG) +trace

	# @echo
	# @echo "-- COVERAGE ----------------"
	# @rm -rf logs/annotated
	# $(VERILATOR_COVERAGE) --annotate logs/annotated logs/coverage.dat

	@echo
	@echo "-- DONE --------------------"
	@echo "To see waveforms in a waveform viewer, run: gtkwave $(OBJ_DIR)/$(WAVE_FILE)"
	@echo
	# @gtkwave $(OBJ_DIR)/$(WAVE_FILE)

clean: 
	rm -rf $(SIM_BUILD_DIR)

.PHONY: clean sim
