import numpy as np

def txt_to_matrix(file_path: str, delimiter: str = ',') -> np.ndarray:
    return np.loadtxt(file_path, delimiter=delimiter)

def find_non_negative_one_indices(matrix: np.ndarray) -> (np.ndarray, np.ndarray, np.ndarray):
    all_non_negative_one_indices = []
    last_element_flags = []
    num_each_row = []

    for row_index, row in enumerate(matrix):
        non_negative_one_indices = np.where(row != -1)[0]
        # print(f"{row_index} \t: {non_negative_one_indices.tolist()}, len = {len(non_negative_one_indices)}")

        is_last_col = np.zeros(len(non_negative_one_indices), dtype=bool)
        if len(is_last_col) > 0:
            is_last_col[-1] = True  # 将最后一个元素设置为 True
            # print(f"{row_index} \t: {is_last_col.tolist()}")

        all_non_negative_one_indices.append(non_negative_one_indices)
        last_element_flags.append(is_last_col)
        num_each_row.append(len(non_negative_one_indices))

    return all_non_negative_one_indices, last_element_flags, num_each_row

def print_message(matrix: np.ndarray):
    col_idx, is_last_col, num_each_row = find_non_negative_one_indices(matrix)
    print("All column indices where elements are not -1:")
    for row_index, row in enumerate(col_idx):
        print(f"/*{row_index}*/ {', '.join(map(str, row))},")

    print("Is last column flags by row:")
    for row_index, flags in enumerate(is_last_col):
        print(f"/*{row_index}*/ {', '.join(map(lambda x: str(x).lower(), flags))},")  # 将 True 和 False 转为小写
    print(f"num_each_row:  ${num_each_row}")


def main():
    # 文件路径
    NR_BG1_384 = '/home/ubuntu/Projects/LDPC-Decoder/scripts/NR_BG1_384.txt'
    NR_BG2_384 = '/home/ubuntu/Projects/LDPC-Decoder/scripts/NR_BG2_384.txt'

    # 读取矩阵
    BG1 = txt_to_matrix(NR_BG1_384, delimiter=',')
    BG2 = txt_to_matrix(NR_BG2_384, delimiter=',')
    
    print("BG1 size:", BG1.size)
    print("BG2 size:", BG2.size)

    print_message(BG1)
    print_message(BG2)

if __name__ == "__main__":
    main()
