package cn.edu.whut.cszhtang.vblcd;

import cn.edu.whut.cszhtang.vblcd.data.GraphDataReader;
import cn.edu.whut.cszhtang.vblcd.model.CommonMethods;
import cn.edu.whut.cszhtang.vblcd.model.VariationalBayesClustering;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

public class App {
    private static Logger logger = Logger.getLogger(App.class);

    public static void main(String[] args) {
        // Predefined values for sigma, mu, lambda
        float sigmaValues = 1.0f;
        float muValues = 1.0f;
        float lambdaValues = 1.0f;
        String[] fixedArgs = new String[]{
                "datasets/realData/cora",
                "7",
                "10",
                "100",
                "0.0001"
        };

        String[] customArgs = new String[fixedArgs.length + 3];
        System.arraycopy(fixedArgs, 0, customArgs, 0, fixedArgs.length);

        customArgs[fixedArgs.length] = String.valueOf(sigmaValues);
        customArgs[fixedArgs.length + 1] = String.valueOf(muValues);
        customArgs[fixedArgs.length + 2] = String.valueOf(lambdaValues);
        runCoraAndCiteseer(customArgs);
    }

    private static void runRemainderDatasets(String[] args) {
        int numThreads = Integer.parseInt(args[0]);
        String rootFolder = "remainder";
        int maxLoops = 10;
        float maxDiff = (float) 0.1;
        String res_path = args[5]+args[6]+args[7];
        float sigma = Float.parseFloat(args[5]);
        float mu = Float.parseFloat(args[6]);
        float lambda = Float.parseFloat(args[7]);

        String summaryPath = rootFolder + File.separator + "summary";
        Map<String, Integer> numClustersOfDatasetsMap = getNumClustersOfDatasetsMap(summaryPath);

        File rootFolderFile = new File(rootFolder);
        for (String graphFolder : rootFolderFile.list()
        ) {
            if ("summary".contentEquals(graphFolder)) {
                continue;
            }
            int numClusters = numClustersOfDatasetsMap.get(graphFolder);
            String subFolder = rootFolder + File.separator + graphFolder;
            String graphFile = subFolder + File.separator + "graph";
            GraphDataReader graphDataReader = new GraphDataReader(graphFile);
            graphDataReader.run();

            for (int i = 0; i < 1; i++) {
                runExperimentsAndRecordTime(subFolder, numClusters, numThreads, maxLoops, maxDiff, graphDataReader, res_path, sigma, mu, lambda);

            }
        }

    }

    private static void runCoraAndCiteseer(String[] args) {
        String rootFolder = args[0];
        int numClusters = Integer.parseInt(args[1]);
        int numThreads = Integer.parseInt(args[2]);
        int maxLoops = Integer.parseInt(args[3]);
        float maxDiff = Float.parseFloat(args[4]);
        String res_path = args[5]+args[6]+args[7];
        float sigma = Float.parseFloat(args[5]);
        float mu = Float.parseFloat(args[6]);
        float lambda = Float.parseFloat(args[7]);

        int numRuns = args.length == 6 ? Integer.parseInt(args[5]) : 1;

        String graphFile = rootFolder + File.separator + "graph";

        long t1 = System.currentTimeMillis();

        GraphDataReader graphDataReader = new GraphDataReader(graphFile);
        graphDataReader.run();

        long t2 = System.currentTimeMillis();
        logger.info("runtime(GraphDataReader): " + (t2 - t1));

//        saveGraphInformation(graphDataReader, rootFolder);

        for (int i = 0; i < numRuns; i++) {
            System.out.println(i);
            runExperimentsAndRecordTime(rootFolder, numClusters, numThreads, maxLoops, maxDiff, graphDataReader, res_path, sigma, mu, lambda);
        }
    }

//    private static void saveGraphInformation(GraphDataReader graphDataReader, String rootFolder) {
//        byte[][] incidenceMatrixArray = graphDataReader.getIncidenceMatrixArray();
//        byte[][] attributeMatrixArray = graphDataReader.getAttributeMatrixArray();
//
//    }

    private static void runExperimentsAndRecordTime(String rootFolder, int numClusters, int numThreads, int maxLoops, float maxDiff, GraphDataReader graphDataReader, String res_path, float sigma, float mu, float lambda) {
        long startTime = System.currentTimeMillis();
        runExperiments(rootFolder, numThreads, maxLoops, maxDiff, numClusters, graphDataReader, res_path, sigma, mu, lambda);
        long endTime = System.currentTimeMillis();
        long runtime = endTime - startTime;
        logger.info("runtime: " + runtime);
        String runtimeFolder = "runtime" + File.separator + rootFolder;
        CopySubDirectory(runtimeFolder);
        String runtimePath = runtimeFolder + File.separator + "runtime-" + System.currentTimeMillis();

        CommonMethods.saveToFile(runtime + "", runtimePath);
    }

    private static void CopySubDirectory(String folder) {
        File file = new File(folder);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                logger.error("File path [" + folder + "] does not exist.");
            }
        }
    }

    private static void runRealData(String[] args) {
        String rootFolder = args[0];
        int numThreads = Integer.parseInt(args[1]);
        int maxLoops = Integer.parseInt(args[2]);
        float maxDiff = Float.parseFloat(args[3]);
        String res_path = args[5]+args[6]+args[7];
        float sigma = Float.parseFloat(args[5]);
        float mu = Float.parseFloat(args[6]);
        float lambda = Float.parseFloat(args[7]);

        List<String> NMIAs0GraphList = getNMIAs0GraphList(rootFolder);

        String[] datasets = {"twitter", "facebook"};
        List<Map<String, Integer>> numClustersOfDatasetsMapList = getNumClustersOfDatasetsMapList(rootFolder, datasets);
        for (int i = 0; i < datasets.length; i++) {
            for (String graphFolder : numClustersOfDatasetsMapList.get(i).keySet()) {
                if (graphFolder.contentEquals("110538600381916983600") //Java heap space
                        || graphFolder.contentEquals("111091089527727420853")//Java heap space
                        || graphFolder.contentEquals("115625564993990145546")//It takes too long
                        || graphFolder.contentEquals("104987932455782713675")//Java heap space
                        || graphFolder.contentEquals("103236949470535942612")//Java heap space
                        || graphFolder.contentEquals("113718775944980638561")// File is too large
                        || graphFolder.contentEquals("107223200089245371832")//It takes too long
                        || graphFolder.contentEquals("114147483140782280818")//It takes too long
                        || graphFolder.contentEquals("1912")) {
                    continue;
                }

                List<String> poorGraphList = getPoorGraphList("poor");
                List<String> poorGraphList2 = getPoorGraphList("poor2");
                if (!(poorGraphList.contains(graphFolder) || poorGraphList2.contains(graphFolder))) {
                    continue;
                }

//                if (!NMIAs0GraphList.contains(graphFolder)) {
//                    continue;
//                }

                String subFolder = rootFolder + File.separator + datasets[i] + File.separator + graphFolder;

                String resultFilePath = "clusters" + File.separator + subFolder;
                File resultFile = new File(resultFilePath);
                if (!resultFile.exists()) {
                    continue;
                }
                int numAvailableResults = resultFile.list().length;
                if (numAvailableResults >= 20) {
                    continue;
                }

                logger.info(subFolder);
                int numClusters = numClustersOfDatasetsMapList.get(i).get(graphFolder);

                String graphFile = subFolder + File.separator + "graph";

                GraphDataReader graphDataReader = new GraphDataReader(graphFile);
                graphDataReader.run();

                for (int j = 0; j < 20 - numAvailableResults; j++) {
                    runExperimentsAndRecordTime(subFolder, numClusters, numThreads, maxLoops, maxDiff, graphDataReader, res_path, sigma, mu, lambda);
                }
            }
        }

    }

    private static List<String> getPoorGraphList(String poor) {
        List<String> poorGraphList = new ArrayList<String>();

        BufferedReader br;
        String line;
        try {
            br = new BufferedReader(new FileReader("poor"));
            while ((line = br.readLine()) != null) {
                poorGraphList.add(line.trim());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return poorGraphList;
    }

    private static List<String> getNMIAs0GraphList(String rootFolder) {
        List<String> list = new ArrayList<String>();
        BufferedReader br;
        String line;
        try {
            br = new BufferedReader(new FileReader(rootFolder + File.separator + "NMIAs0Graph"));
            while ((line = br.readLine()) != null) {
                list.add(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    private static void runExperiments(String rootFolder, int numThreads, int maxLoops, float maxDiff, int numClusters, GraphDataReader graphDataReader, String res_path, float sigma, float mu, float lambda) {

        VariationalBayesClustering clustering = new VariationalBayesClustering(numClusters, numThreads, maxLoops,
                maxDiff, graphDataReader.getIncidenceMatrixArray(), graphDataReader.getAttributeMatrixArray(), sigma, mu, lambda);

        clustering.setInitVAlphaArray(null);
        clustering.run();

        String resultFolder = "clusters" + File.separator + rootFolder;
        CopySubDirectory(resultFolder);

        String resultFile = resultFolder + File.separator + "clusters-" + res_path;
        //System.out.println(Arrays.deepToString(clustering.getClusteringResult()));
        saveClusteringResult(resultFile, clustering.getClusteringResult(), graphDataReader.getEdge2IndexMap());

    }

    private static void saveEdgeClusteringResult(String resultFile, float[][] clusteringResult, Map<List<String>, Integer> edge2IndexMap) {

        String content = "";
        for (int i = 0; i < clusteringResult.length; i++) {
            resultFile += edge2IndexMap.get(i) + "\t";
            for (int j = 0; j < clusteringResult[i].length; j++) {
                resultFile += clusteringResult[i][j] + "\t";
            }
            resultFile += "\n";
        }
        CommonMethods.saveToFile(content, resultFile);
    }

    private static float[][] getPreEdgeVAlphaArray(String filePath, int numClusters, GraphDataReader graphDataReader) {

        Map<List<String>, Integer> edge2IndexMap = graphDataReader.getEdge2IndexMap();
        Map<String, Integer> vertex2IndexMap = graphDataReader.getVertex2IndexMap();
        int numEdges = edge2IndexMap.size();
        int numVertexs = vertex2IndexMap.size();

        float[][] preVertexVAlphaArray = new float[numVertexs][numClusters];
        float[][] preEdgeVAlphaArray = new float[numEdges][numClusters];

        BufferedReader br;
        String line;
        try {
            br = new BufferedReader(new FileReader(filePath));
            while ((line = br.readLine()) != null) {
                String[] item = line.split(Constants.DELIMITER);
                int vertexIndex = vertex2IndexMap.get(item[0]) - 1;
                for (int i = 0; i < numClusters; i++) {
                    preVertexVAlphaArray[vertexIndex][i] = Float.parseFloat(item[i + 1]);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (List<String> edge :
                edge2IndexMap.keySet()) {
            int edgeIndex = edge2IndexMap.get(edge) - 1;
            for (String vertex :
                    edge) {
                int vertexIndex = vertex2IndexMap.get(vertex) - 1;
                for (int i = 0; i < numClusters; i++) {
                    preEdgeVAlphaArray[edgeIndex][i] += preVertexVAlphaArray[vertexIndex][i] / 2;
                }
            }
        }

        return preEdgeVAlphaArray;
    }

    private static List<Map<String, Integer>> getNumClustersOfDatasetsMapList(String rootFolder, String[] datasets) {
        List<Map<String, Integer>> numClustersOfDatasetsMapList = new ArrayList<Map<String, Integer>>();

        for (int i = 0; i < datasets.length; i++) {
            String summaryPath = rootFolder + File.separator + datasets[i] + File.separator + "summary";
            Map<String, Integer> numClustersOfDatasetsMap = getNumClustersOfDatasetsMap(summaryPath);
            numClustersOfDatasetsMapList.add(numClustersOfDatasetsMap);
        }
        return numClustersOfDatasetsMapList;
    }

    private static Map<String, Integer> getNumClustersOfDatasetsMap(String filePath) {
        Map<String, Integer> numClustersOfDatasetsMap = new HashMap<String, Integer>();
        String line;
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            while ((line = br.readLine()) != null) {
                String[] item = line.split(Constants.COMMA);
                int value = Integer.parseInt(item[1]);
                numClustersOfDatasetsMap.put(item[0], value);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return numClustersOfDatasetsMap;
    }

    private static void runSyntheticData(String[] args) {
        String rootFolder = args[0];
        int numClusters = Integer.parseInt(args[1]);
        int numAttributes = Integer.parseInt(args[2]);
        int numThreads = Integer.parseInt(args[3]);
        int maxLoops = Integer.parseInt(args[4]);
        float maxDiff = Float.parseFloat(args[5]);
        String res_path = args[5]+args[6]+args[7];
        float sigma = Float.parseFloat(args[5]);
        float mu = Float.parseFloat(args[6]);
        float lambda = Float.parseFloat(args[7]);

        String graphFile = rootFolder + File.separator + "graph";

        String[] attributeFiles = new String[numAttributes];
        for (int i = 1; i <= attributeFiles.length; i++) {
            attributeFiles[i - 1] = rootFolder + File.separator + "attribute-" + i;
        }

        GraphDataReader graphDataReader = new GraphDataReader(graphFile, attributeFiles);
        graphDataReader.run();

        runExperimentsAndRecordTime(rootFolder, numClusters, numThreads, maxLoops, maxDiff, graphDataReader, res_path, sigma, mu, lambda);
    }

    private static void saveClusteringResult(String file, float[][] clusteringResult,
                                             Map<List<String>, Integer> edge2IndexMap) {
        Map<String, float[]> vertex2ClusterMap = new HashMap<String, float[]>();

        for (List<String> edge : edge2IndexMap.keySet()) {

            int index = edge2IndexMap.get(edge);

            if (!vertex2ClusterMap.containsKey(edge.get(0))) {
                vertex2ClusterMap.put(edge.get(0), clusteringResult[index - 1]);
            } else {
                for (int i = 0; i < clusteringResult[index - 1].length; i++) {
                    vertex2ClusterMap.get(edge.get(0))[i] = adjustClusters(vertex2ClusterMap.get(edge.get(0))[i],
                            clusteringResult[index - 1][i]);
                }
            }
            if (!vertex2ClusterMap.containsKey(edge.get(1))) {
                vertex2ClusterMap.put(edge.get(1), clusteringResult[index - 1]);
            } else {
                for (int i = 0; i < clusteringResult[index - 1].length; i++) {
                    vertex2ClusterMap.get(edge.get(1))[i] = adjustClusters(vertex2ClusterMap.get(edge.get(1))[i],
                            clusteringResult[index - 1][i]);
                }
            }

        }

        // Adjust the value of the cluster
        for (String vertex : vertex2ClusterMap.keySet()) {
            float[] clusters = vertex2ClusterMap.get(vertex);
            float sum = 0;
            for (float cluster : clusters) {
                sum += cluster;
            }
            for (int i = 0; i < clusters.length; i++) {
                clusters[i] /= sum;
            }
            vertex2ClusterMap.put(vertex, clusters);
        }

        String content = "";
        List<String> onlyOne = new ArrayList<String>();
        for (List<String> edge : edge2IndexMap.keySet()) {
            if (!onlyOne.contains(edge.get(0))) {
                onlyOne.add(edge.get(0));
                content += (content.length() == 0 ? "" : "\n") + edge.get(0);
                for (float f : vertex2ClusterMap.get(edge.get(0))) {
                    content += Constants.DELIMITER + f;
                }
            }

            if (!onlyOne.contains(edge.get(1))) {
                onlyOne.add(edge.get(1));
                content += (content.length() == 0 ? "" : "\n") + edge.get(1);
                for (float f : vertex2ClusterMap.get(edge.get(1))) {
                    content += Constants.DELIMITER + f;
                }
            }
        }


        CommonMethods.saveToFile(content, file);
    }

    private static float adjustClusters(float f, float g) {
        return f + g;
    }

}
