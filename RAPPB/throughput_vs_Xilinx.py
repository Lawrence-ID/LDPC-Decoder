import math
import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import os

z_sets = [
    [2, 4, 8, 16, 32, 64, 128, 256],       # Set 0
    [3, 6, 12, 24, 48, 96, 192, 384],     # Set 1
    [5, 10, 20, 40, 80, 160, 320],        # Set 2
    [7, 14, 28, 56, 112, 224],            # Set 3
    [9, 18, 36, 72, 144, 288],            # Set 4
    [11, 22, 44, 88, 176, 352],           # Set 5
    [13, 26, 52, 104, 208],               # Set 6
    [15, 30, 60, 120, 240]                # Set 7
]

# Merge all Zc values from subsets and remove duplicates
all_zc = sorted({z for subset in z_sets for z in subset})
width_bus = 256
bg = 1
f = 12
zc_max = 384
freq = 266
it_max = 8

# New: Enable LaTeX rendering
plt.rc('text', usetex=True)
plt.rc('font', family='serif')

# 修改: 设置全局字体大小
plt.rc('font', size=8)  # 全局字体大小
plt.rc('axes', titlesize=8)  # 子图标题字体大小
plt.rc('axes', labelsize=8)  # 坐标轴标签字体大小
plt.rc('xtick', labelsize=6)  # x轴刻度字体大小
plt.rc('ytick', labelsize=6)  # y轴刻度字体大小
plt.rc('legend', fontsize=8)  # 图例字体大小

code_rate_lists = {
    1: [(22, 68, 628), (22, 27, 146)]
}

xilinx_throughput_values_cr_22_68 = [
    0.01, 0.0192,0.0285,0.0377,0.0469,0.0562,0.0654,0.0746,0.0838,0.0931,0.1023,0.1115,0.1208,0.1300,0.1392,0.1577,0.1762,0.1946,0.2131,0.2315, 0.25, 0.065, 0.0690,0.0770,0.0849,0.0929,0.1008,0.1088,0.1167,0.1247,0.1327,0.1486,0.1645,0.1804,0.1963,0.2122,0.2282,0.2441, 0.26, 0.145, 0.1593,0.1736,0.1879,0.2021,0.2164,0.2307, 0.25, 0.13, 0.1417,0.1533, 0.165
]

xilinx_throughput_values_cr_22_27 = [
    0.01, 0.0519,0.0938,0.1358,0.1777,0.2196,0.2615,0.3035,0.3454,0.3873,0.4292,0.4712,0.5131,0.5550,0.5969,0.6808,0.7646,0.8485,0.9323,1.0162, 1.1, 
    0.25, 0.2669,0.3008,0.3347,0.3686,0.4024,0.4363,0.4702,0.5041,0.5380,0.6057,0.6735,0.7412,0.8090,0.8767,0.9445,1.0122, 1.08, 
    0.55, 0.6114,0.6729,0.7343,0.7957,0.8571,0.9186, 0.990, 
    0.7,0.7500,0.8000 , 0.85
]
code_rate_list = code_rate_lists.get(bg, [])

# 新增：计算平均值的函数
def calculate_average(values):
    return sum(values) / len(values) if values else 0

linewidth = 3.48761
heightwidth = linewidth / 1.618
fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(linewidth, heightwidth*2+1), sharey=False)

# 新增：设置 y 轴刻度精度
from matplotlib.ticker import MultipleLocator, FormatStrFormatter

for code_rate_ncalc, ax in zip(code_rate_list, [ax1, ax2]):

    n_read_values = []
    code_word_length_values = []
    throughput_values = []
    throughput_without_nread_values = []

    row = code_rate_ncalc[1] - code_rate_ncalc[0]
    col = code_rate_ncalc[1]
    n_calc = code_rate_ncalc[2]
    n_b = col

    for z in all_zc:
        group_width = zc_max / f
        z_new = int(math.ceil(z / group_width) * group_width)
        n_read = z_new * n_b * 8 / width_bus
        n_read_values.append(n_read)

        code_word_length = z * n_b
        code_word_length_values.append(code_word_length)

        throughput = (code_word_length * freq) / (n_calc*it_max + n_read) / 1000 # G/bps
        throughput_values.append(throughput)

        throughput_without_nread = (code_word_length * freq) / (n_calc * it_max) / 1000 # G/bps
        throughput_without_nread_values.append(throughput_without_nread)

    xilinx_throughput_values = []
    if n_b == 68:
        xilinx_throughput_values = xilinx_throughput_values_cr_22_68
    elif n_b == 27:
        xilinx_throughput_values = xilinx_throughput_values_cr_22_27
    
    print(throughput_values)
    print(throughput_without_nread_values)
    # 计算平均值
    avg_throughput = calculate_average(throughput_values)
    avg_throughput_without_nread = calculate_average(throughput_without_nread_values)
    avg_xilinx_throughput = calculate_average(xilinx_throughput_values)

    # 新增：绘制折线图
    ax.plot(all_zc, throughput_values, marker='o', linestyle='-', label=f'Proposed', markersize=3)
    ax.plot(all_zc, xilinx_throughput_values, marker='x', linestyle='--', label=f'Xilinx', markersize=3)

    if n_b == 68:
        ax.set_xlabel(f'Lifting Size $Z_c$ \n a) BG1, CR=22/{n_b}')
    elif n_b == 27:
        ax.set_xlabel(f'Lifting Size $Z_c$ \n b) BG1, CR=22/{n_b}')
    ax.set_ylabel('Throughput(G/bps)')
    # ax.set_title(f'a) BG1, R=22/{n_b}')  # 修改: 删除 loc='bottom' 参数，仅保留 y=-0.2 调整标题位置
    ax.legend(loc='upper left')  # 添加图例

    # 设置 y 轴刻度间隔为 0.1
    # ax.yaxis.set_major_locator(MultipleLocator(0.1))
    # 设置 y 轴刻度格式为小数形式
    ax.yaxis.set_major_formatter(FormatStrFormatter('%.1f'))

# 新增：调整子图之间的间距
plt.subplots_adjust(hspace=0.3, top=0.99, bottom=0.1)  # 减少顶部和底部的空白

# 保存图像为PDF文件
output_dir = "/home/g/Projects/LDPC-Decoder/RAPPB/build/throughput"
if not os.path.exists(output_dir):
    os.makedirs(output_dir)
output_file_path = os.path.join(output_dir, f"throughput_BG{bg}.pdf")
plt.savefig(output_file_path, dpi=300, format="pdf")
plt.close(fig)
