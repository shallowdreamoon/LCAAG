import numpy as np
from evaluation import eva_metrics


def get_true_lab(filename):
    with open(filename, 'r') as f:
        data = [line.strip().split(',') for line in f.readlines()]

    categories = sorted(set([category for _, category in data]))

    category_to_value = {category: idx for idx, category in enumerate(categories)}

    vector = [category_to_value[category] for _, category in data]
    node_names = [name for name, _ in data]

    return np.array(vector), np.array(node_names)

def get_pre_lab(filename, node_names):
    node_to_probs = {}
    with open(filename, 'r') as f:
        for line in f:
            parts = line.strip().split('\t')
            node_name = parts[0]
            probs = list(map(float, parts[1:]))
            node_to_probs[node_name] = probs

    matrix = []
    for node_name in node_names:
        if node_name in node_to_probs:
            matrix.append(node_to_probs[node_name])
        else:
            matrix.append([0.0] * len(node_to_probs[node_names[0]]))  # 假设概率向量长度一致
    probability_matrix = np.array(matrix)

    return probability_matrix

def get_evaluation_res(groundtruth_path = r"datasets\realData\cora\ground_truth", predict_path = r"clusters\datasets\realData\cora\clusters-1.01.01.0"):
    true_lab, node_idx = get_true_lab(groundtruth_path)

    pre_lab_mat = get_pre_lab(predict_path, node_idx)
    # print(pre_lab_mat)
    pre_lab = np.argmax(np.array(pre_lab_mat), axis=1)
    #print(pre_lab)

    res = eva_metrics(true_lab, pre_lab)
    return res
if __name__ == '__main__':
    evaluation_res = get_evaluation_res()






