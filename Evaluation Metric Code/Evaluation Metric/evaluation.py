import numpy as np
from sklearn.metrics import cluster
from scipy.optimize import linear_sum_assignment
from munkres import Munkres
from sklearn import metrics
import sklearn

def acc(y_true, y_pred):
    """
    Calculate clustering accuracy. Require scikit-learn installed
    # Arguments
        y: true labels, numpy.array with shape `(n_samples,)`
        y_pred: predicted labels, numpy.array with shape `(n_samples,)`
    # Return
        accuracy, in [0,1]
    """
    y_true = y_true.astype(np.int64)
    assert y_pred.size == y_true.size
    D = max(y_pred.max(), y_true.max()) + 1
    w = np.zeros((D, D), dtype=np.int64)
    for i in range(y_pred.size):
        w[y_pred[i], y_true[i]] += 1
    ind = linear_sum_assignment(w.max() - w)
    ind = np.array(ind).T
    #ind = linear_assignment(w.max()-w)
    return sum([w[i, j] for i, j in ind]) * 1.0 / y_pred.size

def pairwise_precision(y_true, y_pred):
    true_positives, false_positives, _, _ = _pairwise_confusion(y_true, y_pred)
    return true_positives / (true_positives + false_positives)

def pairwise_recall(y_true, y_pred):
    true_positives, _, false_negatives, _ = _pairwise_confusion(y_true, y_pred)
    return true_positives / (true_positives + false_negatives)

def _pairwise_confusion(y_true, y_pred):
    contingency = cluster.contingency_matrix(y_true, y_pred)
    same_class_true = np.max(contingency, 1)
    same_class_pred = np.max(contingency, 0)
    diff_class_true = contingency.sum(axis=1) - same_class_true
    diff_class_pred = contingency.sum(axis=0) - same_class_pred
    total = contingency.sum()

    true_positives = (same_class_true * (same_class_true - 1)).sum()
    false_positives = (diff_class_true * same_class_true * 2).sum()
    false_negatives = (diff_class_pred * same_class_pred * 2).sum()
    true_negatives = total * (
      total - 1) - true_positives - false_positives - false_negatives

    return true_positives, false_positives, false_negatives, true_negatives

def f_score(true_label, pred_label):
    # best mapping between true_label and predict label
    l1 = list(set(true_label))
    numclass1 = len(l1)

    l2 = list(set(pred_label))
    numclass2 = len(l2)
    if numclass1 != numclass2:
        # print('Class Not equal, Error!!!!')
        # return 0
        precision = pairwise_precision(true_label, pred_label)
        recall = pairwise_recall(true_label, pred_label)
        F1 = 2 * precision * recall / (precision + recall)
        return F1

    cost = np.zeros((numclass1, numclass2), dtype=int)
    for i, c1 in enumerate(l1):
        mps = [i1 for i1, e1 in enumerate(true_label) if e1 == c1]
        for j, c2 in enumerate(l2):
            mps_d = [i1 for i1 in mps if pred_label[i1] == c2]

            cost[i][j] = len(mps_d)

    # match two clustering results by Munkres algorithm
    m = Munkres()
    cost = cost.__neg__().tolist()

    indexes = m.compute(cost)

    # get the match results
    new_predict = np.zeros(len(pred_label))
    for i, c in enumerate(l1):
        # correponding label in l2:
        c2 = l2[indexes[i][1]]

        # ai is the index with label==c2 in the pred_label list
        ai = [ind for ind, elm in enumerate(pred_label) if elm == c2]
        new_predict[ai] = c

    acc = metrics.accuracy_score(true_label, new_predict)

    f1_macro = metrics.f1_score(true_label, new_predict, average='macro')
    return f1_macro

def jac_self(pred, true):
    # 1.生成one_hot
    #解决标签不一致的问题
    l1=list(set(pred))
    l2=list(np.arange(len(l1)))
    dic=dict(zip(l1,l2))
    for i in range(len(pred)):
        pred[i]=dic[pred[i]]

    k = np.max(true) + 1
    k1 = np.max(pred) + 1
    k2 = np.max(true) + 1
    #print(k)
    #print(pred.max())
    if pred.max() >= k:
        k = pred.max()+1
    P = np.eye(k)[pred]
    T = np.eye(k)[true]
    # 2.根据pred和true中的标签，将节点划分成簇
    p_set = []
    t_set = []
    for i in range(k):
        temp = []
        for j in range(P.shape[0]):
            if P[j][i] == 1:
                temp.append(j)
        p_set.append(temp)
    for i in range(k):
        temp = []
        for j in range(T.shape[0]):
            if T[j][i] == 1:
                temp.append(j)
        t_set.append(temp)
    one = 0
    for i in range(k):
        max_ji = 0
        for j in range(k):
            temp1 = set(p_set[i]).intersection(t_set[j])
            temp2 = set(p_set[i]).union(t_set[j])
            if len(temp2) == 0:
                temp = 0
            else:
                temp = len(temp1) / len(temp2)
            max_ji = max(max_ji, temp)
        one += max_ji
    one = one / (2 * k1)

    two = 0
    for i in range(k):
        max_ji = 0
        for j in range(k):
            temp1 = set(t_set[i]).intersection(p_set[j])
            temp2 = set(t_set[i]).union(p_set[j])
            if len(temp2) == 0:
                temp = 1
            else:
                temp = len(temp1) / len(temp2)
            max_ji = max(max_ji, temp)
        two += max_ji
    two = two / (2 * k2)
    return (one + two)

def modularity(adjacency, clusters):
  """Computes graph modularity.

  Args:
    adjacency: Input graph in terms of its sparse adjacency matrix.
    clusters: An (n,) int cluster vector.

  Returns:
    The value of graph modularity.
    https://en.wikipedia.org/wiki/Modularity_(networks)
  """
  degrees = adjacency.sum(axis=0).A1
  n_edges = degrees.sum()  # Note that it's actually 2*n_edges.
  result = 0
  for cluster_id in np.unique(clusters):
    cluster_indices = np.where(clusters == cluster_id)[0]
    adj_submatrix = adjacency[cluster_indices, :][:, cluster_indices]
    degrees_submatrix = degrees[cluster_indices]
    result += np.sum(adj_submatrix) - (np.sum(degrees_submatrix)**2) / n_edges
  return result / n_edges

def modularity_1(adjacency, clusters):
  """
  Computes the modularity of a graph partition.

  Args:
      adjacency (scipy.sparse.csr_matrix): The sparse adjacency matrix of the graph.
      clusters (np.ndarray): An array of cluster labels for each node.

  Returns:
      float: The modularity value.
  """
  n = adjacency.shape[0]
  degrees = adjacency.sum(axis=1).A1
  total_edges = adjacency.sum() / 2
  modularity = 0

  for cluster_id in np.unique(clusters):
    cluster_nodes = np.where(clusters == cluster_id)[0]
    cluster_size = len(cluster_nodes)
    cluster_edges = adjacency[cluster_nodes, :][:, cluster_nodes].sum() / 2
    cluster_degree_sum = np.sum(degrees[cluster_nodes])
    modularity += cluster_edges / total_edges - (cluster_degree_sum / (2 * total_edges)) ** 2

  return modularity

def eva_metrics(true, pred):
    Accuracy = acc(true, pred)
    NMI = sklearn.metrics.normalized_mutual_info_score(true, pred, average_method='arithmetic')
    JI = jac_self(true, pred)
    Accuracy = round(Accuracy, 4)
    NMI = round(NMI, 4)
    JI = round(JI, 4)
    return [Accuracy, NMI, JI]


