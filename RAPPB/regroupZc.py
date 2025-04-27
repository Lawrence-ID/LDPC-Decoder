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

width_bus = 256
zc_max = max(all_zc)
bg_list = [0, 1]

# bg0 factors: [1, 2, 3, 4, 6, 8, 12, 16, 24, 48]
# bg1 factors: [1, 2, 3, 4, 6, 8, 12, 24]

# New: Used to store n_read elements for different factors
n_read_dict = {}
route_pattern_dict = {}

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

# Create a canvas with two subplots
fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(linewidth, heightwidth), sharey=True)  # 修改: 不共享纵坐标

# Process cases for bg==0 and bg==1 separately
for bg, ax in zip(bg_list, [ax1, ax2]):
    n_b = 68 if bg == 0 else 46
    factors = find_intersection(get_factors(zc_max), get_factors(int(zc_max*n_b*8 / width_bus)))
    print(factors)

    # Print results
    print(f"\nbg = {bg}")
    print("All possible values of Zc in 5G NR LDPC (total {}):".format(len(all_zc)))
    
    # Only process factors 1, 3, 6, 12, 24
    # filtered_factors = [f for f in factors if f in [1, 3, 6, 12, 24]]
    
    for f in factors:
        z_new_list = set()
        n_read_values = []  # New: Store n_read values for the current factor
        for idx, z in enumerate(all_zc):
            group_width = zc_max / f
            z_new = int(math.ceil(z / group_width) * group_width)
            n_read = z_new * n_b * 8 / width_bus
            z_new_list.add(z_new)
            n_read_values.append(n_read)  # New: Add n_read value to the list
            # print(f"group_width = {group_width}, Zc[{idx+1}] = {z} -> Zc_new = {z_new}, n_read = {n_read}")
        n_read_dict[f] = n_read_values  # New: Store n_read values for the current factor in the dictionary
        route_pattern_dict[f] = len(z_new_list)
        print(f"f = {f}, group_width = {group_width}, route_pattern = {len(z_new_list)}, ideal_pattern = {zc_max / group_width}, avg_n_read = {cal_avg(n_read_dict[f])}")
        print(f"-------------------------------------------------------------------------------------------")

    # New: Print n_read elements for different factors
    # print("\nN_read elements for different factors: ")
    # for f, n_read_values in n_read_dict.items():
    #     print(f"factor = {f}, n_read = {n_read_values}")

    # New: Plot line graph in the corresponding subplot
    # 修改图例位置，放置在图表左下角
    for f, n_read_values in n_read_dict.items():
        ax.plot(all_zc, n_read_values, label=f"$f$={f}")  # 修改图例标签

    if bg == 0:
        # ax.set_title(f"$n_{{read}}$ for Different $f$")
        ax.set_xlabel("lifting size $Z_c$ \n a) $BG$=0")
        ax.set_ylabel("$n_{{read}}$(Cycles)")  # 纵坐标标签仅在第一个子图显示
    elif bg == 1:
        ax.set_xlabel("lifting size $Z_c$ \n b) $BG$=1")

    ax.legend(loc='lower right')  # 将图例放置在图表内左下角
    ax.grid(True)

    # New: Clear n_read_dict to ensure independence between different bg
    n_read_dict.clear()
    route_pattern_dict.clear()

# Uniform vertical axis scale
fig.tight_layout()

# Save the image to the current directory as a vector format (PDF)
plt.savefig("n_read_vs_Zc_stacked.pdf", dpi=300, format="pdf")  # 修改文件名以反映上下布局

plt.show()

print("done")