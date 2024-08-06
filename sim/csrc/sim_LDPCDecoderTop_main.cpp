#include <stdio.h>
#include "verilated.h"
#include "verilated_vcd_c.h"
// #include "svdpi.h"
#include "VLDPCDecoderTop.h"
// #include "VLDPCDecoderTop__Dpi.h"
#include "VLDPCDecoderTop___024root.h"

VerilatedContext *contextp = new VerilatedContext;
VLDPCDecoderTop *top = new VLDPCDecoderTop{contextp};
VerilatedVcdC *tfp = new VerilatedVcdC;

void step(){
    contextp->timeInc(1);
    top->clock = !top->clock;
    top->eval();
    tfp->dump(contextp->time());
}

int main(int argc, char **argv){
    contextp->commandArgs(argc, argv);
    contextp->traceEverOn(true);
    top->trace(tfp, 99);
    tfp->open("/home/ubuntu/Projects/LDPC-Decoder/sim/build/obj_dir/wave.vcd");

    top->clock = 1;
    top->reset = 1; 
    step();step();
    top->reset = 0;

    for(int i = 0; i < 3000; i++){
        step();
    }

    tfp->close();
    delete top;
    delete contextp;
    return 0;
}