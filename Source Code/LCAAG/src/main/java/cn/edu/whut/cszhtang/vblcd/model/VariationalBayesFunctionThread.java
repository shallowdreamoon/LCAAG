package cn.edu.whut.cszhtang.vblcd.model;

import cn.edu.whut.cszhtang.vblcd.Constants;
import org.apache.commons.math3.special.Gamma;
import org.apache.log4j.Logger;

import java.util.Date;

public class VariationalBayesFunctionThread implements Runnable {
    private static Logger logger = Logger.getLogger(VariationalBayesFunctionThread.class);

    private static int ExpectationOverSigmaIndex = 0;
    private static int ExpectationOverBetaIndex = 1;
    private static int ExpectationOverThetaIndex = 2;
    private static int ExpectationOverAIndex = 3;
    private static int ExpectationOverXIndex = 4;
    private static int ExpectationOverVAlphaIndex = 5;
    private static int ExpectationOverVBetaIndex = 6;
    private static int ExpectationOverVThetaIndex = 7;
    private static int ExpectationOverVCIndex = 8;
    private static int ExpectationOverCIndex = 9;

    private float[] vSigmaArray, sigmaArray;
    private float[][] muArray;
    private float[][] vMuArray;
    private float[][] vLambdaArray;
    private float[][] lambdaArray;
    private float[][] vAlphaArray;
    private byte[][] attributeMatrixArray;
    private byte[][] incidenceMatrixArray;
    private float[] expectationOverSigmaArray, expectationOverBetaArray, expectationOverThetaArray,
            expectationOverPhiArray, expectationOverAArray, expectationOverXArray, expectationOverVAlphaArray,
            expectationOverVBetaArray, expectationOverVThetaArray, expectationOverVCArray,
            expectationOverCArray;
    private int threadIndex, beginIndex, endIndex, numAttributes, numClusters, numEdges;


    public VariationalBayesFunctionThread(float[][] lambdaArray, float[] sigmaArray, float[][] muArray,
                                          float[][] vAlphaArray, byte[][] attributeMatrixArray,
                                          byte[][] incidenceMatrixArray, float[] vSigmaArray, float[][] vMuArray,
                                          float[][] vLambdaArray, float[] expectationOverSigmaArray,
                                          float[] expectationOverBetaArray, float[] expectationOverThetaArray,
                                          float[] expectationOverAArray, float[] expectationOverXArray,
                                          float[] expectationOverVAlphaArray, float[] expectationOverVBetaArray,
                                          float[] expectationOverVThetaArray, float[] expectationOverVCArray,
                                          float[] expectationOverCArray, int threadIndex, int beginIndex,
                                          int endIndex, int numAttributes, int numClusters, int numEdges) {
        this.lambdaArray = lambdaArray;
        this.sigmaArray = sigmaArray;
        this.muArray = muArray;
        this.vAlphaArray = vAlphaArray;
        this.attributeMatrixArray = attributeMatrixArray;
        this.incidenceMatrixArray = incidenceMatrixArray;
        this.vSigmaArray = vSigmaArray;
        this.vMuArray = vMuArray;
        this.vLambdaArray = vLambdaArray;
        this.expectationOverSigmaArray = expectationOverSigmaArray;
        this.expectationOverBetaArray = expectationOverBetaArray;
        this.expectationOverThetaArray = expectationOverThetaArray;
        this.expectationOverAArray = expectationOverAArray;
        this.expectationOverXArray = expectationOverXArray;
        this.expectationOverVAlphaArray = expectationOverVAlphaArray;
        this.expectationOverVBetaArray = expectationOverVBetaArray;
        this.expectationOverVThetaArray = expectationOverVThetaArray;
        this.expectationOverVCArray = expectationOverVCArray;
        this.expectationOverCArray = expectationOverCArray;
        this.threadIndex = threadIndex;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        this.numAttributes = numAttributes;
        this.numClusters = numClusters;
        this.numEdges = numEdges;
    }

    public void run() {
//        logger.info("Thread #" + this.threadIndex + "[Start calculating variational bayes function]: "
//                + new Date().toString());

        float[] expectations = new float[10];
        updateExpectations(expectations);

        this.expectationOverSigmaArray[this.threadIndex] = expectations[ExpectationOverSigmaIndex];
        this.expectationOverBetaArray[this.threadIndex] = expectations[ExpectationOverBetaIndex];
        this.expectationOverThetaArray[this.threadIndex] = expectations[ExpectationOverThetaIndex];
        this.expectationOverAArray[this.threadIndex] = expectations[ExpectationOverAIndex];
        this.expectationOverXArray[this.threadIndex] = expectations[ExpectationOverXIndex];
        this.expectationOverVAlphaArray[this.threadIndex] = expectations[ExpectationOverVAlphaIndex];
        this.expectationOverVBetaArray[this.threadIndex] = expectations[ExpectationOverVBetaIndex];
        this.expectationOverVThetaArray[this.threadIndex] = expectations[ExpectationOverVThetaIndex];
        this.expectationOverVCArray[this.threadIndex] = expectations[ExpectationOverVCIndex];
        this.expectationOverCArray[this.threadIndex] = expectations[ExpectationOverCIndex];

//        logger.info("Thread #" + this.threadIndex + "[Finish calculating variational bayes function]: "
//                + new Date().toString());
    }

    private void updateExpectations(float[] expectations) {
/*        for (int k = this.beginIndex; k < this.endIndex; k++) {
            float diffOfDigammaFunctionForVSigma = CommonMethods.getDiffOfDigammaFunction(this.vSigmaArray, k);

            // update expectationOverVAlpha and expectationOverSigma
            expectations[ExpectationOverVAlphaIndex] += (this.vSigmaArray[k] - 1) * diffOfDigammaFunctionForVSigma;
            expectations[ExpectationOverSigmaIndex] += (this.sigmaArray[k] - 1) * diffOfDigammaFunctionForVSigma;

            for (int i = 0; i < this.numEdges; i++) {
                // update expectationOverVC
                if (this.vAlphaArray[i][k] != 0) {
                    expectations[ExpectationOverVCIndex] += this.vAlphaArray[i][k] * Math.log(this.vAlphaArray[i][k]);
                }
                updateExpectationsRelatedToAttribute(k, i, expectations);
                updateExpectationsRelatedToTopology(k, i, expectations);
            }
        }
        expectations[ExpectationOverVAlphaIndex] += getLogFunctionZ(this.vSigmaArray);*/

        for (int i = beginIndex; i < endIndex; i++) {
            for (int k = 0; k < this.numClusters; k++) {
                float diffOfDigammaFunctionForVSigma = CommonMethods.getDiffOfDigammaFunction(this.vSigmaArray, k);
                // update expectationOverVAlpha and expectationOverSigma
                expectations[ExpectationOverVAlphaIndex] += (this.vSigmaArray[k] - 1) * diffOfDigammaFunctionForVSigma;
                expectations[ExpectationOverSigmaIndex] += (this.sigmaArray[k] - 1) * diffOfDigammaFunctionForVSigma;
                if (this.vAlphaArray[i][k] != 0) {
                    expectations[ExpectationOverVCIndex] += this.vAlphaArray[i][k] * Math.log(this.vAlphaArray[i][k]);
                }
                updateExpectationsRelatedToAttribute(k, i, expectations);
                updateExpectationsRelatedToTopology(k, i, expectations);
            }
        }
        expectations[ExpectationOverVAlphaIndex] += getLogFunctionZ(this.vSigmaArray);
    }

    private void updateExpectationsRelatedToTopology(int k, int i, float[] expectations) {
        for (int l = 0; l < this.numClusters; l++) {
            // update expectationOverBeta and expectationOverVBeta
            float diffOfDigammaFunctionForVMu = CommonMethods.getDiffOfDigammaFunction(this.vMuArray[k], l);

            if (i == 0) {
                expectations[ExpectationOverBetaIndex] += (this.muArray[k][l] - 1)
                        * diffOfDigammaFunctionForVMu;
                expectations[ExpectationOverVBetaIndex] += (this.vMuArray[k][l] - 1)
                        * diffOfDigammaFunctionForVMu;
            }

            // update expectationOverA
            for (int j = 0; j < i; j++) {
                if (this.incidenceMatrixArray[i][j] >= 1) {
                    expectations[ExpectationOverAIndex] += this.vAlphaArray[i][k] * this.vAlphaArray[j][l]
                            * diffOfDigammaFunctionForVMu * this.incidenceMatrixArray[i][j];
                }
            }

            expectations[ExpectationOverBetaIndex] += getLogFunctionZ(this.muArray[k]);
            expectations[ExpectationOverVBetaIndex] += getLogFunctionZ(this.vMuArray[k]);
        }


    }

    private void updateExpectationsRelatedToAttribute(int k, int i, float[] expectations) {
        float attributePartOfExpectationOverX = 0;
        for (int m = 0; m < this.numAttributes; m++) {
            float diffOfDigammaFunctionForVLambda =
                    CommonMethods.getDiffOfDigammaFunction(this.vLambdaArray[k], m);

            // update C
            if (k == 0) {
                expectations[ExpectationOverCIndex] += this.vAlphaArray[i][k]
                        * CommonMethods.getDiffOfDigammaFunction(this.vSigmaArray, k);
            }

            //update theta and vTheta
            if (i == 0) {
                // update expectation over theta
                expectations[ExpectationOverThetaIndex] += (this.lambdaArray[k][m] - 1)
                        * diffOfDigammaFunctionForVLambda;

                // update expectation over vTheta
                if (m == 0) {
                    expectations[ExpectationOverVThetaIndex] += getLogFunctionZ(this.vLambdaArray[k]);
                }
                expectations[ExpectationOverVThetaIndex] += (this.vLambdaArray[k][m] - 1)
                        * diffOfDigammaFunctionForVLambda;
            }

            // update attribute part of ExpectationOverX
            expectations[ExpectationOverXIndex] += this.vAlphaArray[i][k] * diffOfDigammaFunctionForVLambda
                    * this.attributeMatrixArray[m][i];

        }
    }

    private float getLogFunctionZ(float[] array) {
        float sum = 0;
        double denominatorInLog = 1;
        for (float f :
                array) {
            sum += f;

            if (f != 0) {
                denominatorInLog += Gamma.logGamma(f);
            }
        }
        double nominatorInLog = Gamma.logGamma(sum);
        return (float) (nominatorInLog - denominatorInLog);
    }
}
