#include <stdio.h>
#include <stdlib.h>
#include "verilated.h"
#include "verilated_vcd_c.h"
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
VerilatedVcdC *tfp = new VerilatedVcdC;

int8_t llr_in[MAX_COL][MAX_Zc];
int Zc = 3;
int BG = 1;

void step(){
    contextp->timeInc(1);
    top->clock = !top->clock;
    top->eval();
    tfp->dump(contextp->time());
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
    int cnt = 1;
    for(int l = 0; l < MAX_COL; l++){
        for(int i = 0; i < MAX_Zc; i++){
            if (i < Zc) {
                llr_in[l][i] = cnt++;
            }
        }
    }
#endif

    contextp->commandArgs(argc, argv);
    contextp->traceEverOn(true);
    top->trace(tfp, 99);

    tfp->open("../sim/build/obj_dir/wave.vcd");

    top->clock = 1;
    top->reset = 1; 
    step();step();
    top->reset = 0;

    top->io_zSize = Zc;
    
    for(int i = 0; i < 3000; i++){
        top->io_llrIn_0   = llr_in[top->io_vnuLayerCounter][0];
        top->io_llrIn_1   = llr_in[top->io_vnuLayerCounter][1];
        top->io_llrIn_2   = llr_in[top->io_vnuLayerCounter][2];
        top->io_llrIn_3   = llr_in[top->io_vnuLayerCounter][3];
        top->io_llrIn_4   = llr_in[top->io_vnuLayerCounter][4];
        top->io_llrIn_5   = llr_in[top->io_vnuLayerCounter][5];
        top->io_llrIn_6   = llr_in[top->io_vnuLayerCounter][6];
        top->io_llrIn_7   = llr_in[top->io_vnuLayerCounter][7];
        top->io_llrIn_8   = llr_in[top->io_vnuLayerCounter][8];
        top->io_llrIn_9   = llr_in[top->io_vnuLayerCounter][9];
        top->io_llrIn_10  = llr_in[top->io_vnuLayerCounter][10];
        top->io_llrIn_11  = llr_in[top->io_vnuLayerCounter][11];
        top->io_llrIn_12  = llr_in[top->io_vnuLayerCounter][12];
        top->io_llrIn_13  = llr_in[top->io_vnuLayerCounter][13];
        top->io_llrIn_14  = llr_in[top->io_vnuLayerCounter][14];
        top->io_llrIn_15  = llr_in[top->io_vnuLayerCounter][15];
        top->io_llrIn_16  = llr_in[top->io_vnuLayerCounter][16];
        top->io_llrIn_17  = llr_in[top->io_vnuLayerCounter][17];
        top->io_llrIn_18  = llr_in[top->io_vnuLayerCounter][18];
        top->io_llrIn_19  = llr_in[top->io_vnuLayerCounter][19];
        top->io_llrIn_20  = llr_in[top->io_vnuLayerCounter][20];
        top->io_llrIn_21  = llr_in[top->io_vnuLayerCounter][21];
        top->io_llrIn_22  = llr_in[top->io_vnuLayerCounter][22];
        top->io_llrIn_23  = llr_in[top->io_vnuLayerCounter][23];
        top->io_llrIn_24  = llr_in[top->io_vnuLayerCounter][24];
        top->io_llrIn_25  = llr_in[top->io_vnuLayerCounter][25];
        top->io_llrIn_26  = llr_in[top->io_vnuLayerCounter][26];
        top->io_llrIn_27  = llr_in[top->io_vnuLayerCounter][27];
        top->io_llrIn_28  = llr_in[top->io_vnuLayerCounter][28];
        top->io_llrIn_29  = llr_in[top->io_vnuLayerCounter][29];
        top->io_llrIn_30  = llr_in[top->io_vnuLayerCounter][30];
        top->io_llrIn_31  = llr_in[top->io_vnuLayerCounter][31];
        top->io_llrIn_32  = llr_in[top->io_vnuLayerCounter][32];
        top->io_llrIn_33  = llr_in[top->io_vnuLayerCounter][33];
        top->io_llrIn_34  = llr_in[top->io_vnuLayerCounter][34];
        top->io_llrIn_35  = llr_in[top->io_vnuLayerCounter][35];
        top->io_llrIn_36  = llr_in[top->io_vnuLayerCounter][36];
        top->io_llrIn_37  = llr_in[top->io_vnuLayerCounter][37];
        top->io_llrIn_38  = llr_in[top->io_vnuLayerCounter][38];
        top->io_llrIn_39  = llr_in[top->io_vnuLayerCounter][39];
        top->io_llrIn_40  = llr_in[top->io_vnuLayerCounter][40];
        top->io_llrIn_41  = llr_in[top->io_vnuLayerCounter][41];
        top->io_llrIn_42  = llr_in[top->io_vnuLayerCounter][42];
        top->io_llrIn_43  = llr_in[top->io_vnuLayerCounter][43];
        top->io_llrIn_44  = llr_in[top->io_vnuLayerCounter][44];
        top->io_llrIn_45  = llr_in[top->io_vnuLayerCounter][45];
        top->io_llrIn_46  = llr_in[top->io_vnuLayerCounter][46];
        top->io_llrIn_47  = llr_in[top->io_vnuLayerCounter][47];
        top->io_llrIn_48  = llr_in[top->io_vnuLayerCounter][48];
        top->io_llrIn_49  = llr_in[top->io_vnuLayerCounter][49];
        top->io_llrIn_50  = llr_in[top->io_vnuLayerCounter][50];
        top->io_llrIn_51  = llr_in[top->io_vnuLayerCounter][51];
        top->io_llrIn_52  = llr_in[top->io_vnuLayerCounter][52];
        top->io_llrIn_53  = llr_in[top->io_vnuLayerCounter][53];
        top->io_llrIn_54  = llr_in[top->io_vnuLayerCounter][54];
        top->io_llrIn_55  = llr_in[top->io_vnuLayerCounter][55];
        top->io_llrIn_56  = llr_in[top->io_vnuLayerCounter][56];
        top->io_llrIn_57  = llr_in[top->io_vnuLayerCounter][57];
        top->io_llrIn_58  = llr_in[top->io_vnuLayerCounter][58];
        top->io_llrIn_59  = llr_in[top->io_vnuLayerCounter][59];
        top->io_llrIn_60  = llr_in[top->io_vnuLayerCounter][60];
        top->io_llrIn_61  = llr_in[top->io_vnuLayerCounter][61];
        top->io_llrIn_62  = llr_in[top->io_vnuLayerCounter][62];
        top->io_llrIn_63  = llr_in[top->io_vnuLayerCounter][63];
        top->io_llrIn_64  = llr_in[top->io_vnuLayerCounter][64];
        top->io_llrIn_65  = llr_in[top->io_vnuLayerCounter][65];
        top->io_llrIn_66  = llr_in[top->io_vnuLayerCounter][66];
        top->io_llrIn_67  = llr_in[top->io_vnuLayerCounter][67];
        top->io_llrIn_68  = llr_in[top->io_vnuLayerCounter][68];
        top->io_llrIn_69  = llr_in[top->io_vnuLayerCounter][69];
        top->io_llrIn_70  = llr_in[top->io_vnuLayerCounter][70];
        top->io_llrIn_71  = llr_in[top->io_vnuLayerCounter][71];
        top->io_llrIn_72  = llr_in[top->io_vnuLayerCounter][72];
        top->io_llrIn_73  = llr_in[top->io_vnuLayerCounter][73];
        top->io_llrIn_74  = llr_in[top->io_vnuLayerCounter][74];
        top->io_llrIn_75  = llr_in[top->io_vnuLayerCounter][75];
        top->io_llrIn_76  = llr_in[top->io_vnuLayerCounter][76];
        top->io_llrIn_77  = llr_in[top->io_vnuLayerCounter][77];
        top->io_llrIn_78  = llr_in[top->io_vnuLayerCounter][78];
        top->io_llrIn_79  = llr_in[top->io_vnuLayerCounter][79];
        top->io_llrIn_80  = llr_in[top->io_vnuLayerCounter][80];
        top->io_llrIn_81  = llr_in[top->io_vnuLayerCounter][81];
        top->io_llrIn_82  = llr_in[top->io_vnuLayerCounter][82];
        top->io_llrIn_83  = llr_in[top->io_vnuLayerCounter][83];
        top->io_llrIn_84  = llr_in[top->io_vnuLayerCounter][84];
        top->io_llrIn_85  = llr_in[top->io_vnuLayerCounter][85];
        top->io_llrIn_86  = llr_in[top->io_vnuLayerCounter][86];
        top->io_llrIn_87  = llr_in[top->io_vnuLayerCounter][87];
        top->io_llrIn_88  = llr_in[top->io_vnuLayerCounter][88];
        top->io_llrIn_89  = llr_in[top->io_vnuLayerCounter][89];
        top->io_llrIn_90  = llr_in[top->io_vnuLayerCounter][90];
        top->io_llrIn_91  = llr_in[top->io_vnuLayerCounter][91];
        top->io_llrIn_92  = llr_in[top->io_vnuLayerCounter][92];
        top->io_llrIn_93  = llr_in[top->io_vnuLayerCounter][93];
        top->io_llrIn_94  = llr_in[top->io_vnuLayerCounter][94];
        top->io_llrIn_95  = llr_in[top->io_vnuLayerCounter][95];
        top->io_llrIn_96  = llr_in[top->io_vnuLayerCounter][96];
        top->io_llrIn_97  = llr_in[top->io_vnuLayerCounter][97];
        top->io_llrIn_98  = llr_in[top->io_vnuLayerCounter][98];
        top->io_llrIn_99  = llr_in[top->io_vnuLayerCounter][99];
        top->io_llrIn_100 = llr_in[top->io_vnuLayerCounter][100];
        top->io_llrIn_101 = llr_in[top->io_vnuLayerCounter][101];
        top->io_llrIn_102 = llr_in[top->io_vnuLayerCounter][102];
        top->io_llrIn_103 = llr_in[top->io_vnuLayerCounter][103];
        top->io_llrIn_104 = llr_in[top->io_vnuLayerCounter][104];
        top->io_llrIn_105 = llr_in[top->io_vnuLayerCounter][105];
        top->io_llrIn_106 = llr_in[top->io_vnuLayerCounter][106];
        top->io_llrIn_107 = llr_in[top->io_vnuLayerCounter][107];
        top->io_llrIn_108 = llr_in[top->io_vnuLayerCounter][108];
        top->io_llrIn_109 = llr_in[top->io_vnuLayerCounter][109];
        top->io_llrIn_110 = llr_in[top->io_vnuLayerCounter][110];
        top->io_llrIn_111 = llr_in[top->io_vnuLayerCounter][111];
        top->io_llrIn_112 = llr_in[top->io_vnuLayerCounter][112];
        top->io_llrIn_113 = llr_in[top->io_vnuLayerCounter][113];
        top->io_llrIn_114 = llr_in[top->io_vnuLayerCounter][114];
        top->io_llrIn_115 = llr_in[top->io_vnuLayerCounter][115];
        top->io_llrIn_116 = llr_in[top->io_vnuLayerCounter][116];
        top->io_llrIn_117 = llr_in[top->io_vnuLayerCounter][117];
        top->io_llrIn_118 = llr_in[top->io_vnuLayerCounter][118];
        top->io_llrIn_119 = llr_in[top->io_vnuLayerCounter][119];
        top->io_llrIn_120 = llr_in[top->io_vnuLayerCounter][120];
        top->io_llrIn_121 = llr_in[top->io_vnuLayerCounter][121];
        top->io_llrIn_122 = llr_in[top->io_vnuLayerCounter][122];
        top->io_llrIn_123 = llr_in[top->io_vnuLayerCounter][123];
        top->io_llrIn_124 = llr_in[top->io_vnuLayerCounter][124];
        top->io_llrIn_125 = llr_in[top->io_vnuLayerCounter][125];
        top->io_llrIn_126 = llr_in[top->io_vnuLayerCounter][126];
        top->io_llrIn_127 = llr_in[top->io_vnuLayerCounter][127];
        top->io_llrIn_128 = llr_in[top->io_vnuLayerCounter][128];
        top->io_llrIn_129 = llr_in[top->io_vnuLayerCounter][129];
        top->io_llrIn_130 = llr_in[top->io_vnuLayerCounter][130];
        top->io_llrIn_131 = llr_in[top->io_vnuLayerCounter][131];
        top->io_llrIn_132 = llr_in[top->io_vnuLayerCounter][132];
        top->io_llrIn_133 = llr_in[top->io_vnuLayerCounter][133];
        top->io_llrIn_134 = llr_in[top->io_vnuLayerCounter][134];
        top->io_llrIn_135 = llr_in[top->io_vnuLayerCounter][135];
        top->io_llrIn_136 = llr_in[top->io_vnuLayerCounter][136];
        top->io_llrIn_137 = llr_in[top->io_vnuLayerCounter][137];
        top->io_llrIn_138 = llr_in[top->io_vnuLayerCounter][138];
        top->io_llrIn_139 = llr_in[top->io_vnuLayerCounter][139];
        top->io_llrIn_140 = llr_in[top->io_vnuLayerCounter][140];
        top->io_llrIn_141 = llr_in[top->io_vnuLayerCounter][141];
        top->io_llrIn_142 = llr_in[top->io_vnuLayerCounter][142];
        top->io_llrIn_143 = llr_in[top->io_vnuLayerCounter][143];
        top->io_llrIn_144 = llr_in[top->io_vnuLayerCounter][144];
        top->io_llrIn_145 = llr_in[top->io_vnuLayerCounter][145];
        top->io_llrIn_146 = llr_in[top->io_vnuLayerCounter][146];
        top->io_llrIn_147 = llr_in[top->io_vnuLayerCounter][147];
        top->io_llrIn_148 = llr_in[top->io_vnuLayerCounter][148];
        top->io_llrIn_149 = llr_in[top->io_vnuLayerCounter][149];
        top->io_llrIn_150 = llr_in[top->io_vnuLayerCounter][150];
        top->io_llrIn_151 = llr_in[top->io_vnuLayerCounter][151];
        top->io_llrIn_152 = llr_in[top->io_vnuLayerCounter][152];
        top->io_llrIn_153 = llr_in[top->io_vnuLayerCounter][153];
        top->io_llrIn_154 = llr_in[top->io_vnuLayerCounter][154];
        top->io_llrIn_155 = llr_in[top->io_vnuLayerCounter][155];
        top->io_llrIn_156 = llr_in[top->io_vnuLayerCounter][156];
        top->io_llrIn_157 = llr_in[top->io_vnuLayerCounter][157];
        top->io_llrIn_158 = llr_in[top->io_vnuLayerCounter][158];
        top->io_llrIn_159 = llr_in[top->io_vnuLayerCounter][159];
        top->io_llrIn_160 = llr_in[top->io_vnuLayerCounter][160];
        top->io_llrIn_161 = llr_in[top->io_vnuLayerCounter][161];
        top->io_llrIn_162 = llr_in[top->io_vnuLayerCounter][162];
        top->io_llrIn_163 = llr_in[top->io_vnuLayerCounter][163];
        top->io_llrIn_164 = llr_in[top->io_vnuLayerCounter][164];
        top->io_llrIn_165 = llr_in[top->io_vnuLayerCounter][165];
        top->io_llrIn_166 = llr_in[top->io_vnuLayerCounter][166];
        top->io_llrIn_167 = llr_in[top->io_vnuLayerCounter][167];
        top->io_llrIn_168 = llr_in[top->io_vnuLayerCounter][168];
        top->io_llrIn_169 = llr_in[top->io_vnuLayerCounter][169];
        top->io_llrIn_170 = llr_in[top->io_vnuLayerCounter][170];
        top->io_llrIn_171 = llr_in[top->io_vnuLayerCounter][171];
        top->io_llrIn_172 = llr_in[top->io_vnuLayerCounter][172];
        top->io_llrIn_173 = llr_in[top->io_vnuLayerCounter][173];
        top->io_llrIn_174 = llr_in[top->io_vnuLayerCounter][174];
        top->io_llrIn_175 = llr_in[top->io_vnuLayerCounter][175];
        top->io_llrIn_176 = llr_in[top->io_vnuLayerCounter][176];
        top->io_llrIn_177 = llr_in[top->io_vnuLayerCounter][177];
        top->io_llrIn_178 = llr_in[top->io_vnuLayerCounter][178];
        top->io_llrIn_179 = llr_in[top->io_vnuLayerCounter][179];
        top->io_llrIn_180 = llr_in[top->io_vnuLayerCounter][180];
        top->io_llrIn_181 = llr_in[top->io_vnuLayerCounter][181];
        top->io_llrIn_182 = llr_in[top->io_vnuLayerCounter][182];
        top->io_llrIn_183 = llr_in[top->io_vnuLayerCounter][183];
        top->io_llrIn_184 = llr_in[top->io_vnuLayerCounter][184];
        top->io_llrIn_185 = llr_in[top->io_vnuLayerCounter][185];
        top->io_llrIn_186 = llr_in[top->io_vnuLayerCounter][186];
        top->io_llrIn_187 = llr_in[top->io_vnuLayerCounter][187];
        top->io_llrIn_188 = llr_in[top->io_vnuLayerCounter][188];
        top->io_llrIn_189 = llr_in[top->io_vnuLayerCounter][189];
        top->io_llrIn_190 = llr_in[top->io_vnuLayerCounter][190];
        top->io_llrIn_191 = llr_in[top->io_vnuLayerCounter][191];
        top->io_llrIn_192 = llr_in[top->io_vnuLayerCounter][192];
        top->io_llrIn_193 = llr_in[top->io_vnuLayerCounter][193];
        top->io_llrIn_194 = llr_in[top->io_vnuLayerCounter][194];
        top->io_llrIn_195 = llr_in[top->io_vnuLayerCounter][195];
        top->io_llrIn_196 = llr_in[top->io_vnuLayerCounter][196];
        top->io_llrIn_197 = llr_in[top->io_vnuLayerCounter][197];
        top->io_llrIn_198 = llr_in[top->io_vnuLayerCounter][198];
        top->io_llrIn_199 = llr_in[top->io_vnuLayerCounter][199];
        top->io_llrIn_200 = llr_in[top->io_vnuLayerCounter][200];
        top->io_llrIn_201 = llr_in[top->io_vnuLayerCounter][201];
        top->io_llrIn_202 = llr_in[top->io_vnuLayerCounter][202];
        top->io_llrIn_203 = llr_in[top->io_vnuLayerCounter][203];
        top->io_llrIn_204 = llr_in[top->io_vnuLayerCounter][204];
        top->io_llrIn_205 = llr_in[top->io_vnuLayerCounter][205];
        top->io_llrIn_206 = llr_in[top->io_vnuLayerCounter][206];
        top->io_llrIn_207 = llr_in[top->io_vnuLayerCounter][207];
        top->io_llrIn_208 = llr_in[top->io_vnuLayerCounter][208];
        top->io_llrIn_209 = llr_in[top->io_vnuLayerCounter][209];
        top->io_llrIn_210 = llr_in[top->io_vnuLayerCounter][210];
        top->io_llrIn_211 = llr_in[top->io_vnuLayerCounter][211];
        top->io_llrIn_212 = llr_in[top->io_vnuLayerCounter][212];
        top->io_llrIn_213 = llr_in[top->io_vnuLayerCounter][213];
        top->io_llrIn_214 = llr_in[top->io_vnuLayerCounter][214];
        top->io_llrIn_215 = llr_in[top->io_vnuLayerCounter][215];
        top->io_llrIn_216 = llr_in[top->io_vnuLayerCounter][216];
        top->io_llrIn_217 = llr_in[top->io_vnuLayerCounter][217];
        top->io_llrIn_218 = llr_in[top->io_vnuLayerCounter][218];
        top->io_llrIn_219 = llr_in[top->io_vnuLayerCounter][219];
        top->io_llrIn_220 = llr_in[top->io_vnuLayerCounter][220];
        top->io_llrIn_221 = llr_in[top->io_vnuLayerCounter][221];
        top->io_llrIn_222 = llr_in[top->io_vnuLayerCounter][222];
        top->io_llrIn_223 = llr_in[top->io_vnuLayerCounter][223];
        top->io_llrIn_224 = llr_in[top->io_vnuLayerCounter][224];
        top->io_llrIn_225 = llr_in[top->io_vnuLayerCounter][225];
        top->io_llrIn_226 = llr_in[top->io_vnuLayerCounter][226];
        top->io_llrIn_227 = llr_in[top->io_vnuLayerCounter][227];
        top->io_llrIn_228 = llr_in[top->io_vnuLayerCounter][228];
        top->io_llrIn_229 = llr_in[top->io_vnuLayerCounter][229];
        top->io_llrIn_230 = llr_in[top->io_vnuLayerCounter][230];
        top->io_llrIn_231 = llr_in[top->io_vnuLayerCounter][231];
        top->io_llrIn_232 = llr_in[top->io_vnuLayerCounter][232];
        top->io_llrIn_233 = llr_in[top->io_vnuLayerCounter][233];
        top->io_llrIn_234 = llr_in[top->io_vnuLayerCounter][234];
        top->io_llrIn_235 = llr_in[top->io_vnuLayerCounter][235];
        top->io_llrIn_236 = llr_in[top->io_vnuLayerCounter][236];
        top->io_llrIn_237 = llr_in[top->io_vnuLayerCounter][237];
        top->io_llrIn_238 = llr_in[top->io_vnuLayerCounter][238];
        top->io_llrIn_239 = llr_in[top->io_vnuLayerCounter][239];
        top->io_llrIn_240 = llr_in[top->io_vnuLayerCounter][240];
        top->io_llrIn_241 = llr_in[top->io_vnuLayerCounter][241];
        top->io_llrIn_242 = llr_in[top->io_vnuLayerCounter][242];
        top->io_llrIn_243 = llr_in[top->io_vnuLayerCounter][243];
        top->io_llrIn_244 = llr_in[top->io_vnuLayerCounter][244];
        top->io_llrIn_245 = llr_in[top->io_vnuLayerCounter][245];
        top->io_llrIn_246 = llr_in[top->io_vnuLayerCounter][246];
        top->io_llrIn_247 = llr_in[top->io_vnuLayerCounter][247];
        top->io_llrIn_248 = llr_in[top->io_vnuLayerCounter][248];
        top->io_llrIn_249 = llr_in[top->io_vnuLayerCounter][249];
        top->io_llrIn_250 = llr_in[top->io_vnuLayerCounter][250];
        top->io_llrIn_251 = llr_in[top->io_vnuLayerCounter][251];
        top->io_llrIn_252 = llr_in[top->io_vnuLayerCounter][252];
        top->io_llrIn_253 = llr_in[top->io_vnuLayerCounter][253];
        top->io_llrIn_254 = llr_in[top->io_vnuLayerCounter][254];
        top->io_llrIn_255 = llr_in[top->io_vnuLayerCounter][255];
        top->io_llrIn_256 = llr_in[top->io_vnuLayerCounter][256];
        top->io_llrIn_257 = llr_in[top->io_vnuLayerCounter][257];
        top->io_llrIn_258 = llr_in[top->io_vnuLayerCounter][258];
        top->io_llrIn_259 = llr_in[top->io_vnuLayerCounter][259];
        top->io_llrIn_260 = llr_in[top->io_vnuLayerCounter][260];
        top->io_llrIn_261 = llr_in[top->io_vnuLayerCounter][261];
        top->io_llrIn_262 = llr_in[top->io_vnuLayerCounter][262];
        top->io_llrIn_263 = llr_in[top->io_vnuLayerCounter][263];
        top->io_llrIn_264 = llr_in[top->io_vnuLayerCounter][264];
        top->io_llrIn_265 = llr_in[top->io_vnuLayerCounter][265];
        top->io_llrIn_266 = llr_in[top->io_vnuLayerCounter][266];
        top->io_llrIn_267 = llr_in[top->io_vnuLayerCounter][267];
        top->io_llrIn_268 = llr_in[top->io_vnuLayerCounter][268];
        top->io_llrIn_269 = llr_in[top->io_vnuLayerCounter][269];
        top->io_llrIn_270 = llr_in[top->io_vnuLayerCounter][270];
        top->io_llrIn_271 = llr_in[top->io_vnuLayerCounter][271];
        top->io_llrIn_272 = llr_in[top->io_vnuLayerCounter][272];
        top->io_llrIn_273 = llr_in[top->io_vnuLayerCounter][273];
        top->io_llrIn_274 = llr_in[top->io_vnuLayerCounter][274];
        top->io_llrIn_275 = llr_in[top->io_vnuLayerCounter][275];
        top->io_llrIn_276 = llr_in[top->io_vnuLayerCounter][276];
        top->io_llrIn_277 = llr_in[top->io_vnuLayerCounter][277];
        top->io_llrIn_278 = llr_in[top->io_vnuLayerCounter][278];
        top->io_llrIn_279 = llr_in[top->io_vnuLayerCounter][279];
        top->io_llrIn_280 = llr_in[top->io_vnuLayerCounter][280];
        top->io_llrIn_281 = llr_in[top->io_vnuLayerCounter][281];
        top->io_llrIn_282 = llr_in[top->io_vnuLayerCounter][282];
        top->io_llrIn_283 = llr_in[top->io_vnuLayerCounter][283];
        top->io_llrIn_284 = llr_in[top->io_vnuLayerCounter][284];
        top->io_llrIn_285 = llr_in[top->io_vnuLayerCounter][285];
        top->io_llrIn_286 = llr_in[top->io_vnuLayerCounter][286];
        top->io_llrIn_287 = llr_in[top->io_vnuLayerCounter][287];
        top->io_llrIn_288 = llr_in[top->io_vnuLayerCounter][288];
        top->io_llrIn_289 = llr_in[top->io_vnuLayerCounter][289];
        top->io_llrIn_290 = llr_in[top->io_vnuLayerCounter][290];
        top->io_llrIn_291 = llr_in[top->io_vnuLayerCounter][291];
        top->io_llrIn_292 = llr_in[top->io_vnuLayerCounter][292];
        top->io_llrIn_293 = llr_in[top->io_vnuLayerCounter][293];
        top->io_llrIn_294 = llr_in[top->io_vnuLayerCounter][294];
        top->io_llrIn_295 = llr_in[top->io_vnuLayerCounter][295];
        top->io_llrIn_296 = llr_in[top->io_vnuLayerCounter][296];
        top->io_llrIn_297 = llr_in[top->io_vnuLayerCounter][297];
        top->io_llrIn_298 = llr_in[top->io_vnuLayerCounter][298];
        top->io_llrIn_299 = llr_in[top->io_vnuLayerCounter][299];
        top->io_llrIn_300 = llr_in[top->io_vnuLayerCounter][300];
        top->io_llrIn_301 = llr_in[top->io_vnuLayerCounter][301];
        top->io_llrIn_302 = llr_in[top->io_vnuLayerCounter][302];
        top->io_llrIn_303 = llr_in[top->io_vnuLayerCounter][303];
        top->io_llrIn_304 = llr_in[top->io_vnuLayerCounter][304];
        top->io_llrIn_305 = llr_in[top->io_vnuLayerCounter][305];
        top->io_llrIn_306 = llr_in[top->io_vnuLayerCounter][306];
        top->io_llrIn_307 = llr_in[top->io_vnuLayerCounter][307];
        top->io_llrIn_308 = llr_in[top->io_vnuLayerCounter][308];
        top->io_llrIn_309 = llr_in[top->io_vnuLayerCounter][309];
        top->io_llrIn_310 = llr_in[top->io_vnuLayerCounter][310];
        top->io_llrIn_311 = llr_in[top->io_vnuLayerCounter][311];
        top->io_llrIn_312 = llr_in[top->io_vnuLayerCounter][312];
        top->io_llrIn_313 = llr_in[top->io_vnuLayerCounter][313];
        top->io_llrIn_314 = llr_in[top->io_vnuLayerCounter][314];
        top->io_llrIn_315 = llr_in[top->io_vnuLayerCounter][315];
        top->io_llrIn_316 = llr_in[top->io_vnuLayerCounter][316];
        top->io_llrIn_317 = llr_in[top->io_vnuLayerCounter][317];
        top->io_llrIn_318 = llr_in[top->io_vnuLayerCounter][318];
        top->io_llrIn_319 = llr_in[top->io_vnuLayerCounter][319];
        top->io_llrIn_320 = llr_in[top->io_vnuLayerCounter][320];
        top->io_llrIn_321 = llr_in[top->io_vnuLayerCounter][321];
        top->io_llrIn_322 = llr_in[top->io_vnuLayerCounter][322];
        top->io_llrIn_323 = llr_in[top->io_vnuLayerCounter][323];
        top->io_llrIn_324 = llr_in[top->io_vnuLayerCounter][324];
        top->io_llrIn_325 = llr_in[top->io_vnuLayerCounter][325];
        top->io_llrIn_326 = llr_in[top->io_vnuLayerCounter][326];
        top->io_llrIn_327 = llr_in[top->io_vnuLayerCounter][327];
        top->io_llrIn_328 = llr_in[top->io_vnuLayerCounter][328];
        top->io_llrIn_329 = llr_in[top->io_vnuLayerCounter][329];
        top->io_llrIn_330 = llr_in[top->io_vnuLayerCounter][330];
        top->io_llrIn_331 = llr_in[top->io_vnuLayerCounter][331];
        top->io_llrIn_332 = llr_in[top->io_vnuLayerCounter][332];
        top->io_llrIn_333 = llr_in[top->io_vnuLayerCounter][333];
        top->io_llrIn_334 = llr_in[top->io_vnuLayerCounter][334];
        top->io_llrIn_335 = llr_in[top->io_vnuLayerCounter][335];
        top->io_llrIn_336 = llr_in[top->io_vnuLayerCounter][336];
        top->io_llrIn_337 = llr_in[top->io_vnuLayerCounter][337];
        top->io_llrIn_338 = llr_in[top->io_vnuLayerCounter][338];
        top->io_llrIn_339 = llr_in[top->io_vnuLayerCounter][339];
        top->io_llrIn_340 = llr_in[top->io_vnuLayerCounter][340];
        top->io_llrIn_341 = llr_in[top->io_vnuLayerCounter][341];
        top->io_llrIn_342 = llr_in[top->io_vnuLayerCounter][342];
        top->io_llrIn_343 = llr_in[top->io_vnuLayerCounter][343];
        top->io_llrIn_344 = llr_in[top->io_vnuLayerCounter][344];
        top->io_llrIn_345 = llr_in[top->io_vnuLayerCounter][345];
        top->io_llrIn_346 = llr_in[top->io_vnuLayerCounter][346];
        top->io_llrIn_347 = llr_in[top->io_vnuLayerCounter][347];
        top->io_llrIn_348 = llr_in[top->io_vnuLayerCounter][348];
        top->io_llrIn_349 = llr_in[top->io_vnuLayerCounter][349];
        top->io_llrIn_350 = llr_in[top->io_vnuLayerCounter][350];
        top->io_llrIn_351 = llr_in[top->io_vnuLayerCounter][351];
        top->io_llrIn_352 = llr_in[top->io_vnuLayerCounter][352];
        top->io_llrIn_353 = llr_in[top->io_vnuLayerCounter][353];
        top->io_llrIn_354 = llr_in[top->io_vnuLayerCounter][354];
        top->io_llrIn_355 = llr_in[top->io_vnuLayerCounter][355];
        top->io_llrIn_356 = llr_in[top->io_vnuLayerCounter][356];
        top->io_llrIn_357 = llr_in[top->io_vnuLayerCounter][357];
        top->io_llrIn_358 = llr_in[top->io_vnuLayerCounter][358];
        top->io_llrIn_359 = llr_in[top->io_vnuLayerCounter][359];
        top->io_llrIn_360 = llr_in[top->io_vnuLayerCounter][360];
        top->io_llrIn_361 = llr_in[top->io_vnuLayerCounter][361];
        top->io_llrIn_362 = llr_in[top->io_vnuLayerCounter][362];
        top->io_llrIn_363 = llr_in[top->io_vnuLayerCounter][363];
        top->io_llrIn_364 = llr_in[top->io_vnuLayerCounter][364];
        top->io_llrIn_365 = llr_in[top->io_vnuLayerCounter][365];
        top->io_llrIn_366 = llr_in[top->io_vnuLayerCounter][366];
        top->io_llrIn_367 = llr_in[top->io_vnuLayerCounter][367];
        top->io_llrIn_368 = llr_in[top->io_vnuLayerCounter][368];
        top->io_llrIn_369 = llr_in[top->io_vnuLayerCounter][369];
        top->io_llrIn_370 = llr_in[top->io_vnuLayerCounter][370];
        top->io_llrIn_371 = llr_in[top->io_vnuLayerCounter][371];
        top->io_llrIn_372 = llr_in[top->io_vnuLayerCounter][372];
        top->io_llrIn_373 = llr_in[top->io_vnuLayerCounter][373];
        top->io_llrIn_374 = llr_in[top->io_vnuLayerCounter][374];
        top->io_llrIn_375 = llr_in[top->io_vnuLayerCounter][375];
        top->io_llrIn_376 = llr_in[top->io_vnuLayerCounter][376];
        top->io_llrIn_377 = llr_in[top->io_vnuLayerCounter][377];
        top->io_llrIn_378 = llr_in[top->io_vnuLayerCounter][378];
        top->io_llrIn_379 = llr_in[top->io_vnuLayerCounter][379];
        top->io_llrIn_380 = llr_in[top->io_vnuLayerCounter][380];
        top->io_llrIn_381 = llr_in[top->io_vnuLayerCounter][381];
        top->io_llrIn_382 = llr_in[top->io_vnuLayerCounter][382];
        top->io_llrIn_383 = llr_in[top->io_vnuLayerCounter][383];
        step();
    }

    tfp->close();
#ifdef READ_FROM_FILE
    fclose(llr_in_file);
#endif
    delete top;
    delete contextp;
    return 0;
}