package com.setfinder.setsolver;

import org.opencv.core.Point;

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
}
