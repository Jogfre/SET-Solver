package com.setfinder.setsolver;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

public class Card {

    final private Mat frame;
    final private Mat isolatedCard;
    final private MatOfPoint contour;

    final private Point center;



    private String code = "none";



    private String numCode = "none";



    private String amount = "none";
    private String color = "none";
    private String shape = "none";
    private String filling = "none";


    public Card(Mat frame, Mat isolatedCard, MatOfPoint contour) {
        this.frame = frame;
        this.isolatedCard = isolatedCard;
        this.contour = contour;

        Moments moment = Imgproc.moments(this.contour);
        int x = (int) (moment.get_m10() / moment.get_m00());
        int y = (int) (moment.get_m01() / moment.get_m00());
        this.center = new Point(x, y);
    }
    public String generateString() {
        return "[" + this.amount + ", " + this.color + ", " + this.filling + ", " + this.shape + "]";
    }
    private void codeParser() {
        // TODO: generate the color, shape, amount and filling from the code. Format is <amount, color, filling, shape>
        char[] chars = code.toCharArray();

        setAmount("" + chars[0]);
        StringBuilder newNumCode = new StringBuilder();
        newNumCode.append(chars[0]);
        String tmp = "";

        switch (chars[1]) {
            case 'B':
                setColor("Blue");
                tmp = "1";
                break;
            case 'G':
                setColor("Green");
                tmp = "2";
                break;
            case 'R':
                setColor("Red");
                tmp = "3";
                break;
        }
        newNumCode.append(tmp);

        switch (chars[2]) {
            case 'E':
                setFilling("Empty");
                tmp = "1";
                break;
            case 'F':
                setFilling("Solid");
                tmp = "2";
                break;
            case 'S':
                setFilling("Striped");
                tmp = "3";
                break;
        }
        newNumCode.append(tmp);

        switch (chars[3]) {
            case 'D':
                setShape("Diamond");
                tmp = "1";
                break;
            case 'C':
                setShape("Cylinder");
                tmp = "2";
                break;
            case 'S':
                setShape("Squiggle");
                tmp = "3";
                break;
        }
        newNumCode.append(tmp);
        this.numCode = newNumCode.toString();
    }

    /*
    -------- Setters --------
     */
    public void setCode(String code) {
        this.code = code;
        codeParser();
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    public void setFilling(String filling) {
        this.filling = filling;
    }



    /*
    -------- Getters --------
     */
    public Mat getFrame() {
        return this.frame;
    }

    public Mat getIsolatedCard(){
        return this.isolatedCard;
    }

    public MatOfPoint getContour() {
        return this.contour;
    }

    public String getCode() {
        return this.code;
    }

    public String getAmount() {
        return amount;
    }
    public String getColor() {
        return color;
    }

    public String getShape() {
        return shape;
    }

    public String getFilling() {
        return filling;
    }

    public Point getCenter() {
        return center;
    }

    public String getNumCode() {
        return numCode;
    }
}
