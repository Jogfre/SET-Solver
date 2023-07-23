package com.setfinder.setsolver;

import android.util.Log;

import androidx.annotation.NonNull;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CardFinder {

    /**
     * <H3>drawContours()</H3>
     * Takes an input Matrix from openCV and draws contours on any objects it finds.
     * Makes use of the "findAllCards" and "preProcessImage" functions.
     * @param inputMat an openCV Mat object that will be drawn on.
     * @param contours a List containing the MatOfPoint of all contours that will be drawn.
     */
    public static void drawContours(Mat inputMat, List<MatOfPoint> contours) {

        if (contours.size() > 0) {
            Log.d("CardFinder", "trying to draw contours");

            // Draw Color contour
            // Project Primary Color: new Scalar(124, 230, 230)
            Imgproc.drawContours(inputMat, contours, -1, new Scalar(0, 255, 0), 6);

            // Draw Black contour on-top of the color one
            Imgproc.drawContours(inputMat, contours, -1, new Scalar(0, 0, 0), 2);
        }
    }

    /**
     * <H3>createIsolatedCard()</H3>
     * This function will take the contour and crop it out of the input frame and then transform the perspective to be upright and then
     * match it to the input width and height. The function will only work properly if the given contour has 4 clearly defined corners.
     * If more corners are present, the results will be unreliable.
     *
     * @param contour an openCV MatOfPoint object that will be used to define the region that will be extracted from the frame.
     * @param inputFrame an openCV Mat object that will be the base of the extracted region.
     * @param width an integer that defines how wide (in pixels) the output Mat will be.
     * @param height an integer that defines how tall (in pixels) the output Mat will be.
     * @return an openCV Mat with a transformed region that has been extracted from the input Mat.
     * @see #sortPoints
     */
    public static Mat createIsolatedCard(MatOfPoint contour, Mat inputFrame, int width, int height) {

        // Get the 4 corners of the card from the contours
        // Source: https://stackoverflow.com/questions/44156405/opencv-java-card-extraction-from-image
        MatOfPoint2f quadrilateral = new MatOfPoint2f();
        MatOfPoint2f convexShape = new MatOfPoint2f(contour.toArray());
        Imgproc.approxPolyDP(convexShape,quadrilateral,20, true);

        //Sort the points so the topLeft is always in the correct spot.
        Point[] sortedPoints = sortPoints(quadrilateral.toList(), contour);

        // Corner points of the card that should be transformed
        MatOfPoint2f src = new MatOfPoint2f(
                sortedPoints[0],
                sortedPoints[1],
                sortedPoints[2],
                sortedPoints[3]
        );

        // Corner points of the target destination that the card should be transformed into
        MatOfPoint2f dst = new MatOfPoint2f(
                // Points in order of: Top-left, Top-Right, Bottom-Left, Bottom-Right
                // The -1 is there because the pixels are 0-indexed and the width/height are indexed from 1.
                new Point(0, 0),
                new Point(width - 1, 0),
                new Point(0, height - 1),
                new Point(width- 1,  height - 1)
        );

        // Calculate the warpMat (matrix transformation)
        Mat warpMat = Imgproc.getPerspectiveTransform(src, dst);

        // Apply the transformation to a new destination Mat
        Mat destImage = new Mat();
        Imgproc.warpPerspective(inputFrame, destImage, warpMat, inputFrame.size());
        Rect rect = Imgproc.boundingRect(dst);

        return destImage.submat(rect);
    }

    /**
     * <H3>sortPoints()</H3>
     * The function will take a list of points and sort them so they are in the 'correct' orientation for the isolateCards() function.
     * The points are sorted in the order of: Top left, Top Right, Bottom right, Bottom left.
     *
     * @param points a List containing the points that will be sorted.
     * @param contour the contour the points were extracted from (as a backup if the number of points exceed 4)
     * @return An array of size 4 containing the points in the above mentioned sorted order.
     */
    public static Point[] sortPoints(List<Point> points, MatOfPoint contour) {
        Point[] sortedPoints = new Point[4];

        if (points.size() != 4) {
            List<Point> corners = UtilClass.getCornersFromPoints(contour.toList());
            //Calculate the center of mass of contour using moments
            Moments moment = Imgproc.moments(contour);
            int x = (int) (moment.get_m10() / moment.get_m00());
            int y = (int) (moment.get_m01() / moment.get_m00());

            Point data;
            for (int i = 0; i < 4; i++) {
                data = corners.get(i);
                double dataX = data.x;
                double dataY = data.y;
                if (dataX < x && dataY < y) {
                    sortedPoints[0] = new Point(dataX, dataY);
                } else if (dataX > x && dataY < y) {
                    sortedPoints[1] = new Point(dataX, dataY);
                } else if (dataX < x && dataY > y) {
                    sortedPoints[2] = new Point(dataX, dataY);
                } else if (dataX > x && dataY > y) {
                    sortedPoints[3] = new Point(dataX, dataY);
                }
            }
            return sortedPoints;
        }

        sortedPoints[0] = points.get(1); // Top left
        sortedPoints[1] = points.get(0); // Top Right
        sortedPoints[2] = points.get(2); // Bottom Right
        sortedPoints[3] = points.get(3); // Bottom Left

        double distanceShort = UtilClass.eucilidean_fast(sortedPoints[0],sortedPoints[1]) * 1.1;
        double distanceLong = UtilClass.eucilidean_fast(sortedPoints[0], sortedPoints[2]);

        // If the card is closer to the horizontal axis
        if (distanceShort > distanceLong) {
            Point p0 = sortedPoints[0];
            Point p1 = sortedPoints[1];
            Point p2 = sortedPoints[2];
            Point p3 = sortedPoints[3];

            sortedPoints[0] = p1;
            sortedPoints[1] = p3;
            sortedPoints[2] = p0;
            sortedPoints[3] = p2;
        }
        return sortedPoints;
    }

    public static List<MatOfPoint> findAllContours(final Mat inputMat) {
        //Find the contours of an image
        final Mat processedImage = preProcessImage(inputMat);
        final int noiseThreshold = 3000;

        final List<MatOfPoint> allContours = new ArrayList<>();
        Imgproc.findContours(
                processedImage,
                allContours,
                new Mat(processedImage.size(), processedImage.type()),
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_NONE
        );

        return allContours.stream().filter(contour -> { // Lambda function to filter out contours that are not cards.
            final double area = Imgproc.contourArea(contour);
            final boolean isNotNoise = area > noiseThreshold;

            // Approximate the corners of the given contour to check if there are only 4
            MatOfPoint2f quadrilateral = new MatOfPoint2f();
            MatOfPoint2f convexShape = new MatOfPoint2f(contour.toArray());
            Imgproc.approxPolyDP(convexShape,quadrilateral,8, true);
            List<Point> points = quadrilateral.toList();
            return isNotNoise && points.size() == 4; // If true, will add the contour to filteredContours

        }).collect(Collectors.toList());
    }
    @NonNull
    private static Mat preProcessImage(final Mat originalImage) {

        final Mat processedImage = new Mat();
        Imgproc.cvtColor(originalImage, processedImage, Imgproc.COLOR_BGR2GRAY);

        /*
        Sample the background to get a light level to set the Threshold value.
        Calculate the average brightness across two points and add 60 to get Threshold.
         */
        final int THRESH_VALUE = 80;
        int BLUR_VALUE = 1;
        int DEPTH = 30;


        final Mat Gray = processedImage.clone();

        int img_h = Gray.height();
        int img_w = Gray.width();
        double sample  = 0;

        Point[] samples = {
                new Point(img_w - DEPTH, img_h - DEPTH),
                new Point(img_w - DEPTH, DEPTH),
                new Point(DEPTH, img_h - DEPTH),
                new Point(DEPTH, DEPTH)
        };

        for (Point p : samples) {
            sample += Gray.get((int)p.y, (int)p.x)[0];
        }
        sample /= samples.length;
        double thresh = sample + THRESH_VALUE;
        if (thresh > 110) {
            thresh += 40;
            BLUR_VALUE += 4;
        }

        Imgproc.blur(processedImage, processedImage,new Size(BLUR_VALUE,BLUR_VALUE));
        Imgproc.threshold(processedImage, processedImage, thresh, 255,Imgproc.THRESH_BINARY);
        Imgproc.putText(processedImage, "Threshold: " + thresh, new Point(10, 30), 2, 0.5, new Scalar(128, 128, 128), 1);
        for (Point p : samples ) {
            Imgproc.drawMarker(processedImage, p, new Scalar(128, 128, 128));
        }
        return processedImage;
    }
}
