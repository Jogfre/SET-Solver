package com.setfinder.setsolver;

import android.util.Log;

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


    public static void drawIdentifiedCards(Mat inputMat, List<MatOfPoint> contours) {
        //TODO: Implement function that draws what card is what on screen.
    }

    /**
     * Takes an input Matrix from openCV and draws contours on any objects it finds.
     * Makes use of the "findAllCards" and "preProcessImage" functions.
     * @param inputMat the mat that will be drawn on.
     * @param contours contours that will be drawn.
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

    public static Mat isolateCard(MatOfPoint contour, Mat inputFrame, int width, int height) {
        //Sort points relative to center of mass
        Point[] sortedPoints = new Point[4];
        List<Point> corners = UtilClass.getCornersFromPoints(contour.toList());

        // Black magic shit to get the 4 corners from the contours
        // Source: https://stackoverflow.com/questions/44156405/opencv-java-card-extraction-from-image
        MatOfPoint2f quadrilateral = new MatOfPoint2f();
        MatOfPoint2f convexShape = new MatOfPoint2f(contour.toArray());
        Imgproc.approxPolyDP(convexShape,quadrilateral,20, true);

        //Sort the points so the topLeft is always in the correct spot.
        List<Point> points = quadrilateral.toList();
        if (points.size() == 4) {
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

        } else {
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
        }

        //Prepare Mat src and dst
        MatOfPoint2f src = new MatOfPoint2f(
                sortedPoints[0],
                sortedPoints[1],
                sortedPoints[2],
                sortedPoints[3]
        );
        MatOfPoint2f dst = new MatOfPoint2f(
                // Points in order of: Top-left, Top-Right, Bottom-Left, Bottom-Right
                new Point(0, 0),
                new Point(width - 1, 0),
                new Point(0, height - 1),
                new Point(width- 1,  height - 1)
        );
        Mat warpMat = Imgproc.getPerspectiveTransform(src, dst);

        Mat destImage = new Mat();
        Imgproc.warpPerspective(inputFrame, destImage, warpMat, inputFrame.size());
        Rect rect = Imgproc.boundingRect(dst);

        //HighGui.imshow("Isolated Card", destImage.submat(rect));

        return destImage.submat(rect);
    }

    public static List<MatOfPoint> findAllContours(final Mat inputMat) {
        //Find the contours of an image
        final Mat processedImage = preProcessImage(inputMat);

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
            final boolean isNotNoise = area > 3_000;

            // Approximate the corners of the given contour to check if there are only 4
            MatOfPoint2f quadrilateral = new MatOfPoint2f();
            MatOfPoint2f convexShape = new MatOfPoint2f(contour.toArray());
            Imgproc.approxPolyDP(convexShape,quadrilateral,8, true);
            List<Point> points = quadrilateral.toList();
            return isNotNoise && points.size() == 4; // If true, will add the contour to filteredContours

        }).collect(Collectors.toList());
    }
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
