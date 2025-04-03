package cn.edu.whut.cszhtang.vblcd.model;

import cn.edu.whut.cszhtang.vblcd.Constants;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class VariationalBayesClustering {
    private static Logger logger = Logger.getLogger(VariationalBayesClustering.class);

    public static float MinVAlphaValue = 1e-10f;

    //private static int MaxVAlphaLoops = 10; //原始
    private static int MaxVAlphaLoops = 1;
    //private static float MaxVAlphaDiff = 1e-2f;  //原始
    private static float MaxVAlphaDiff = 5e-2f;

    // input parameters
    private int numClusters, numThreads, maxLoops, numEdges, numAttributes;
    private float maxDiff;
    private byte[][] incidenceMatrixArray;
    private byte[][] attributeMatrixArray;

    private float sigma, mu, lambda;

    // hyper parameters
    private float[] sigmaArray;
    private float[][] muArray;
    private float[][] lambdaArray;

    // variational parameters
    private float[] vSigmaArray;
    private float[][] vMuArray;
    private float[][] vLambdaArray;
    private float[][] vAlphaArray;
    // other parameters
    private float[] sigmaDiffOfDigammaFunctionArray;

    public VariationalBayesClustering(int numClusters, int numThreads, int maxLoops, float maxDiff,
                                      byte[][] incidenceMatrixArray, byte[][] attributeMatrixArray, float sigma, float mu, float lambda) {
        this.numClusters = numClusters;
        this.numThreads = numThreads;
        this.maxLoops = maxLoops;
        this.numEdges = incidenceMatrixArray.length;
        this.numAttributes = attributeMatrixArray.length;
        this.maxDiff = maxDiff;
        this.incidenceMatrixArray = incidenceMatrixArray;
        this.attributeMatrixArray = attributeMatrixArray;
        this.sigma = sigma;
        this.mu = mu;
        this.lambda = lambda;

    }

    public void run() {
        int currLoop = 1;
        float lastResult = 0, diff = 0;

        //初始化参数
        initializeParameters();
//        int[][] threadSlotsForClusters = getThreadSlots(this.numClusters, this.numThreads);
        //线程设置
        int[][] threadSlotsForEdges = getThreadSlots(this.numEdges, this.numThreads);

        do {
            logger.info("Start Loop #" + currLoop + ": " + new Date().toString());
//            updateVariationalParamsExceptVAlpha(threadSlotsForClusters);

            long t1 = System.currentTimeMillis();

            //变分参数的更新（多线程设置、以及具体的更新方式）
            updateVariationalParamsExceptVAlpha(threadSlotsForEdges);

            long t2 = System.currentTimeMillis();
            logger.info("runtime(updateVariationalParamsExceptVAlpha): " + (t2 - t1));

            //alpha更新、以及终止条件判断【貌似此处耗时最多】
            updateVAlphaArray(threadSlotsForEdges);

            long t3 = System.currentTimeMillis();
            logger.info("runtime(updateVAlphaArray): " + (t3 - t2));

//            float currResult = getResultOfVariationalBayesFunction(threadSlotsForClusters);

            //获取聚类结果
            float currResult = getResultOfVariationalBayesFunction(threadSlotsForEdges);
            System.out.println(currResult);

            long t4 = System.currentTimeMillis();
            logger.info("runtime(getResultOfVariationalBayesFunction): " + (t4 - t3));

            //以下这段对应论文中的终止条件
            if (lastResult == 0) {
                diff = 1;
            } else {
                diff = Math.abs(1 - currResult / lastResult);
            }
            lastResult = currResult;
            System.out.println("Loop: " + currLoop + ", Diff: " + diff);
             logger.info("Finish Loop #" + currLoop + ": " + diff + "[" + currResult + "]" + new Date().toString());
        } while (currLoop++ < this.maxLoops && Math.abs(diff) > this.maxDiff);
    }

    //主要用于计算变分贝叶斯函数的结果。该方法利用多线程来并行处理计算，并最终汇总结果。
    private float getResultOfVariationalBayesFunction(int[][] threadSlots) {
        // multithreading
        float[] expectationOverSigmaArray = new float[this.numThreads];
        float[] expectationOverBetaArray = new float[this.numThreads];
        float[] expectationOverThetaArray = new float[this.numThreads];
        float[] expectationOverAArray = new float[this.numThreads];
        float[] expectationOverXArray = new float[this.numThreads];
        float[] expectationOverVAlphaArray = new float[this.numThreads];
        float[] expectationOverVBetaArray = new float[this.numThreads];
        float[] expectationOverVThetaArray = new float[this.numThreads];
        float[] expectationOverVCArray = new float[this.numThreads];
        float[] expectationOverCArray = new float[this.numThreads];

        // setup thread pool
        ExecutorService executor = Executors.newFixedThreadPool(this.numThreads);
        for (int i = 0; i < threadSlots.length; i++) {
            int beginIndex = threadSlots[i][0];
            int endIndex = threadSlots[i][1];

            //具体的实现逻辑
            Runnable aThread = new VariationalBayesFunctionThread(this.lambdaArray, this.sigmaArray, this.muArray,
                    this.vAlphaArray, this.attributeMatrixArray, this.incidenceMatrixArray, this.vSigmaArray, this.vMuArray,
                    this.vLambdaArray, expectationOverSigmaArray, expectationOverBetaArray, expectationOverThetaArray,
                    expectationOverAArray, expectationOverXArray, expectationOverVAlphaArray,
                    expectationOverVBetaArray, expectationOverVThetaArray,
                    expectationOverVCArray, expectationOverCArray, i, beginIndex, endIndex,
                    this.numAttributes, this.numClusters, this.numEdges);
            executor.execute(aThread);
        }

        // wait till the executor finishes all threads
        executor.shutdown();
        while (!executor.isTerminated()) {
            try {
                executor.awaitTermination(10, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // calculate final result
        float result = 0;
        for (int i = 0; i < this.numThreads; i++) {
            result += expectationOverCArray[i] + expectationOverSigmaArray[i] + expectationOverBetaArray[i]
                    + expectationOverThetaArray[i] + expectationOverAArray[i]
                    + expectationOverXArray[i] - expectationOverVAlphaArray[i] - expectationOverVBetaArray[i]
                    - expectationOverVThetaArray[i] - expectationOverVCArray[i];
        }
        return result;
    }

    //旨在更新 vAlphaArray 参数。它使用多线程来并行执行更新，并包含一个循环来监测收敛情况。
    private void updateVAlphaArray(int[][] threadSlots) {
        float oldDiff = 0, currDiff = 0;
        int currLoop = 0;

        float[][] oldVAlphaArray = new float[this.vAlphaArray.length][];
        do {
            copyTwoDimensionArray(oldVAlphaArray, this.vAlphaArray);
            oldDiff = currDiff;

            // setup thread pool
            ExecutorService executor = Executors.newFixedThreadPool(this.numThreads);
            for (int i = 0; i < threadSlots.length; i++) {
                int beginIndex = threadSlots[i][0];
                int endIndex = threadSlots[i][1];

                //alpha参数更新的具体方式
                Runnable aThread = new VAlphaUpdateThread(this.vAlphaArray, oldVAlphaArray, this.attributeMatrixArray,
                        this.incidenceMatrixArray, this.vSigmaArray, this.vMuArray, this.vLambdaArray, beginIndex, endIndex,
                        this.numAttributes, this.numEdges, this.numClusters, i);
                executor.execute(aThread);
            }

            // wait till the executor finishes all threads
            executor.shutdown();
            while (!executor.isTerminated()) {
                try {
                    executor.awaitTermination(10, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // perform the comparison
            //阈值比较，为何是alpha呢？====这里对应论文中哪一块呢？？？
            currDiff = compareTwoDimensionalArray(this.vAlphaArray, oldVAlphaArray);
//            logger.info("Internal Loop #" + currLoop + ":\t" + currDiff);

            // special termination conditions
            if (currLoop > 1 && currDiff > oldDiff) {
                this.vAlphaArray = oldVAlphaArray;
                break;
            } else if (currDiff == 0) {
                break;
            }
            currLoop++;
            System.out.println("curLoop: " + currLoop + ", curDiff: " + currDiff);
        } while (currLoop == 1 || (currDiff > MaxVAlphaDiff && currLoop < MaxVAlphaLoops));
    }

    private float compareTwoDimensionalArray(float[][] array1, float[][] array2) {
        float maxDiff = 0;
        for (int i = 0; i < array1.length; i++) {
            for (int j = 0; j < array1[i].length; j++) {
                maxDiff = Math.abs(Math.max(maxDiff, array1[i][j] - array2[i][j]));
            }
        }
        return maxDiff;
    }

    private void copyTwoDimensionArray(float[][] array, float[][] arrayToCopy) {
        for (int i = 0; i < arrayToCopy.length; i++) {
            if (array[i] == null) {
                array[i] = new float[arrayToCopy[i].length];

                System.arraycopy(arrayToCopy[i], 0, array[i], 0, arrayToCopy[i].length);
            }
        }
    }

    //主要用于更新变分参数（variational parameters），但不包括 vAlpha 参数。它使用线程池来并行处理任务。
    private void updateVariationalParamsExceptVAlpha(int[][] threadSlots) {
        // setup thread pool
        ExecutorService executor = Executors.newFixedThreadPool(this.numThreads);
        for (int i = 0; i < threadSlots.length; i++) {
            int beginIndex = threadSlots[i][0];
            int endIndex = threadSlots[i][1];

            //变分参数更新的具体方式
            Runnable aThread = new VariationalParamsExceptVAlphaThread(this.vAlphaArray, this.attributeMatrixArray,
                    this.incidenceMatrixArray, this.vSigmaArray, this.vMuArray, this.vLambdaArray, this.sigmaArray,
                    this.muArray, this.lambdaArray, beginIndex, endIndex, this.numAttributes, this.numEdges,
                    this.numClusters, i);
            executor.execute(aThread);
        }

        // wait till executor finishes all threads
        executor.shutdown();
        while (!executor.isTerminated()) {
            try {
                executor.awaitTermination(10, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //旨在根据给定的实例数量和线程数量，生成一个二维数组，表示每个线程处理的实例范围。
    private int[][] getThreadSlots(int numInstances, int numThreads) {
        int[][] threadSlots;
        if (numInstances <= numThreads) {
            threadSlots = new int[numInstances][2];
            for (int i = 0; i < numInstances; i++) {
                threadSlots[i][0] = i;
                threadSlots[i][1] = i + 1;
            }
        } else {
            threadSlots = new int[numThreads][2];
            int numItemsInEachThread = Math.round(numInstances / numThreads);
            int remainder = numInstances - numItemsInEachThread * numThreads;

            // set thread pool
            for (int i = 0; i < numThreads; i++) {
                int extra = Math.min(i, remainder);
                int beginIndex = numItemsInEachThread * i + extra;
                int increasement = numItemsInEachThread + (i < remainder ? 1 : 0);
                int endIndex = (i == numThreads - 1 ? numInstances : (beginIndex + increasement));
                threadSlots[i][0] = beginIndex;
                threadSlots[i][1] = endIndex;
            }
        }
        return threadSlots;
    }

    //该方法的主要功能是初始化多个与聚类相关的参数数组，所有参数初始值均设置为 1，这通常用于建立非信息性先验，
    // 以便后续的学习或推断过程。通过这种方式，模型在开始时不会对参数施加太多假设，从而允许数据主导学习过程。
    private void initializeParameters() {
        // set all values of hyperparameters to 1 for the noninformative priors
        this.sigmaArray = new float[this.numClusters];
        this.vSigmaArray = new float[this.numClusters];
        this.sigmaDiffOfDigammaFunctionArray = new float[this.numClusters];
        Arrays.fill(this.sigmaArray, this.sigma);
        Arrays.fill(this.vSigmaArray, 1);

        this.muArray = new float[this.numClusters][this.numClusters];
        this.vMuArray = new float[this.numClusters][this.numClusters];
        setAllValuesAs1ForTwoDimArray(this.muArray, this.mu);
        setAllValuesAs1ForTwoDimArray(this.vMuArray, 1);

        this.lambdaArray = new float[this.numClusters][this.numAttributes];
        this.vLambdaArray = new float[this.numClusters][this.numAttributes];
        setAllValuesAs1ForTwoDimArray(this.lambdaArray, this.lambda);
        setAllValuesAs1ForTwoDimArray(this.vLambdaArray, 1);
    }

    private void setAllValuesAs1ForTwoDimArray(float[][] array, float value) {
        for (float[] fs : array) {
            Arrays.fill(fs, value);
        }
    }

    private void setAllValuesAs1ForThreeDimArray(float[][][] threeDimArray) {
        for (float[][] fs : threeDimArray) {
            for (float[] f : fs) {
                Arrays.fill(f, 1);
            }
        }
    }

    //论文中的alpha，初始化
    public void setInitVAlphaArray(float[][] inputArray) {
        if (inputArray == null) {
            this.vAlphaArray = new float[this.numEdges][this.numClusters];
            setRandomValuesForVAlphaArray();
        } else {
            this.vAlphaArray = inputArray;
        }
        for (int i = 0; i < this.vAlphaArray.length; i++) {
            CommonMethods.normalizeArrayAtRow(this.vAlphaArray, i, MinVAlphaValue);
        }
    }

    private void setRandomValuesForVAlphaArray() {
        for (int i = 0; i < this.vAlphaArray.length; i++) {
            for (int j = 0; j < this.vAlphaArray[i].length; j++) {
                this.vAlphaArray[i][j] = (float) Math.random();
            }
        }
    }

    public float[][] getClusteringResult() {
        return this.vAlphaArray;
    }
}
