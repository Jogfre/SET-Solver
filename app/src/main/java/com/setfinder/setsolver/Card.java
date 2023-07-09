package com.setfinder.setsolver;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;

public class Card {

    final private Mat frame;
    final private Mat isolatedCard;
    final private MatOfPoint contour;
    private String code = "none";



    private String amount = "none";
    private String color = "none";
    private String shape = "none";
    private String filling = "none";


    public Card(Mat frame, Mat isolatedCard, MatOfPoint contour) {
        this.frame = frame;
        this.isolatedCard = isolatedCard;
        this.contour = contour;
    }

    public void generateStrings() {
        // TODO: generate the color, shape, amount and filling from the code. Format is <amount, color, filling, shape>
    }

    /*
    -------- Setters --------
     */
    public void setCode(String code) {
        this.code = code;
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
}
