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

#define MAX_LINE_LENGTH 26112

#define MAX_COL 68
#define MAX_Zc 384

// #define READ_FROM_FILE 1

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
        VlWide<72> llrPacked = top->rootp->LDPCDecoderTop__DOT__LLRRAM__DOT__array_0_ext__DOT__Memory[i]; //VlWide<72>/*2303:0*/
        
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

    top->io_zSize = Zc;
    top->io_isBG1 = 1;

    int prev_tick_llrWValid = 0;
    
    for(int i = 0; i < 6000; i++){
        if((i > 2 && i < 200) || (i > 5100 && i < 5300)) {top->io_llrIn_valid = 1;}
        else {top->io_llrIn_valid = 0;}
        top->io_llrIn_bits_0   = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][0];
        top->io_llrIn_bits_1   = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][1];
        top->io_llrIn_bits_2   = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][2];
        top->io_llrIn_bits_3   = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][3];
        top->io_llrIn_bits_4   = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][4];
        top->io_llrIn_bits_5   = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][5];
        top->io_llrIn_bits_6   = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][6];
        top->io_llrIn_bits_7   = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][7];
        top->io_llrIn_bits_8   = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][8];
        top->io_llrIn_bits_9   = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][9];
        top->io_llrIn_bits_10  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][10];
        top->io_llrIn_bits_11  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][11];
        top->io_llrIn_bits_12  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][12];
        top->io_llrIn_bits_13  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][13];
        top->io_llrIn_bits_14  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][14];
        top->io_llrIn_bits_15  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][15];
        top->io_llrIn_bits_16  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][16];
        top->io_llrIn_bits_17  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][17];
        top->io_llrIn_bits_18  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][18];
        top->io_llrIn_bits_19  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][19];
        top->io_llrIn_bits_20  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][20];
        top->io_llrIn_bits_21  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][21];
        top->io_llrIn_bits_22  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][22];
        top->io_llrIn_bits_23  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][23];
        top->io_llrIn_bits_24  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][24];
        top->io_llrIn_bits_25  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][25];
        top->io_llrIn_bits_26  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][26];
        top->io_llrIn_bits_27  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][27];
        top->io_llrIn_bits_28  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][28];
        top->io_llrIn_bits_29  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][29];
        top->io_llrIn_bits_30  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][30];
        top->io_llrIn_bits_31  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][31];
        top->io_llrIn_bits_32  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][32];
        top->io_llrIn_bits_33  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][33];
        top->io_llrIn_bits_34  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][34];
        top->io_llrIn_bits_35  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][35];
        top->io_llrIn_bits_36  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][36];
        top->io_llrIn_bits_37  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][37];
        top->io_llrIn_bits_38  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][38];
        top->io_llrIn_bits_39  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][39];
        top->io_llrIn_bits_40  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][40];
        top->io_llrIn_bits_41  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][41];
        top->io_llrIn_bits_42  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][42];
        top->io_llrIn_bits_43  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][43];
        top->io_llrIn_bits_44  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][44];
        top->io_llrIn_bits_45  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][45];
        top->io_llrIn_bits_46  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][46];
        top->io_llrIn_bits_47  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][47];
        top->io_llrIn_bits_48  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][48];
        top->io_llrIn_bits_49  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][49];
        top->io_llrIn_bits_50  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][50];
        top->io_llrIn_bits_51  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][51];
        top->io_llrIn_bits_52  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][52];
        top->io_llrIn_bits_53  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][53];
        top->io_llrIn_bits_54  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][54];
        top->io_llrIn_bits_55  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][55];
        top->io_llrIn_bits_56  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][56];
        top->io_llrIn_bits_57  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][57];
        top->io_llrIn_bits_58  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][58];
        top->io_llrIn_bits_59  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][59];
        top->io_llrIn_bits_60  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][60];
        top->io_llrIn_bits_61  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][61];
        top->io_llrIn_bits_62  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][62];
        top->io_llrIn_bits_63  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][63];
        top->io_llrIn_bits_64  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][64];
        top->io_llrIn_bits_65  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][65];
        top->io_llrIn_bits_66  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][66];
        top->io_llrIn_bits_67  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][67];
        top->io_llrIn_bits_68  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][68];
        top->io_llrIn_bits_69  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][69];
        top->io_llrIn_bits_70  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][70];
        top->io_llrIn_bits_71  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][71];
        top->io_llrIn_bits_72  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][72];
        top->io_llrIn_bits_73  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][73];
        top->io_llrIn_bits_74  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][74];
        top->io_llrIn_bits_75  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][75];
        top->io_llrIn_bits_76  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][76];
        top->io_llrIn_bits_77  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][77];
        top->io_llrIn_bits_78  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][78];
        top->io_llrIn_bits_79  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][79];
        top->io_llrIn_bits_80  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][80];
        top->io_llrIn_bits_81  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][81];
        top->io_llrIn_bits_82  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][82];
        top->io_llrIn_bits_83  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][83];
        top->io_llrIn_bits_84  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][84];
        top->io_llrIn_bits_85  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][85];
        top->io_llrIn_bits_86  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][86];
        top->io_llrIn_bits_87  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][87];
        top->io_llrIn_bits_88  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][88];
        top->io_llrIn_bits_89  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][89];
        top->io_llrIn_bits_90  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][90];
        top->io_llrIn_bits_91  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][91];
        top->io_llrIn_bits_92  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][92];
        top->io_llrIn_bits_93  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][93];
        top->io_llrIn_bits_94  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][94];
        top->io_llrIn_bits_95  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][95];
        top->io_llrIn_bits_96  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][96];
        top->io_llrIn_bits_97  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][97];
        top->io_llrIn_bits_98  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][98];
        top->io_llrIn_bits_99  = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][99];
        top->io_llrIn_bits_100 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][100];
        top->io_llrIn_bits_101 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][101];
        top->io_llrIn_bits_102 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][102];
        top->io_llrIn_bits_103 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][103];
        top->io_llrIn_bits_104 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][104];
        top->io_llrIn_bits_105 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][105];
        top->io_llrIn_bits_106 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][106];
        top->io_llrIn_bits_107 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][107];
        top->io_llrIn_bits_108 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][108];
        top->io_llrIn_bits_109 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][109];
        top->io_llrIn_bits_110 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][110];
        top->io_llrIn_bits_111 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][111];
        top->io_llrIn_bits_112 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][112];
        top->io_llrIn_bits_113 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][113];
        top->io_llrIn_bits_114 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][114];
        top->io_llrIn_bits_115 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][115];
        top->io_llrIn_bits_116 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][116];
        top->io_llrIn_bits_117 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][117];
        top->io_llrIn_bits_118 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][118];
        top->io_llrIn_bits_119 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][119];
        top->io_llrIn_bits_120 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][120];
        top->io_llrIn_bits_121 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][121];
        top->io_llrIn_bits_122 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][122];
        top->io_llrIn_bits_123 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][123];
        top->io_llrIn_bits_124 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][124];
        top->io_llrIn_bits_125 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][125];
        top->io_llrIn_bits_126 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][126];
        top->io_llrIn_bits_127 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][127];
        top->io_llrIn_bits_128 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][128];
        top->io_llrIn_bits_129 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][129];
        top->io_llrIn_bits_130 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][130];
        top->io_llrIn_bits_131 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][131];
        top->io_llrIn_bits_132 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][132];
        top->io_llrIn_bits_133 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][133];
        top->io_llrIn_bits_134 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][134];
        top->io_llrIn_bits_135 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][135];
        top->io_llrIn_bits_136 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][136];
        top->io_llrIn_bits_137 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][137];
        top->io_llrIn_bits_138 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][138];
        top->io_llrIn_bits_139 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][139];
        top->io_llrIn_bits_140 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][140];
        top->io_llrIn_bits_141 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][141];
        top->io_llrIn_bits_142 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][142];
        top->io_llrIn_bits_143 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][143];
        top->io_llrIn_bits_144 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][144];
        top->io_llrIn_bits_145 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][145];
        top->io_llrIn_bits_146 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][146];
        top->io_llrIn_bits_147 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][147];
        top->io_llrIn_bits_148 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][148];
        top->io_llrIn_bits_149 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][149];
        top->io_llrIn_bits_150 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][150];
        top->io_llrIn_bits_151 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][151];
        top->io_llrIn_bits_152 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][152];
        top->io_llrIn_bits_153 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][153];
        top->io_llrIn_bits_154 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][154];
        top->io_llrIn_bits_155 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][155];
        top->io_llrIn_bits_156 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][156];
        top->io_llrIn_bits_157 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][157];
        top->io_llrIn_bits_158 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][158];
        top->io_llrIn_bits_159 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][159];
        top->io_llrIn_bits_160 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][160];
        top->io_llrIn_bits_161 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][161];
        top->io_llrIn_bits_162 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][162];
        top->io_llrIn_bits_163 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][163];
        top->io_llrIn_bits_164 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][164];
        top->io_llrIn_bits_165 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][165];
        top->io_llrIn_bits_166 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][166];
        top->io_llrIn_bits_167 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][167];
        top->io_llrIn_bits_168 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][168];
        top->io_llrIn_bits_169 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][169];
        top->io_llrIn_bits_170 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][170];
        top->io_llrIn_bits_171 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][171];
        top->io_llrIn_bits_172 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][172];
        top->io_llrIn_bits_173 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][173];
        top->io_llrIn_bits_174 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][174];
        top->io_llrIn_bits_175 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][175];
        top->io_llrIn_bits_176 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][176];
        top->io_llrIn_bits_177 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][177];
        top->io_llrIn_bits_178 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][178];
        top->io_llrIn_bits_179 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][179];
        top->io_llrIn_bits_180 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][180];
        top->io_llrIn_bits_181 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][181];
        top->io_llrIn_bits_182 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][182];
        top->io_llrIn_bits_183 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][183];
        top->io_llrIn_bits_184 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][184];
        top->io_llrIn_bits_185 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][185];
        top->io_llrIn_bits_186 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][186];
        top->io_llrIn_bits_187 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][187];
        top->io_llrIn_bits_188 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][188];
        top->io_llrIn_bits_189 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][189];
        top->io_llrIn_bits_190 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][190];
        top->io_llrIn_bits_191 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][191];
        top->io_llrIn_bits_192 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][192];
        top->io_llrIn_bits_193 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][193];
        top->io_llrIn_bits_194 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][194];
        top->io_llrIn_bits_195 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][195];
        top->io_llrIn_bits_196 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][196];
        top->io_llrIn_bits_197 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][197];
        top->io_llrIn_bits_198 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][198];
        top->io_llrIn_bits_199 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][199];
        top->io_llrIn_bits_200 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][200];
        top->io_llrIn_bits_201 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][201];
        top->io_llrIn_bits_202 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][202];
        top->io_llrIn_bits_203 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][203];
        top->io_llrIn_bits_204 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][204];
        top->io_llrIn_bits_205 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][205];
        top->io_llrIn_bits_206 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][206];
        top->io_llrIn_bits_207 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][207];
        top->io_llrIn_bits_208 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][208];
        top->io_llrIn_bits_209 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][209];
        top->io_llrIn_bits_210 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][210];
        top->io_llrIn_bits_211 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][211];
        top->io_llrIn_bits_212 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][212];
        top->io_llrIn_bits_213 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][213];
        top->io_llrIn_bits_214 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][214];
        top->io_llrIn_bits_215 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][215];
        top->io_llrIn_bits_216 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][216];
        top->io_llrIn_bits_217 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][217];
        top->io_llrIn_bits_218 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][218];
        top->io_llrIn_bits_219 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][219];
        top->io_llrIn_bits_220 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][220];
        top->io_llrIn_bits_221 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][221];
        top->io_llrIn_bits_222 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][222];
        top->io_llrIn_bits_223 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][223];
        top->io_llrIn_bits_224 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][224];
        top->io_llrIn_bits_225 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][225];
        top->io_llrIn_bits_226 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][226];
        top->io_llrIn_bits_227 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][227];
        top->io_llrIn_bits_228 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][228];
        top->io_llrIn_bits_229 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][229];
        top->io_llrIn_bits_230 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][230];
        top->io_llrIn_bits_231 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][231];
        top->io_llrIn_bits_232 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][232];
        top->io_llrIn_bits_233 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][233];
        top->io_llrIn_bits_234 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][234];
        top->io_llrIn_bits_235 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][235];
        top->io_llrIn_bits_236 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][236];
        top->io_llrIn_bits_237 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][237];
        top->io_llrIn_bits_238 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][238];
        top->io_llrIn_bits_239 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][239];
        top->io_llrIn_bits_240 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][240];
        top->io_llrIn_bits_241 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][241];
        top->io_llrIn_bits_242 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][242];
        top->io_llrIn_bits_243 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][243];
        top->io_llrIn_bits_244 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][244];
        top->io_llrIn_bits_245 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][245];
        top->io_llrIn_bits_246 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][246];
        top->io_llrIn_bits_247 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][247];
        top->io_llrIn_bits_248 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][248];
        top->io_llrIn_bits_249 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][249];
        top->io_llrIn_bits_250 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][250];
        top->io_llrIn_bits_251 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][251];
        top->io_llrIn_bits_252 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][252];
        top->io_llrIn_bits_253 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][253];
        top->io_llrIn_bits_254 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][254];
        top->io_llrIn_bits_255 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][255];
        top->io_llrIn_bits_256 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][256];
        top->io_llrIn_bits_257 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][257];
        top->io_llrIn_bits_258 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][258];
        top->io_llrIn_bits_259 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][259];
        top->io_llrIn_bits_260 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][260];
        top->io_llrIn_bits_261 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][261];
        top->io_llrIn_bits_262 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][262];
        top->io_llrIn_bits_263 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][263];
        top->io_llrIn_bits_264 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][264];
        top->io_llrIn_bits_265 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][265];
        top->io_llrIn_bits_266 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][266];
        top->io_llrIn_bits_267 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][267];
        top->io_llrIn_bits_268 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][268];
        top->io_llrIn_bits_269 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][269];
        top->io_llrIn_bits_270 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][270];
        top->io_llrIn_bits_271 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][271];
        top->io_llrIn_bits_272 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][272];
        top->io_llrIn_bits_273 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][273];
        top->io_llrIn_bits_274 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][274];
        top->io_llrIn_bits_275 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][275];
        top->io_llrIn_bits_276 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][276];
        top->io_llrIn_bits_277 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][277];
        top->io_llrIn_bits_278 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][278];
        top->io_llrIn_bits_279 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][279];
        top->io_llrIn_bits_280 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][280];
        top->io_llrIn_bits_281 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][281];
        top->io_llrIn_bits_282 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][282];
        top->io_llrIn_bits_283 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][283];
        top->io_llrIn_bits_284 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][284];
        top->io_llrIn_bits_285 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][285];
        top->io_llrIn_bits_286 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][286];
        top->io_llrIn_bits_287 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][287];
        top->io_llrIn_bits_288 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][288];
        top->io_llrIn_bits_289 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][289];
        top->io_llrIn_bits_290 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][290];
        top->io_llrIn_bits_291 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][291];
        top->io_llrIn_bits_292 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][292];
        top->io_llrIn_bits_293 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][293];
        top->io_llrIn_bits_294 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][294];
        top->io_llrIn_bits_295 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][295];
        top->io_llrIn_bits_296 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][296];
        top->io_llrIn_bits_297 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][297];
        top->io_llrIn_bits_298 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][298];
        top->io_llrIn_bits_299 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][299];
        top->io_llrIn_bits_300 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][300];
        top->io_llrIn_bits_301 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][301];
        top->io_llrIn_bits_302 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][302];
        top->io_llrIn_bits_303 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][303];
        top->io_llrIn_bits_304 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][304];
        top->io_llrIn_bits_305 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][305];
        top->io_llrIn_bits_306 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][306];
        top->io_llrIn_bits_307 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][307];
        top->io_llrIn_bits_308 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][308];
        top->io_llrIn_bits_309 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][309];
        top->io_llrIn_bits_310 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][310];
        top->io_llrIn_bits_311 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][311];
        top->io_llrIn_bits_312 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][312];
        top->io_llrIn_bits_313 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][313];
        top->io_llrIn_bits_314 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][314];
        top->io_llrIn_bits_315 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][315];
        top->io_llrIn_bits_316 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][316];
        top->io_llrIn_bits_317 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][317];
        top->io_llrIn_bits_318 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][318];
        top->io_llrIn_bits_319 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][319];
        top->io_llrIn_bits_320 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][320];
        top->io_llrIn_bits_321 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][321];
        top->io_llrIn_bits_322 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][322];
        top->io_llrIn_bits_323 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][323];
        top->io_llrIn_bits_324 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][324];
        top->io_llrIn_bits_325 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][325];
        top->io_llrIn_bits_326 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][326];
        top->io_llrIn_bits_327 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][327];
        top->io_llrIn_bits_328 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][328];
        top->io_llrIn_bits_329 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][329];
        top->io_llrIn_bits_330 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][330];
        top->io_llrIn_bits_331 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][331];
        top->io_llrIn_bits_332 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][332];
        top->io_llrIn_bits_333 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][333];
        top->io_llrIn_bits_334 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][334];
        top->io_llrIn_bits_335 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][335];
        top->io_llrIn_bits_336 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][336];
        top->io_llrIn_bits_337 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][337];
        top->io_llrIn_bits_338 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][338];
        top->io_llrIn_bits_339 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][339];
        top->io_llrIn_bits_340 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][340];
        top->io_llrIn_bits_341 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][341];
        top->io_llrIn_bits_342 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][342];
        top->io_llrIn_bits_343 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][343];
        top->io_llrIn_bits_344 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][344];
        top->io_llrIn_bits_345 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][345];
        top->io_llrIn_bits_346 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][346];
        top->io_llrIn_bits_347 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][347];
        top->io_llrIn_bits_348 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][348];
        top->io_llrIn_bits_349 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][349];
        top->io_llrIn_bits_350 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][350];
        top->io_llrIn_bits_351 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][351];
        top->io_llrIn_bits_352 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][352];
        top->io_llrIn_bits_353 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][353];
        top->io_llrIn_bits_354 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][354];
        top->io_llrIn_bits_355 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][355];
        top->io_llrIn_bits_356 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][356];
        top->io_llrIn_bits_357 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][357];
        top->io_llrIn_bits_358 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][358];
        top->io_llrIn_bits_359 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][359];
        top->io_llrIn_bits_360 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][360];
        top->io_llrIn_bits_361 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][361];
        top->io_llrIn_bits_362 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][362];
        top->io_llrIn_bits_363 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][363];
        top->io_llrIn_bits_364 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][364];
        top->io_llrIn_bits_365 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][365];
        top->io_llrIn_bits_366 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][366];
        top->io_llrIn_bits_367 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][367];
        top->io_llrIn_bits_368 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][368];
        top->io_llrIn_bits_369 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][369];
        top->io_llrIn_bits_370 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][370];
        top->io_llrIn_bits_371 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][371];
        top->io_llrIn_bits_372 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][372];
        top->io_llrIn_bits_373 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][373];
        top->io_llrIn_bits_374 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][374];
        top->io_llrIn_bits_375 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][375];
        top->io_llrIn_bits_376 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][376];
        top->io_llrIn_bits_377 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][377];
        top->io_llrIn_bits_378 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][378];
        top->io_llrIn_bits_379 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][379];
        top->io_llrIn_bits_380 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][380];
        top->io_llrIn_bits_381 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][381];
        top->io_llrIn_bits_382 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][382];
        top->io_llrIn_bits_383 = llr_in[top->rootp->LDPCDecoderTop__DOT__llrInitCounter][383];
        if (top->rootp->io_llrWAddr_valid == 0 && prev_tick_llrWValid == 1){
            printf("i = %d\n", i);
            printLLRRAM(llrWrLayer, BG, Zc, llrRAM_file);
            llrWrLayer++;
        }
        prev_tick_llrWValid = top->rootp->io_llrWAddr_valid;
        step();step();
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