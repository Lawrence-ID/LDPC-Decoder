import math
import matplotlib.pyplot as plt
import matplotlib.ticker as ticker  # 新增：导入ticker模块

def cal_avg(values):
    """
    Calculate the average of a list of values.
    
    Parameters:
        values (list): A list of numeric values.
    
    Returns:
        float: The average of the values.
    """
    if not values:
        return 0
    return sum(values) / len(values)

def get_factors(n):
    """
    Calculate all factors of a value n
    """
    factors = []
    for i in range(1, int(math.sqrt(n)) + 1):
        if n % i == 0:
            factors.append(i)
            if i != n // i:
                factors.append(n // i)
    factors.sort()
    return factors

def find_intersection(list1, list2):
    """
    Calculate the intersection of two lists.
    
    Parameters:
        list1 (list): The first list.
        list2 (list): The second list.
    
    Returns:
        list: The intersection of the two lists (sorted and deduplicated).
    """
    return sorted(list(set(list1) & set(list2)))

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

width_bus_list = [32, 64, 128, 256]  # 修改: 将width_bus定义为一个列表
bg_list = [0, 1]

# New: Enable LaTeX rendering
plt.rc('text', usetex=True)
plt.rc('font', family='serif')

# 修改: 设置全局字体大小
plt.rc('font', size=8)  # 全局字体大小
plt.rc('axes', titlesize=8)  # 子图标题字体大小
plt.rc('axes', labelsize=8)  # 坐标轴标签字体大小
plt.rc('xtick', labelsize=8)  # x轴刻度字体大小
plt.rc('ytick', labelsize=8)  # y轴刻度字体大小
plt.rc('legend', fontsize=6)  # 图例字体大小

linewidth = 3.48761
heightwidth = linewidth / 1.3

# Process each width_bus
for width_bus in width_bus_list:
    zc_max = max(all_zc)
    n_read_dict = {}
    route_pattern_dict = {}

    # Create a canvas with two subplots
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(linewidth, heightwidth), sharey=True)  # 修改: 不共享纵坐标

    file = open(f"n_read_vs_Zc_{width_bus}.txt", "w")
    # Process cases for bg==0 and bg==1 separately
    for bg, ax in zip(bg_list, [ax1, ax2]):
        n_b = 68 if bg == 0 else 46
        factors = find_intersection(get_factors(zc_max), get_factors(int(zc_max*8 / width_bus)))
        print(factors)

        # Print results
        file.write(f"\nbg = {bg}\n")
        file.write("All possible values of Zc in 5G NR LDPC (total {}):\n".format(len(all_zc)))
        file.write("f ,\t group_width ,\t route_pattern ,\t ideal_pattern ,\t avg_n_read ,\t max_n_read\n")
        for f in factors:
            z_new_list = set()
            n_read_values = []  # New: Store n_read values for the current factor
            for idx, z in enumerate(all_zc):
                group_width = zc_max / f
                z_new = int(math.ceil(z / group_width) * group_width)
                n_read = z_new * n_b * 8 / width_bus
                z_new_list.add(z_new)
                n_read_values.append(n_read)  # New: Add n_read value to the list
            n_read_dict[f] = n_read_values  # New: Store n_read values for the current factor in the dictionary
            route_pattern_dict[f] = len(z_new_list)
            file.write(f"{f:5d},\t {group_width:10.0f},\t {len(z_new_list):10.0f},\t {zc_max / group_width:10.1f},\t {cal_avg(n_read_dict[f]):10.1f},\t {max(n_read_dict[f]):10.1f}\n")

        # New: Plot line graph in the corresponding subplot
        for f, n_read_values in n_read_dict.items():
            ax.plot(all_zc, n_read_values, label=f"$f$={f}")  # 修改图例标签

        if bg == 0:
            ax.set_xlabel("lifting size $Z_c$ \n a) BG0,R=1/3")
            ax.set_ylabel("$n_{{read}}$(Cycles)")  # 纵坐标标签仅在第一个子图显示
        elif bg == 1:
            ax.set_xlabel("lifting size $Z_c$ \n b) BG1, R=1/5")

        ax.legend(loc='lower right')  # 将图例放置在图表内左下角
        ax.grid(True)

        # New: Clear n_read_dict to ensure independence between different bg
        n_read_dict.clear()
        route_pattern_dict.clear()

    # Uniform vertical axis scale
    fig.tight_layout()

    # Save the image to the current directory as a vector format (PDF)
    plt.savefig(f"n_read_vs_Zc_{width_bus}.pdf", dpi=300, format="pdf")  # 修改文件名以反映不同的width_bus
    plt.close(fig)  # 关闭当前图形，避免重复绘制