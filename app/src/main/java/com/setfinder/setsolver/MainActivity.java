package com.setfinder.setsolver;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.Manifest;
import android.view.View;
import android.widget.ImageView;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends CameraActivity {
    // Variable declarations
    private CameraBridgeViewBase mOpenCvCameraView;
    private ImageView staticCameraView;
    private boolean cameraState = true;
    private int frameIdx = 0;

    Mat lastFrame;
    Mat frameClone;
    ArrayList<Mat> frames;
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

        // TODO: Send all cards to classifier model

        // TODO: Calculate SETs from all the classified cards and store any SETs

        // TODO: Render frames on the screen with 3 cards making up a SET highlighted.
    }

    private void showCards() {
        if (contourClone == null || contourClone.size() < 1) {
            return;
        }
        // Isolate the cards from the image by using the contours.
        ArrayList<Mat> isolatedCards = isolateCards(200, 300);

        // Add the RecyclerView and put all isolated cards into it.
        RecyclerView recyclerView = findViewById(R.id.mRecyclerView);
        Card_RecyclerViewAdapter adapter = new Card_RecyclerViewAdapter(this, isolatedCards);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
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