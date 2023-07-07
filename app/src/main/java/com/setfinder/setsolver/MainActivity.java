package com.setfinder.setsolver;

import androidx.annotation.NonNull;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.Manifest;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;

import java.util.Collections;
import java.util.List;

public class MainActivity extends CameraActivity {
    // Variable declarations
    private CameraBridgeViewBase mOpenCvCameraView;


    // Main function for the Activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the required permissions
        getPermission();

        // Attach the cameraView to the corresponding JavaCameraView
        mOpenCvCameraView = findViewById(R.id.cameraView);
        mOpenCvCameraView.enableFpsMeter();

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

                // Mat isolatedCard = CardFinder.isolateCard(contours.get(0), frame, 100, 150);




                if (contours.size() > 0) {
                    CardFinder.drawContours(frame, contours);
                }

                return frame;
            }
        });

        // Check that OpenCV has loaded
        if (OpenCVLoader.initDebug()) {
            mOpenCvCameraView.enableView();
        } else {
            Log.e("openCV", "Failed to load openCV library");
        }
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