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
width_bus = 32
bg = 1
f = 96
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
plt.rc('legend', fontsize=6)  # 图例字体大小

code_rate_lists = {
    1: [(22, 68, 628), (22, 27, 146)]
}

code_rate_list = code_rate_lists.get(bg, [])

# 新增：计算平均值的函数
def calculate_average(values):
    return sum(values) / len(values) if values else 0

linewidth = 3.48761
heightwidth = linewidth / 1.618
fig, ax = plt.subplots(figsize=(linewidth, heightwidth))

nread_color = ['darkorange', 'royalblue']
without_nread_color = ['orange', 'cornflowerblue']
color_ptr = 0

# 新增：设置 y 轴刻度精度
from matplotlib.ticker import MultipleLocator, FormatStrFormatter

for code_rate_ncalc in code_rate_list:

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
    
    # print(throughput_values)
    # print(throughput_without_nread_values)
    # 计算平均值
    avg_throughput = calculate_average(throughput_values)
    avg_throughput_without_nread = calculate_average(throughput_without_nread_values)

    # 新增：绘制折线图
    ax.plot(all_zc, throughput_values, marker='o', linestyle='-', label=f'CR=22/{n_b} with $n_{{read}}$', color=nread_color[color_ptr], markersize=3)
    ax.plot(all_zc, throughput_without_nread_values, marker='x', linestyle='--', label=f'CR=22/{n_b} without $n_{{read}}$', color=without_nread_color[color_ptr], markersize=3)
    color_ptr += 1

    # 修改: 合并后的图例标签
    ax.set_xlabel(f'Lifting Size $Z_c$ \n a) $w_{{bus}}$={width_bus}, $f$={f}')
    ax.set_ylabel('Throughput(G/bps)')

# 修改: 设置 y 轴刻度间隔为 0.1
# ax.yaxis.set_major_locator(MultipleLocator(0.1))
# 设置 y 轴刻度格式为小数形式
ax.yaxis.set_major_formatter(FormatStrFormatter('%.1f'))

# 修改: 添加图例
ax.legend(loc='upper left')

# 修改: 调整布局
plt.subplots_adjust(right=0.8)

# 保存图像为PDF文件
output_dir = "/home/g/Projects/LDPC-Decoder/RAPPB/build/throughput"
if not os.path.exists(output_dir):
    os.makedirs(output_dir)
output_file_path = os.path.join(output_dir, f"throughput_BG{bg}_vs_0nread_{width_bus}.pdf")
plt.savefig(output_file_path, dpi=300, format="pdf", bbox_inches='tight')
plt.close(fig)

