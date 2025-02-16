#include <stdio.h>
#include <stdlib.h>
#include "verilated.h"

#ifdef TRACE_AS_FST
#include "verilated_fst_c.h"
#else
#include "verilated_vcd_c.h"
#endif

// #include "svdpi.h"
#include "VLDPCDecoderTop.h"
// #include "VLDPCDecoderTop__Dpi.h"
#include "VLDPCDecoderTop___024root.h"

#include <fstream>
#include <string>
#include <vector>
#include <iostream>
#include <algorithm>
#include <unordered_map>
#include <utility>
#include <functional>

#define MAX_LINE_LENGTH 26112

#define MAX_COL 68
#define MAX_Zc 384

// #define READ_FROM_FILE 1

using namespace std;

VerilatedContext *contextp = new VerilatedContext;
VLDPCDecoderTop *top = new VLDPCDecoderTop{contextp};
#ifdef TRACE_AS_FST
VerilatedFstC *tfp = new VerilatedFstC;
#else
VerilatedVcdC *tfp = new VerilatedVcdC;
#endif

int8_t llr_in[MAX_COL][MAX_Zc];
int Zc = 3;
int BG = 1;
int llrWrLayer = 0;
int MaxIter = 8;

void step(){
    contextp->timeInc(1);
    top->clock = !top->clock;
    top->eval();
    tfp->dump(contextp->time());
}

int to6BitSigned(int value) {
    assert(0 <= value && value <= 63);
    if (value >= 0 && value <= 31) {
        return value; // 0到31直接输出
    } else {
        return value - 64; 
    }
}

void printLLRRAM(int llrWrLayer, int BG, int Zc, FILE *file){
    int rowNum = BG == 1 ? 46 : 42;
    int colNum = BG == 1 ? 68 : 52;
    int8_t llrRAM[MAX_COL * MAX_Zc];
    int idx = 0;

    // VlUnpacked<VlWide<72>/*2303:0*/, 68> 
    for(int i = 0; i < colNum; ++i){
        VlWide<72> llrPacked = top->rootp->LDPCDecoderTop__DOT__ldpcDecoderCore__DOT__LLRRAM__DOT__array_0_ext__DOT__Memory[i]; //VlWide<72>/*2303:0*/
        
        for(int j = 0; j < 384*6/32; j+=3){
            uint32_t llr32_0 = llrPacked.at(j);
            uint32_t llr32_1 = llrPacked.at(j+1);
            uint32_t llr32_2 = llrPacked.at(j+2);
            // printf("j = %d, %d, %d, %d\n", j, llr32_0, llr32_1, llr32_2);
            __int128_t combined = ((__int128_t)llrPacked[j+2] << 64) | ((__int128_t)llrPacked[j+1] << 32) | (llrPacked[j]);
            for (int k = 0; k < 16; k++) {
                uint32_t mask = (1 << 6) - 1;
                __int128_t shift = 6 * k;
                uint32_t llr6 = (combined >> shift) & mask;
                // printf("Extracted 6 bits (block %d): %u\n", k, llr6);
                llrRAM[idx++] = to6BitSigned(llr6);
            }
        }
    }

    // 每行写入384个数，写68行，每68行之后空一行
    llrWrLayer = llrWrLayer % (rowNum * MaxIter);
    fprintf(file, "it = %d\n", (llrWrLayer / rowNum) + 1);
    fprintf(file, "l = %d\n", (llrWrLayer % rowNum) + 1);
    // for (int row = 0; row < 68; row++) {
    //     for (int col = 0; col < Zc; col++) {
    //         fprintf(file, "%4d ", llrRAM[row * 384 + col]);  // 写入当前元素
    //     }
    //     fprintf(file, "\n");  // 每行末尾写入换行符

    //     // 每68行后插入一个空行
    //     if (row == 67) {
    //         fprintf(file, "\n");
    //     }
    // }
    fprintf(file, "llr_ram = ");
    for (int row = 0; row < 68; row++) {
        for (int col = 0; col < Zc; col++) {
            fprintf(file, "%d ", llrRAM[row * 384 + col]);  // 写入当前元素
        }
    }
    fprintf(file, "\n");
    
}

int main(int argc, char **argv){
    FILE * llr_in_file;
    int llr;
    char line[10];
    
#ifdef READ_FROM_FILE
    llr_in_file = fopen("/nfs/home/pengxiao/Projects/LDPC-Decoder/llrIn_real.txt", "r");
    if (llr_in_file == NULL) {
        perror("Error opening llr_in_file");
        return 1;
    }

    printf("\n");
    // Read each line of the file and process the number
    int total_lines = 0;
    while (fgets(line, sizeof(line), llr_in_file)) {

        if (sscanf(line, "%d", &llr) == 1) {
            // printf("Read llr: %d\n", llr);
            llr_in[total_lines / MAX_COL][total_lines % MAX_Zc] = llr;
            total_lines++;
        } else {
            // Handle the case where sscanf fails
            printf("Failed to parse line: '%s'\n", line);
        }
    }
    printf("total llr data line = %d\n", total_lines);
#else
    int sequence[] = {0, 1, 2, 3, 4, 5, 6, 7, -1, -2, -3, -4, -5, -6, -7, -8};
    int cnt = 0;
    for(int l = 0; l < MAX_COL; l++){
        for(int i = 0; i < MAX_Zc; i++){
            if (i < Zc) {
                llr_in[l][i] = (sequence[cnt % 16] & 0x3f); // input value [-8, 7]
                cnt++;
            }
        }
    }
#endif

    contextp->commandArgs(argc, argv);
    contextp->traceEverOn(true);
    top->trace(tfp, 99);
#ifdef TRACE_AS_FST
    tfp->open("../sim/build/obj_dir/wave.fst");
#else
    tfp->open("../sim/build/obj_dir/wave.vcd");
#endif

    FILE * llrRAM_file = fopen("/nfs/home/pengxiao/Projects/LDPC-Decoder/llrRAM.txt", "w");
    if (llrRAM_file == NULL) {
        perror("Error opening llrRAM_file");
        exit(0);
    }

    top->clock = 1;
    top->reset = 1;
    step();step();
    top->reset = 0;

    top->io_in_bits_zSize = Zc;
    top->io_in_bits_isBG1 = 1;

    top->io_out_ready = 1;

    int prev_tick_llrWValid = 0;
    int llr_256block_k = 0;
    
    for(int i = 0; i < 6000; i++){
        if((i > 2 && i < 1000)) {top->io_in_valid = 1;}
        else {top->io_in_valid = 0;}

        int8_t a[32] = {0}; // [hi:lo]
        for (int idx = 0; idx < 32; idx++){
            a[idx] = llr_in[(llr_256block_k / 12) % MAX_COL][((llr_256block_k % 12) * 32 + idx) % MAX_Zc];
        }
        VlWide<8> /*255:0*/ data;
        for (int k = 0; k < 8; k++){
            data.m_storage[k] = a[4 * k] + (((uint32_t)a[4 * k + 1]) << 8) + (((uint32_t)a[4 * k + 2]) << 16) + (((uint32_t)a[4 * k + 3]) << 24);
        }

        top->io_in_bits_llrBlock = data;
        // print_bit(top->io_in_bits_llrBlock);

        if (top->rootp->io_llrWAddr_valid == 0 && prev_tick_llrWValid == 1){
            printf("i = %d\n", i);
            printLLRRAM(llrWrLayer, BG, Zc, llrRAM_file);
            llrWrLayer++;
        }
        prev_tick_llrWValid = top->rootp->io_llrWAddr_valid;
        step();step();
        if(top->io_in_valid == 1 && top->io_in_ready == 1) {
            llr_256block_k++;
        }
    }

    tfp->close();
#ifdef READ_FROM_FILE
    fclose(llr_in_file);
#endif
    fclose(llrRAM_file);
    delete top;
    delete contextp;
    return 0;
}