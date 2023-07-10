package com.setfinder.setsolver;


import androidx.annotation.NonNull;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.Manifest;
import android.view.View;
import android.widget.ImageView;

import com.setfinder.setsolver.ml.OptimizedModel;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class MainActivity extends CameraActivity {
    // Variable declarations
    private CameraBridgeViewBase mOpenCvCameraView;
    private ImageView staticCameraView;
    private boolean cameraState = true;
    private int frameIdx = 0;

    private final String[] classes = {"1BEC", "1BED", "1BES", "1BFC", "1BFD", "1BFS", "1BSC", "1BSD", "1BSS", "1GEC", "1GED", "1GES", "1GFC", "1GFD", "1GFS", "1GSC", "1GSD", "1GSS", "1REC", "1RED", "1RES", "1RFC", "1RFD", "1RFS", "1RSC", "1RSD", "1RSS", "2BEC", "2BED", "2BES", "2BFC", "2BFD", "2BFS", "2BSC", "2BSD", "2BSS", "2GEC", "2GED", "2GES", "2GFC", "2GFD", "2GFS", "2GSC", "2GSD", "2GSS", "2REC", "2RED", "2RES", "2RFC", "2RFD", "2RFS", "2RSC", "2RSD", "2RSS", "3BEC", "3BED", "3BES", "3BFC", "3BFD", "3BFS", "3BSC", "3BSD", "3BSS", "3GEC", "3GED", "3GES", "3GFC", "3GFD", "3GFS", "3GSC", "3GSD", "3GSS", "3REC", "3RED", "3RES", "3RFC", "3RFD", "3RFS", "3RSC", "3RSD", "3RSS"};

    Mat lastFrame;
    Mat frameClone;
    ArrayList<Mat> frames;
    ArrayList<Mat> setFrames;
    List<MatOfPoint> contourClone;
    ImageView iconButton;
    ImageView forwardButton;
    ImageView backButton;


    // Main function for the Activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the required permissions
        getPermission();


        iconButton = findViewById(R.id.centerButton);
        iconButton.setOnClickListener(v -> mainButton());

        forwardButton = findViewById(R.id.forwardButton);
        forwardButton.setOnClickListener(v -> cycleForward());

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> cycleBack());


        // Attach the cameraView to the corresponding JavaCameraView
        staticCameraView = findViewById(R.id.staticCameraView);
        frames = new ArrayList<>();
        setFrames = new ArrayList<>();
        mOpenCvCameraView = findViewById(R.id.cameraView);

        // Create the CameraView Listener that will activate the camera and capture frames to be used
        mOpenCvCameraView.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {
                Log.i("openCV", "CameraView Started!");
            }

            @Override
            public void onCameraViewStopped() {
                Log.i("openCV", "CameraView Stopped!");
            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                Mat frame = inputFrame.rgba();
                List<MatOfPoint> contours = CardFinder.findAllContours(frame);
                //TODO: Implement Isolated Card detection


                if (contours.size() > 0) {
                    frameClone = frame.clone();
                    contourClone = contours;
                    CardFinder.drawContours(frame, contours);
                } else {
                    frameClone = null;
                    contourClone = null;
                }
                lastFrame = frame.clone();
                return frame;
            }
        });

        // Check that OpenCV has loaded
        if (OpenCVLoader.initDebug()) {
            turnOnCamera();
        } else {
            Log.e("openCV", "Failed to load openCV library");
        }
    }
    private void mainButton() {
        if (cameraState) {
            turnOffCamera();
            getCardValues();
            drawFrames();
            matToImage(frames.get(0), staticCameraView);

        } else {
            turnOnCamera();
        }
    }
    private void cycleForward() {
        if (frameIdx + 1 < frames.size()) {
            frameIdx++;
            matToImage(frames.get(frameIdx), staticCameraView);
        }
    }
    private void cycleBack() {
        if (frameIdx - 1 >= 0) {
            frameIdx--;
            matToImage(frames.get(frameIdx), staticCameraView);
        }
    }
    private void getCardValues() {
        // TODO: Get isolated cards for each contour, then put that in a <Card class> and store all cards in a List/Map
        if (contourClone == null || contourClone.size() < 1) {
            return;
        }

        ArrayList<Mat> isolatedCards = isolateCards(100, 150);
        ArrayList<Card> cards = new ArrayList<>();

        for (int i = 0; i < isolatedCards.size(); i++) {
            Mat isolatedFrame = isolatedCards.get(i);
            MatOfPoint contour = contourClone.get(i);
            Card card = new Card(frameClone, isolatedFrame, contour);
            cards.add(card);
        }

        // TODO: Send all cards to classifier model
        HashMap<String, Card> setMap = new HashMap<>();
        for (Card card : cards) {
            String code = getPrediction(card.getIsolatedCard());
            if (code != null) {
                card.setCode(code);
                setMap.put(card.getNumCode(), card);
                Log.d("openCV", card.generateString());
            }
        }



        // TODO: Calculate SETs from all the classified cards and store any SETs
        Set<Card[]> SET = SETCalculator.calculateSET(setMap);
        Log.d("openCV", "" + SET.size());
        setFrames.clear();
        if (SET.size() > 0) {
            for (Card[] set : SET){
                Mat tmpFrame = frameClone.clone();
                List<MatOfPoint> contours = new ArrayList<>();
                for (Card setCard : set) {
                    contours.add(setCard.getContour());
                }
                CardFinder.drawContours(tmpFrame, contours);
                setFrames.add(tmpFrame);
            }
        }

        // TODO: Render frames on the screen with 3 cards making up a SET highlighted.
    }
    private void putText(String text, Point location) {
        Imgproc.putText(
                frameClone,
                text,
                location,
                2,
                1,
                new Scalar(0, 0, 0),
                2
        );
        Imgproc.putText(
                frameClone,
                text,
                location,
                2,
                1,
                new Scalar(255, 255, 255),
                1
        );
    }
    private String getPrediction(Mat inputMat) {

        Bitmap image = Bitmap.createBitmap(inputMat.cols(), inputMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(inputMat, image);
        String result = null;

        try {
            OptimizedModel model = OptimizedModel.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 150, 100, 3}, DataType.FLOAT32);

            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * 150 * 100 * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[150 * 100];
            image.getPixels(intValues, 0, image.getWidth(), 0,0,image.getWidth(), image.getHeight());
            int pixel = 0;
            for (int row = 0; row < inputMat.rows(); row++) {
                for (int col = 0; col < inputMat.cols(); col++) {
                    int val = intValues[pixel++]; // RGB
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255));
                    byteBuffer.putFloat((val >> 0xFF)  * (1.f / 255));
                }
            }


            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            OptimizedModel.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();
            // find the index of the class with the highest confidence
            int maxPos = 0;
            float maxConfidence = 0;
            for (int i = 0; i < confidences.length; i++) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }

            result = classes[maxPos];

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            Log.e("openCV", "IOEXception occured with tensorflow model");
        }
        return result;
    }

    private ArrayList<Mat> isolateCards(int width, int height) {
        ArrayList<Mat> isolatedCards = new ArrayList<>();
        for (MatOfPoint contour : contourClone) {
            Mat card = CardFinder.isolateCard(contour, frameClone, width, height);
            isolatedCards.add(card);
        }

        return isolatedCards;
    }
    private void rotateFrame(Mat mat) {
        Core.flip(mat.t(), mat, 1); // this will rotate the image 90° clockwise
    }
    private void drawFrames() {
        // TODO: Draw all frames with the three cards that make a SET
        // Remember to rotate frames because Android draws frames -90 degrees clockwise.
        frames.clear();
        rotateFrame(lastFrame);
        frames.add(lastFrame);
        if (frameClone != null) {
            Mat tmp = frameClone.clone();
            rotateFrame(tmp);
            frames.add(tmp);
        }

        if (setFrames.size() > 0) {
            for (Mat frame : setFrames) {
                rotateFrame(frame);
                frames.add(frame);
            }
        }
    }
    private void matToImage(Mat mat, ImageView iv) {
        if (mat.empty()) {
            return;
        }
        Bitmap bm = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bm);

        iv.setImageBitmap(bm);
    }
    private void turnOffCamera() {
        mOpenCvCameraView.disableView();

        mOpenCvCameraView.setVisibility(View.GONE);
        staticCameraView.setVisibility(View.VISIBLE);

        frameIdx = 0;
        forwardButton.setEnabled(true);
        backButton.setEnabled(true);

        cameraState = false;
    }
    private void turnOnCamera() {
        mOpenCvCameraView.setVisibility(View.VISIBLE);
        staticCameraView.setVisibility(View.GONE);

        mOpenCvCameraView.enableView();

        forwardButton.setEnabled(false);
        backButton.setEnabled(false);

        cameraState = true;
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        // This was needed according to the documentation. Not sure why, maybe look into at some point?
        return Collections.singletonList(mOpenCvCameraView);
    }

    // Permission Requests.
    void getPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.CAMERA}, 101);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            getPermission();
        }
    }

    /*

    Experiment to try and get the Flashlight enabled. Does not work yet.

    private void turnOnFlashlight() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);

            if (characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)) {
                cameraManager.setTorchMode(cameraId, true);
            }
        } catch (CameraAccessException e) {
            Log.e("Camera", "Failed to turn on flashlight: " + e.getMessage());
        }
    }*/
}