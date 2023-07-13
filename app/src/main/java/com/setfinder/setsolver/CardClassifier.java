package com.setfinder.setsolver;


import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.setfinder.setsolver.ml.OptimizedModel;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CardClassifier {
    private final static String[] classes = {"1BEC", "1BED", "1BES", "1BFC", "1BFD", "1BFS", "1BSC", "1BSD", "1BSS", "1GEC", "1GED", "1GES", "1GFC", "1GFD", "1GFS", "1GSC", "1GSD", "1GSS", "1REC", "1RED", "1RES", "1RFC", "1RFD", "1RFS", "1RSC", "1RSD", "1RSS", "2BEC", "2BED", "2BES", "2BFC", "2BFD", "2BFS", "2BSC", "2BSD", "2BSS", "2GEC", "2GED", "2GES", "2GFC", "2GFD", "2GFS", "2GSC", "2GSD", "2GSS", "2REC", "2RED", "2RES", "2RFC", "2RFD", "2RFS", "2RSC", "2RSD", "2RSS", "3BEC", "3BED", "3BES", "3BFC", "3BFD", "3BFS", "3BSC", "3BSD", "3BSS", "3GEC", "3GED", "3GES", "3GFC", "3GFD", "3GFS", "3GSC", "3GSD", "3GSS", "3REC", "3RED", "3RES", "3RFC", "3RFD", "3RFS", "3RSC", "3RSD", "3RSS"};
    public static String getPrediction(Context context, Mat inputMat) {

        final int imgHeight = 150;
        final int imgWidth = 100;
        final int denominator = 255; // Can not be 0.


        Bitmap image = Bitmap.createBitmap(inputMat.cols(), inputMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(inputMat, image);
        String result = null;

        try {
            OptimizedModel model = OptimizedModel.newInstance(context);

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, imgHeight, imgWidth, 3}, DataType.FLOAT32);

            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imgHeight * imgWidth * 3);
            byteBuffer.order(ByteOrder.nativeOrder());


            // Load pixel values into an integer array.
            int[] intValues = new int[imgHeight * imgWidth];
            image.getPixels(intValues, 0, image.getWidth(), 0,0,image.getWidth(), image.getHeight());
            int pixel = 0;

            // Iterate over each pixel and put them into the byteBuffer
            for (int row = 0; row < inputMat.rows(); row++) {
                for (int col = 0; col < inputMat.cols(); col++) {
                    int val = intValues[pixel++]; // Will be in RGB. They need to be separated.
                    // Some bit-manipulation to get the RGB values separated into the byteBuffer.
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / denominator));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / denominator));
                    byteBuffer.putFloat((val >> 0xFF)  * (1.f / denominator));
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            OptimizedModel.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();


            // find the index of the class with the highest confidence
            float[] confidences = outputFeature0.getFloatArray();
            result = getClass(confidences);

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            Log.e("openCV", "IOException occurred with tensorflow model");
        }
        return result;
    }


    private static String getClass(float[] confidences) {
        int maxPos = 0;
        float maxConfidence = 0;
        for (int i = 0; i < confidences.length; i++) {
            if (confidences[i] > maxConfidence) {
                maxConfidence = confidences[i];
                maxPos = i;
            }
        }

        return classes[maxPos];
    }
}
