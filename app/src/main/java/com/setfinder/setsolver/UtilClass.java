package com.setfinder.setsolver;

import android.graphics.Bitmap;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class UtilClass {
    public static double eucilidean(Point a, Point b) {
        double v1 = Math.pow(b.x - a.x, 2);
        double v2 = Math.pow(b.y - a.y, 2);

        return Math.sqrt(v1 + v2);
    }

    public static double eucilidean_fast(Point a, Point b) {
        double v1 = Math.pow(b.x - a.x, 2);
        double v2 = Math.pow(b.y - a.y, 2);

        return v1 + v2;
    }

    public static double getMinValue(double[] numbers) {
        double minValue = numbers[0];
        for (int i = 1; i < numbers.length; i++) {
            if (numbers[i] < minValue)
                minValue = numbers[i];
        }
        return minValue;
    }

    public static double getMaxValue(double[] numbers) {
        double maxValue = numbers[0];
        for (int i = 1; i < numbers.length; i++) {
            if (maxValue < numbers[i])
                maxValue = numbers[i];
        }
        return maxValue;
    }

    public static List<Point> getCornersFromPoints(final List<Point> points) {
        double minX = 0;
        double minY = 0;
        double maxX = 0;
        double maxY = 0;
        for (Point point : points) {
            double x = point.x;
            double y = point.y;

            if (minX == 0 || x < minX) {
                minX = x;
            }
            if (minY == 0 || y < minY) {
                minY = y;
            }
            if (maxX == 0 || x > maxX) {
                maxX = x;
            }
            if (maxY == 0 || y > maxY) {
                maxY = y;
            }
        }
        List<Point> corners = new ArrayList<>(4);
        corners.add(new Point(minX, minY));
        corners.add(new Point(minX, maxY));
        corners.add(new Point(maxX, minY));
        corners.add(new Point(maxX, maxY));
        return corners;
    }

    public static void rotateFrame(Mat mat) {
        Core.flip(mat.t(), mat, 1); // this will rotate the image 90° clockwise
    }

    public static void putText(String text, Mat frame, Point location) {
        Imgproc.putText(
                frame,
                text,
                location,
                2,
                0.5,
                new Scalar(0, 0, 0),
                5
        );
        Imgproc.putText(
                frame,
                text,
                location,
                2,
                0.5,
                new Scalar(255, 255, 255),
                1
        );
    }

    /**
     * Converts the given Mat to a bitmap and applies it to an ImageView
     * @param mat the input Mat that shall be converted and put into the ImageView
     * @param iv the ImageView that the Mat will be put into.
     */
    public static void matToImage(Mat mat, ImageView iv) {
        if (mat.empty()) {
            return;
        }
        Bitmap bm = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bm);

        iv.setImageBitmap(bm);
    }
}
