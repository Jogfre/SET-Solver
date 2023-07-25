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

    private final static String[] classes = {"1GED", "1GEC", "1GES", "1GFD", "1GFC", "1GFS", "1GSD", "1GSC", "1GSS", "1BED", "1BEC", "1BES", "1BFD", "1BFC", "1BFS", "1BSD", "1BSC", "1BSS", "1RED", "1REC", "1RES", "1RFD", "1RFC", "1RFS", "1RSD", "1RSC", "1RSS", "2GED", "2GEC", "2GES", "2GFD", "2GFC", "2GFS", "2GSD", "2GSC", "2GSS", "2BED", "2BEC", "2BES", "2BFD", "2BFC", "2BFS", "2BSD", "2BSC", "2BSS", "2RED", "2REC", "2RES", "2RFD", "2RFC", "2RFS", "2RSD", "2RSC", "2RSS", "3GED", "3GEC", "3GES", "3GFD", "3GFC", "3GFS", "3GSD", "3GSC", "3GSS", "3BED", "3BEC", "3BES", "3BFD", "3BFC", "3BFS", "3BSD", "3BSC", "3BSS", "3RED", "3REC", "3RES", "3RFD", "3RFC", "3RFS", "3RSD", "3RSC", "3RSS"};
    public static String getPrediction(Context context, Mat inputMat) {

        final int imgHeight = 150;
        final int imgWidth = 100;


        Bitmap image = Bitmap.createBitmap(inputMat.cols(), inputMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(inputMat, image);
        String result = null;

        try {
            OptimizedModel model = OptimizedModel.newInstance(context);

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, imgHeight, imgWidth, 3}, DataType.FLOAT32);

            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imgHeight * imgWidth * 3);
            // 4 because each float is 32 bits (4 bytes), height * width for each pixel and 3 for each RGB channel.

            byteBuffer.order(ByteOrder.nativeOrder()); // Dunno why. Documentation said to do it.


            // Load pixel values into an integer array.
            int[] intValues = new int[imgHeight * imgWidth];
            image.getPixels(intValues, 0, image.getWidth(), 0,0,image.getWidth(), image.getHeight());
            int pixel = 0;

            // Iterate over each pixel and put them into the byteBuffer
            for (int row = 0; row < inputMat.rows(); row++) {
                for (int col = 0; col < inputMat.cols(); col++) {
                    int val = intValues[pixel++]; // Will be in BGRA. They need to be separated.

                    /*
                    Some bit-manipulation to get the RGB values separated in order into the byteBuffer.

                    u_int32 BGRA format (read backwards)
                    00000000 10000000 20000000 30000000
                    Alpha    Red      Green    Blue

                    Example for reading the Red value:
                    - Right shift by 16, so the `10000000` is all the way to the right.
                    - Then `bitwise-and` with `0xff` so that any bits that aren't the most right 8 bits are 0.
                    - Then subtract 127.5 and then divide it by 127.5. (To make it a value between -1 and 1).

                     */
                    byteBuffer.putFloat((((val >> 16) & 0xFF) - 127.5f) / 127.5f);  // Red Value
                    byteBuffer.putFloat((((val >> 8) & 0xFF) - 127.5f) / 127.5f);   // Green Value
                    byteBuffer.putFloat(((val & 0xFF) - 127.5f) / 127.5f);          // Blue Value
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            OptimizedModel.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();


            // find the index of the class with the highest confidence
            float[] confidences = outputFeature0.getFloatArray();
            result = getPredictedClass(confidences);

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            Log.e("openCV", "IOException occurred with tensorflow model");
        }
        return result;
    }


    private static String getPredictedClass(float[] confidences) {
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
