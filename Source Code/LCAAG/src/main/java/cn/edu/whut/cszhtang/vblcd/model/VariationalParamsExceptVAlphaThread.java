package cn.edu.whut.cszhtang.vblcd.model;

import cn.edu.whut.cszhtang.vblcd.Constants;
import org.apache.log4j.Logger;

import java.util.Date;

public class VariationalParamsExceptVAlphaThread implements Runnable {
    private static Logger logger = Logger.getLogger(VariationalParamsExceptVAlphaThread.class);

    // hyperparameters
    float[][] muArray;
    float[] sigmaArray;
    float[][] lambdaArray;

    // variational parameters
    float[] vSigmaArray;
    float[][] vMuArray;
    float[][] vLambdaArray;
    float[][] vAlphaArray;

    // other parameters
    byte[][] attributeMatrixArray;
    byte[][] incidenceMatrixArray;
    int beginIndex, endIndex, numAttributes, numEdges, numClusters, thread;

    public VariationalParamsExceptVAlphaThread(float[][] vAlphaArray, byte[][] attributeMatrixArray,
                                               byte[][] incidenceMatrixArray, float[] vSigmaArray, float[][] vMuArray,
                                               float[][] vLambdaArray, float[] sigmaArray, float[][] muArray,
                                               float[][] lambdaArray, int beginIndex, int endIndex, int numAttributes,
                                               int numEdges, int numClusters, int thread) {
        this.vAlphaArray = vAlphaArray;
        this.attributeMatrixArray = attributeMatrixArray;
        this.incidenceMatrixArray = incidenceMatrixArray;
        this.vSigmaArray = vSigmaArray;
        this.vMuArray = vMuArray;
        this.vLambdaArray = vLambdaArray;
        this.sigmaArray = sigmaArray;
        this.muArray = muArray;
        this.lambdaArray = lambdaArray;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        this.numAttributes = numAttributes;
        this.numEdges = numEdges;
        this.numClusters = numClusters;
        this.thread = thread;
    }

    public void run() {
//        logger.info("Thread #" + this.thread + "[Start variational params except vAlpha {" + this.beginIndex + ","
//                + this.endIndex + "}]: " + new Date().toString());

/*        for (int k = this.beginIndex; k < this.endIndex; k++) {
//            logger.info("Cluster #" + (k + 1) + ":" + new Date().toString());
            this.vSigmaArray[k] = this.sigmaArray[k];

            for (int i = 0; i < this.numEdges; i++) {
                this.vSigmaArray[k] += this.vAlphaArray[i][k];
                updateVMuArray(k, i);
                updateLambdaArray(k, i);
            }
        }*/
//        logger.info("Thread #" + this.thread + "[Finish variational params except vAlpha {" + this.beginIndex + ", "
//                + this.endIndex + "}]: " + new Date().toString());

        for (int i = this.beginIndex; i < this.endIndex; i++) {
            for (int k = 0; k < this.numClusters; k++) {
                this.vSigmaArray[k] = this.sigmaArray[k];
                this.vSigmaArray[k] += this.vAlphaArray[i][k];
                updateVMuArray(k, i);
                updateLambdaArray(k, i);
            }
        }
    }

    private void updateLambdaArray(int k, int i) {
        for (int m = 0; m < this.numAttributes; m++) {
            if (i == 0) {
                this.vLambdaArray[k][m] = this.lambdaArray[k][m];
            }

            this.vLambdaArray[k][m] += this.vAlphaArray[i][k] * this.attributeMatrixArray[m][i];

        }
    }

    private void updateVMuArray(int k, int i) {
        for (int l = 0; l < this.numClusters; l++) {
            for (int j = 0; j < i; j++) {
                if (i == 0) {
                    this.vMuArray[i][j] = this.muArray[i][j];
                }
                if (this.incidenceMatrixArray[i][j] >= 1) {
                    float value = this.vAlphaArray[i][k] * this.vAlphaArray[j][l] * this.incidenceMatrixArray[i][j];
                    this.vMuArray[k][l] += value;
                    this.vMuArray[l][k] += value;
                }

            }
        }
    }
}
