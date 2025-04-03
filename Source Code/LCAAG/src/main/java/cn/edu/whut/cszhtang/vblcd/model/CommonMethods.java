package cn.edu.whut.cszhtang.vblcd.model;

import org.apache.commons.math3.special.Gamma;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

public class CommonMethods {

    public static void normalizeArrayAtRow(float[][] array, int rowNum, float minValue) {
        float sum = 0;
        for (float f : array[rowNum]) {
            sum += f;
        }
        if (sum == 0) {
            return;
        }
        for (int k = 0; k < array[rowNum].length; k++) {
            array[rowNum][k] = array[rowNum][k] / sum;
            if (array[rowNum][k] < minValue) {
                array[rowNum][k] = 0;
            }
        }
    }

    public static float getDiffOfDigammaFunction(float[] inputArray, int index) {
        if (inputArray[index] == 0) {
            return 0;
        }
        float sum = 0;
        for (float f : inputArray) {
            sum += f;
        }

        return (float) (Gamma.digamma(inputArray[index]) - Gamma.digamma(sum));
    }

    public static void saveToFile(String content, String file) {
        BufferedWriter bw = null;

        try {
            bw = new BufferedWriter(new FileWriter(file));
            bw.write(content);
            bw.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bw != null)
                try {
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }
}
