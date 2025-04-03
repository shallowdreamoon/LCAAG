package cn.edu.whut.cszhtang.vblcd.data;

import cn.edu.whut.cszhtang.vblcd.Constants;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class GraphDataReader {
    private static Logger logger = Logger.getLogger(GraphDataReader.class);

    private String graphFile;
    private String[] attributeFiles;

    private byte[][] incidenceMatrixArray;
    private byte[][] attributeMatrixArray;
    private Map<String, Integer> vertex2IndexMap;
    private Map<List<String>, Integer> edge2IndexMap;

    public GraphDataReader(String graphFile, String[] attributeFiles) {

        this.graphFile = graphFile;
        this.attributeFiles = attributeFiles;
    }

    public GraphDataReader(String graphFile) {

        this.graphFile = graphFile;
        this.attributeFiles = null;
    }

    public void run() {
        setIncidenceAndAttributeMatrix(this.graphFile);
    }

    private void setIncidenceAndAttributeMatrix(String graphFile) {
        setIncidenceMatrixArray(graphFile);
        this.attributeMatrixArray = setAttributeMatrixArray(graphFile);
    }

    private byte[][] setAttributeMatrixArray(String graphFile) {
        Map<String, List<String>> vertex2AttributeListMap = new HashMap<String, List<String>>();
        Map<List<String>, Map<String, Byte>> edge2AttributeMapMap
                = new HashMap<List<String>, Map<String, Byte>>();
        BufferedReader br;
        String line;
        try {
            br = new BufferedReader(new FileReader(graphFile));
            while ((line = br.readLine()) != null) {
                String[] item = line.split(Constants.DELIMITER);
                if (Constants.VertexPrefix.equals(item[0])) {
                    if (item.length == 2) {
                        continue;
                    }
                    String[] attribute = item[2].split(Constants.COMMA);
                    List<String> vertex2AttributeList = new ArrayList<String>(Arrays.asList(attribute));
                    vertex2AttributeListMap.put(item[1], vertex2AttributeList);
                }

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (List<String> edge :
                this.edge2IndexMap.keySet()) {
            for (String vertex :
                    vertex2AttributeListMap.keySet()) {
                if (edge.contains(vertex)) {
                    if (!edge2AttributeMapMap.containsKey(edge)) {
                        edge2AttributeMapMap.put(edge, new HashMap<String, Byte>());
                    }
                    for (String attribute :
                            vertex2AttributeListMap.get(vertex)) {
                        if (!edge2AttributeMapMap.get(edge).containsKey(attribute)) {
                            edge2AttributeMapMap.get(edge).put(attribute, (byte) 1);
                        } else {
                            edge2AttributeMapMap.get(edge).put(attribute, (byte) 2);
                        }
                    }
                }
            }
        }

        List<String> attributeList = new ArrayList<String>();
        for (List<String> edge :
                edge2AttributeMapMap.keySet()) {
            for (String attribute :
                    edge2AttributeMapMap.get(edge).keySet()) {
                if (!attributeList.contains(attribute)) {
                    attributeList.add(attribute);
                }
            }
        }
        int numAttributes = attributeList.size();

        byte[][] attributeMatrixArray = new byte[numAttributes][this.edge2IndexMap.size()];
        for (List<String> edge :
                edge2AttributeMapMap.keySet()) {
            int edgeIndex = this.edge2IndexMap.get(edge) - 1;
            Map<String, Byte> attributeMap = edge2AttributeMapMap.get(edge);

            for (String attribute :
                    attributeMap.keySet()) {
                attributeMatrixArray[attributeList.indexOf(attribute)][edgeIndex] = attributeMap.get(attribute);
            }

        }

        return attributeMatrixArray;
    }

//    private void setAttributeMatrixArray(String[] attributeFiles) {
//        int numAttributes = this.attributeFiles.length;
//        this.attributeMatrixArray = new byte[numAttributes][];
//        for (int i = 0; i < numAttributes; i++) {
//            this.attributeMatrixArray[i] = getAttributeFiles(this.attributeFiles[i]);
//        }
//    }

//    private byte[] getAttributeFiles(String attributeFile) {
//        Map<String, String> vertex2AttributeMap = new HashMap<String, String>();
//        updateVertex2AttributeMapFromFile(attributeFile, vertex2AttributeMap);
//        return getAttributeMatrixFromMap(vertex2AttributeMap);
//    }

//    private byte[] getAttributeMatrixFromMap(Map<String, String> vertex2AttributeMap) {
//        byte[] attributes = new byte[this.edge2IndexMap.size()];
//        for (List<String> edge : this.edge2IndexMap.keySet()) {
//            byte value = 0;
//            int edgeIndex = this.edge2IndexMap.get(edge) - 1;
//            for (String vertex : vertex2AttributeMap.keySet()) {
//                if (edge.contains(vertex)) {
//                    value++;
//                }
//            }
//            attributes[edgeIndex] = value;
//        }
//
//        return attributes;
//    }

    private void updateVertex2AttributeMapFromFile(String attributeFile, Map<String, String> vertex2AttributeMap) {
        BufferedReader br;
        String line;
        try {
            br = new BufferedReader(new FileReader(attributeFile));
            while ((line = br.readLine()) != null) {
                String[] item = line.split(Constants.DELIMITER);
                vertex2AttributeMap.put(item[0], item[1]);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void setIncidenceMatrixArray(String graphFile) {
        this.vertex2IndexMap = new HashMap<String, Integer>();
        this.edge2IndexMap = new HashMap<List<String>, Integer>();
        Map<String, List<String>> edgeMap = new HashMap<String, List<String>>();
        readGraphFile(edgeMap, graphFile);

        int numEdges = this.edge2IndexMap.size();
        this.incidenceMatrixArray = new byte[numEdges][numEdges];
        for (List<String> edge1 : this.edge2IndexMap.keySet()) {
            for (List<String> edge2 : this.edge2IndexMap.keySet()) {
                if (edge1.equals(edge2)) {
                    continue;
                }
                int edge1Index = this.edge2IndexMap.get(edge1) - 1;
                int edge2Index = this.edge2IndexMap.get(edge2) - 1;

                byte value = judgeIncidenceType(edgeMap, edge1, edge2);
                this.incidenceMatrixArray[edge1Index][edge2Index] = value;
                this.incidenceMatrixArray[edge2Index][edge1Index] = value;
            }
        }

    }

    private byte judgeIncidenceType(Map<String, List<String>> edgeMap, List<String> edge1, List<String> edge2) {

        int numLinks = 0;
        for (String vertex1 : edge1) {
            for (String vertex2 : edge2) {
                if ((edgeMap.containsKey(vertex1) && edgeMap.get(vertex1).contains(vertex2))
                        || (edgeMap.containsKey(vertex2) && edgeMap.get(vertex2).contains(vertex1))) {
                    numLinks++;
                }
            }
        }

        if (numLinks == 0) {
            return 0;
        }

        byte type = 0;
        String vertex1 = edge1.get(0);
        String vertex2 = edge1.get(1);
        String vertex3 = edge2.get(0);
        String vertex4 = edge2.get(1);
        boolean l13 = edgeMap.containsKey(vertex1) && edgeMap.get(vertex1).contains(vertex3)
                || edgeMap.containsKey(vertex3) && edgeMap.get(vertex3).contains(vertex1);
        boolean l14 = edgeMap.containsKey(vertex1) && edgeMap.get(vertex1).contains(vertex4)
                || edgeMap.containsKey(vertex4) && edgeMap.get(vertex4).contains(vertex1);
        boolean l23 = edgeMap.containsKey(vertex2) && edgeMap.get(vertex2).contains(vertex3)
                || edgeMap.containsKey(vertex3) && edgeMap.get(vertex3).contains(vertex2);
        boolean l24 = edgeMap.containsKey(vertex2) && edgeMap.get(vertex2).contains(vertex4)
                || edgeMap.containsKey(vertex4) && edgeMap.get(vertex4).contains(vertex2);

        switch (numLinks) {
            case 1:
                if (l13 || l24) {
                    type = 1;
                } else if (l14 || l23) {
                    type = 2;
                } else {
                    logger.error("An exception accured in function : JudgeIncidenceType where numLinks = " + numLinks
                            + "the edges = [" + edge1 + "] and [" + edge2 + "]");
                }
                break;
            case 2:
                if (l13 && l24) {
                    type = 3;
                } else if (l14 && l23) {
                    type = 4;
                } else if ((l13 && l14) || (l23 && l24)) {
                    type = 5;
                } else if ((l13 && l23) || (l14 && l24)) {
                    type = 6;
                } else {
                    logger.error("An exception accured in function : JudgeIncidenceType where numLinks = " + numLinks
                            + "the edges = [" + edge1 + "] and [" + edge2 + "]");
                }
                break;
            case 3:
                if (!(l14 && l23)) {
                    type = 7;
                } else if (!(l13 && l24)) {
                    type = 8;
                } else {
                    logger.error("An exception accured in function : JudgeIncidenceType where numLinks = " + numLinks
                            + "the edges = [" + edge1 + "] and [" + edge2 + "]");
                }
                break;
            case 4:
                if (l13 && l14 && l23 && l24)
                    type = 9;
        }

        return type;
    }

    private void readGraphFile(Map<String, List<String>> edgeMap, String graphFile) {
        BufferedReader br;
        String line;
        try {
            br = new BufferedReader(new FileReader(graphFile));
            while ((line = br.readLine()) != null) {
                String[] item = line.split(Constants.DELIMITER);
                if (Constants.VertexPrefix.equals(item[0])) {
                    String vertex = item[1];
                    if (!this.vertex2IndexMap.containsKey(vertex)) {
                        this.vertex2IndexMap.put(vertex, this.vertex2IndexMap.size() + 1);
                    }

                } else if (Constants.EdgePrefix.equals(item[0])) {
                    List<String> edge = new ArrayList<String>();
                    edge.add(item[1]);
                    edge.add(item[2]);
                    if (!this.edge2IndexMap.containsKey(edge)) {
                        this.edge2IndexMap.put(edge, this.edge2IndexMap.size() + 1);
                    }

                    if (!edgeMap.containsKey(item[1])) {
                        edgeMap.put(item[1], new ArrayList<String>());
                    }
                    edgeMap.get(item[1]).add(item[2]);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[][] getAttributeMatrixArray() {
        return this.attributeMatrixArray;
    }

    public byte[][] getIncidenceMatrixArray() {
        return this.incidenceMatrixArray;
    }

    public Map<String, Integer> getVertex2IndexMap() {
        return this.vertex2IndexMap;
    }

    public Map<List<String>, Integer> getEdge2IndexMap() {
        return this.edge2IndexMap;
    }

}
