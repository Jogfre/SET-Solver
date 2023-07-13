package com.setfinder.setsolver;


import androidx.annotation.NonNull;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.Manifest;
import android.view.View;
import android.widget.ImageView;


import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class MainActivity extends CameraActivity {
    // Variable declarations
    private CameraBridgeViewBase mOpenCvCameraView;
    private ImageView staticCameraView;
    private ImageView forwardButton;
    private ImageView backButton;
    private Mat lastFrame;
    private Mat frameClone;
    private ArrayList<Mat> frames;
    private ArrayList<Mat> setFrames;
    private ArrayList<Card> cards;
    private List<MatOfPoint> contourClone;
    private boolean cameraState = true;
    private int frameIdx = 0;


    // Main function for the Activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the required permissions
        getPermission();

        // Find Buttons and assign their functions
        ImageView iconButton = findViewById(R.id.centerButton);
        iconButton.setOnClickListener(v -> mainButton());

        forwardButton = findViewById(R.id.forwardButton);
        forwardButton.setOnClickListener(v -> cycleForward());

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> cycleBack());

        // Initialize ArrayLists
        frames = new ArrayList<>();
        setFrames = new ArrayList<>();
        cards = new ArrayList<>();

        // Attach the cameraView to the corresponding JavaCameraView
        staticCameraView = findViewById(R.id.staticCameraView);
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
                    UtilClass.rotateFrame(frameClone);
                    contourClone = CardFinder.findAllContours(frameClone);
                    CardFinder.drawContours(frame, contours);

                } else {
                    frameClone = null;
                    contourClone = null;
                }

                lastFrame = frame.clone();
                UtilClass.rotateFrame(lastFrame);
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

    /*
    ------------------------------- Buttons -------------------------------
     */

    private void mainButton() {
        if (cameraState) {
            turnOffCamera();
            getCardValues();
            drawFrames();
            UtilClass.matToImage(frames.get(0), staticCameraView);

        } else {
            turnOnCamera();
        }
    }
    private void cycleForward() {
        if (frameIdx + 1 < frames.size()) {
            frameIdx++;
            UtilClass.matToImage(frames.get(frameIdx), staticCameraView);
        }
    }
    private void cycleBack() {
        if (frameIdx - 1 >= 0) {
            frameIdx--;
            UtilClass.matToImage(frames.get(frameIdx), staticCameraView);
        }
    }
    /*
    ------------------------------- Methods -------------------------------
     */

    private void drawCardCodes(Mat frame) {
        if (cards.size() > 0) {
            for (Card card : cards) {
                Point center = card.getCenter();
                center.x = center.x - 100;
                UtilClass.putText(card.generateString(), frame, center);
            }
        }
    }

    private void getCardValues() {
        // Get isolated cards for each contour, then put that in a <Card class> and store all cards in a List/Map
        if (contourClone == null || contourClone.size() < 1) {
            return;
        }

        ArrayList<Mat> isolatedCards = isolateCards(contourClone, 100, 150);
        cards.clear();

        for (int i = 0; i < isolatedCards.size(); i++) {
            Mat isolatedFrame = isolatedCards.get(i);
            MatOfPoint contour = contourClone.get(i);
            Card card = new Card(frameClone, isolatedFrame, contour);
            cards.add(card);
        }

        // Send all cards to classifier model
        HashMap<String, Card> setMap = new HashMap<>();
        for (Card card : cards) {
            String code = CardClassifier.getPrediction(getApplicationContext(), card.getIsolatedCard());
            if (code != null) {
                card.setCode(code);
                setMap.put(card.getNumCode(), card);
                Log.d("openCV", card.generateString());
            }
        }

        // Calculate SETs from all the classified cards and store any SETs
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
    }

    private ArrayList<Mat> isolateCards(List<MatOfPoint> contours, int width, int height) {
        ArrayList<Mat> isolatedCards = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            Mat cardMat = CardFinder.isolateCard(contour, frameClone, width, height);
            isolatedCards.add(cardMat);
        }

        return isolatedCards;
    }
    private void drawFrames() {
        frames.clear();
        frames.add(lastFrame);
        if (frameClone != null) {
            Mat tmp = frameClone.clone();
            drawCardCodes(tmp);
            frames.add(tmp);
        }

        if (setFrames.size() > 0) {
            frames.addAll(setFrames);
        }
    }

    private void turnOffCamera() {
        // Turn off the camera and hide its ViewField.
        // Then enable the staticCameraView to display captured frames.
        // Also enables the navigation buttons.

        mOpenCvCameraView.disableView();

        mOpenCvCameraView.setVisibility(View.GONE);
        staticCameraView.setVisibility(View.VISIBLE);

        frameIdx = 0;
        forwardButton.setEnabled(true);
        backButton.setEnabled(true);

        cameraState = false;
    }
    private void turnOnCamera() {
        // Hide the staticCameraView and show the CameraView.
        // Also disable the navigation buttons.
        // Then turn on the camera.

        mOpenCvCameraView.setVisibility(View.VISIBLE);
        staticCameraView.setVisibility(View.GONE);

        forwardButton.setEnabled(false);
        backButton.setEnabled(false);

        mOpenCvCameraView.enableView();

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
}