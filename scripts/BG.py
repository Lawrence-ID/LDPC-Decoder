import os  # 新增：导入os模块用于目录操作
import numpy as np
import sys

def txt_to_matrix(file_path: str, delimiter: str = ',') -> np.ndarray:
    return np.loadtxt(file_path, delimiter=delimiter)

def read_csv_to_ndarray(file_path: str) -> np.ndarray:
    return np.loadtxt(file_path, delimiter=',')

def find_non_negative_one_indices(matrix: np.ndarray) -> (np.ndarray, np.ndarray, np.ndarray, np.ndarray):
    all_non_negative_one_indices = []
    all_non_negative_one_elements = []
    last_element_flags = []
    first_element_flags = []
    num_each_row = []

    for row_index, row in enumerate(matrix):
        non_negative_one_indices = np.where(row != -1)[0]
        non_negative_elements = row[row != -1]
        # print(f"{row_index} \t: {non_negative_one_indices.tolist()}, len = {len(non_negative_one_indices)}")
        # print(f"{row_index} \t: {non_negative_elements.tolist()}")

        is_last_col = np.zeros(len(non_negative_one_indices), dtype=bool)
        if len(is_last_col) > 0:
            is_last_col[-1] = True  # 将最后一个元素设置为 True
            # print(f"{row_index} \t: {is_last_col.tolist()}")
        
        is_first_col = np.zeros(len(non_negative_one_indices), dtype=bool)
        if len(is_first_col) > 0:
            is_first_col[0] = True  # 将第一个元素设置为 True
            # print(f"{row_index} \t: {is_last_col.tolist()}")

        all_non_negative_one_elements.append(non_negative_elements)
        all_non_negative_one_indices.append(non_negative_one_indices)
        last_element_flags.append(is_last_col)
        first_element_flags.append(is_first_col)
        num_each_row.append(len(non_negative_one_indices))

    return all_non_negative_one_indices, all_non_negative_one_elements, last_element_flags, first_element_flags, num_each_row

def print_message(matrix: np.ndarray, bg):
    col_idx, shift_value, is_last_col, is_first_col, num_each_row = find_non_negative_one_indices(matrix)

    print(f"BG{bg}RowNum: Int = {matrix.shape[0]},")
    print(f"BG{bg}ColNum: Int = {matrix.shape[1]},")
    
    print("/* NumAtLayer: */")
    print(f"BG{bg}NumAtLayer: Seq[Int] = Seq(")
    print(", ".join(map(str, num_each_row)))
    print("),\n")

    print(f"/* ColIdx (All column indices where elements are not -1):  */")
    print(f"BG{bg}ColIdx: Seq[Int] = Seq(")
    for row_index, row in enumerate(col_idx):
        print(f"/*{row_index}*/ {', '.join(map(str, row))}", end="")
        if row_index < len(col_idx) - 1:
            print(",")
    print("),\n")

    print(f"/* ShiftValue (All column value where elements are not -1): */")
    print(f"BG{bg}ShiftValue: Seq[Int] = Seq(")
    total_shift_value_elements = 0
    for row_index, value in enumerate(shift_value):
        value = [int(v) for v in value]
        total_shift_value_elements += len(value)
        print(f"/*{row_index}*/ {', '.join(map(str, value))}", end="")
        if row_index < len(shift_value) - 1:
            print(",")
    print(f"),\t/*len={total_shift_value_elements} */")

    print(f"/* IsFirstCol (Is last column flags by row): */")
    print(f"val BG{bg}isFirstCol: Seq[Boolean] = Seq(")
    total_is_first_col_elements = 0
    for row_index, flags in enumerate(is_first_col):
        total_is_first_col_elements += len(flags)
        print(f"/*{row_index}*/ {', '.join(map(lambda x: str(x).lower(), flags))}", end="")
        if row_index < len(is_first_col) - 1:
            print(",")
    print(f"),\t/* len={total_is_first_col_elements} */")

    print(f"/* IsLastCol (Is last column flags by row, length={len(is_last_col)}): */")
    print(f"val BG{bg}isLastCol: Seq[Boolean] = Seq(")
    for row_index, flags in enumerate(is_last_col):
        print(f"/*{row_index}*/ {', '.join(map(lambda x: str(x).lower(), flags))}", end="")
        if row_index < len(is_last_col) - 1:
            print(",")
    print(f"),\t/* len={total_is_first_col_elements} */")


def main():
    # 文件路径
    bg_list = [1, 2]
    iLS_list = [0, 1, 2, 3, 4, 5, 6, 7]
    code_rate_lists = {
        1: [(22, 68), (22, 27)],
        2: [(10, 52)]
    }

    for bg in bg_list:
        for iLS in iLS_list:
            code_rate_list = code_rate_lists.get(bg, [])
            if not code_rate_list:
                print(f"No code rates found for BG={bg}, skipping...")
                continue

            for code_rate in code_rate_list:

                row = code_rate[1] - code_rate[0]
                col = code_rate[1]

                csv_file_path = f"/home/g/Projects/LDPC-Decoder/scripts/BG{bg}/BG{bg}_iLS_{iLS}.csv"
                output_file_path = f"/home/g/Projects/LDPC-Decoder/scripts/build/BG{bg}_iLS_{iLS}_cr_{code_rate[0]}_{code_rate[1]}.txt"

                # 新增：检查并创建输出目录
                output_dir = os.path.dirname(output_file_path)
                if not os.path.exists(output_dir):
                    os.makedirs(output_dir)  # 创建目录（如果不存在）

                # 读取矩阵
                HMatrix = txt_to_matrix(csv_file_path, delimiter=',')
                print(f"HMatrix size: {HMatrix.shape[0], HMatrix.shape[1]}")
                if HMatrix.shape[0] < row or HMatrix.shape[1] < col:
                    raise ValueError("HMatrix is smaller than the specified row and column size.")
                HMatrix = HMatrix[:row, :col]
                
                
                print("--------------------------------------------------------------------------------------\n")
                print(f"BG={bg}, iLS={iLS}, HMatrix size: {HMatrix.size}\n")

                # 将输出重定向到文件
                with open(output_file_path, 'w') as f:
                    old_stdout = sys.stdout  # 保存原始标准输出
                    sys.stdout = f  # 将标准输出重定向到文件
                    print_message(HMatrix, bg)
                    sys.stdout = old_stdout  # 恢复原始标准输出

if __name__ == "__main__":
    main()
