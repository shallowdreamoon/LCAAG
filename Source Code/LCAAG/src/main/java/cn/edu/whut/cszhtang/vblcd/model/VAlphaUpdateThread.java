package cn.edu.whut.cszhtang.vblcd.model;

import cn.edu.whut.cszhtang.vblcd.Constants;
import org.apache.log4j.Logger;

import java.util.Date;

public class VAlphaUpdateThread implements Runnable {
    private static Logger logger = Logger.getLogger(VAlphaUpdateThread.class);

    private static float MinFloatValue = (float) Math.log(Float.MIN_VALUE);

    float[] vSigmaArray;
    float[][] vMuArray;
    float[][] vLambdaArray;
    float[][] vAlphaArray;
    float[][] oldAlphaArray;
    byte[][] attributeMatrixArray;
    byte[][] incidenceMatrixArray;
    int beginIndex, endIndex, numAttributes, numEdges, numClusters, thread;

    public VAlphaUpdateThread(float[][] vAlphaArray, float[][] oldVAlphaArray, byte[][] attributeMatrixArray,
                              byte[][] incidenceMatrixArray, float[] vSigmaArray, float[][] vMuArray,
                              float[][] vLambdaArray, int beginIndex, int endIndex, int numAttributes,
                              int numEdges, int numClusters, int thread) {
        this.vAlphaArray = vAlphaArray;
        this.oldAlphaArray = oldVAlphaArray;
        this.attributeMatrixArray = attributeMatrixArray;
        this.incidenceMatrixArray = incidenceMatrixArray;
        this.vSigmaArray = vSigmaArray;
        this.vMuArray = vMuArray;
        this.vLambdaArray = vLambdaArray;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        this.numAttributes = numAttributes;
        this.numEdges = numEdges;
        this.numClusters = numClusters;
        this.thread = thread;
    }

    public void run() {
//        logger.info("Thread #" + this.thread + "[Start updating vAlpha Array {" + this.beginIndex + ", "
//                + this.endIndex + "}]: " + new Date().toString());
        for (int i = this.beginIndex; i < this.endIndex; i++) {
//            logger.info("Edge #" + (i + 1) + ":" + new Date().toString());
            for (int k = 0; k < this.vAlphaArray[i].length; k++) {
                this.vAlphaArray[i][k] = updateVAlphaWithoutExp(i, k, oldAlphaArray);
            }
            subtractMax(this.vAlphaArray[i]);

            for (int k = 0; k < this.vAlphaArray[i].length; k++) {
                if (this.vAlphaArray[i][k] < MinFloatValue) {
                    this.vAlphaArray[i][k] = 0;
                } else {
                    this.vAlphaArray[i][k] = (float) Math.exp(this.vAlphaArray[i][k]);
                }
            }
            CommonMethods.normalizeArrayAtRow(this.vAlphaArray, i, VariationalBayesClustering.MinVAlphaValue);
        }
//        logger.info("Thread #" + this.thread + "[Finish updating vAlpha Array {" + this.beginIndex + ","
//                + this.endIndex + "}]:" + new Date().toString());
    }

    private void subtractMax(float[] inputArray) {
        float max = 0;
        for (int i = 0; i < inputArray.length; i++) {
            max = (i == 0 ? inputArray[i] : Math.max(max, inputArray[i]));
        }
        for (int i = 0; i < inputArray.length; i++) {
            inputArray[i] = inputArray[i] - max;
        }
    }

    private float updateVAlphaWithoutExp(int i, int k, float[][] oldAlphaArray) {
//        logger.info("Sigma part:" + new Date().toString());
        float sigmaPart = getSigmaPartToUpdateAlpha(k);

//        logger.info("Mu part:" + new Date().toString());
        float muPart = getMuPartToUpdateAlpha(i, k, oldAlphaArray);

//        logger.info("Lambda part:" + new Date().toString());
        float lambdaPart = getLambdaToUpdateAlpha(i, k);

//        logger.info("Done: " + new Date().toString());

        return sigmaPart + muPart + lambdaPart;
    }

    private float getLambdaToUpdateAlpha(int i, int k) {
        float lambdaPart = 0;
        for (int m = 0; m < this.numAttributes; m++) {
            lambdaPart += CommonMethods.getDiffOfDigammaFunction(this.vLambdaArray[k], m)
                    * this.attributeMatrixArray[m][i];
        }

//        logger.info("Update vAlpha_" + i + "_" + k + ": lambdaPart " + lambdaPart);
        return lambdaPart;
    }

    private float getMuPartToUpdateAlpha(int i, int k, float[][] oldAlphaArray) {
        float muPart = 0;
        for (int j = 0; j < numEdges; j++) {
            if (j == i) {
                continue;
            }
            for (int l = 0; l < this.numClusters; l++) {
                if (this.incidenceMatrixArray[i][j] >= 1) {
                    muPart += oldAlphaArray[j][l] * CommonMethods.getDiffOfDigammaFunction(this.vMuArray[k], l)
                            * this.incidenceMatrixArray[i][j];
                }
            }
        }
//        logger.info("Update vAlpha_" + i + "_" + k + ": muPart = " + muPart);
        return muPart;
    }

    private float getSigmaPartToUpdateAlpha(int k) {
        return CommonMethods.getDiffOfDigammaFunction(this.vSigmaArray, k);
    }
}
