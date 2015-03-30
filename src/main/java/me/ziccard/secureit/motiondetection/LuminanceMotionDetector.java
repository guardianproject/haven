/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package me.ziccard.secureit.motiondetection;

import java.util.ArrayList;
import java.util.List;

public class LuminanceMotionDetector implements IMotionDetector {
	
	/**
	 * Difference in luma for each pixel
	 */
	private int VALUE_THRESHOLD = 50;
	/**
	 * Difference in number of pixel for each image
	 */
	private int NUMBER_THRESHOLD = 10000;
	
	/**
	 * Levels of motion detection
	 */
	public static final int MOTION_LOW = 0;
	public static final int MOTION_MEDIUM = 1;
	public static final int MOTION_HIGH = 2;
	
	/**
	 * Sets different sensitivity for the algorithm
	 * @param thresh sensitivity identifier
	 */	
	public void setThreshold(int thresh) {
		switch(thresh) {
		case MOTION_LOW:
			VALUE_THRESHOLD = 60;
			NUMBER_THRESHOLD = 20000;
			break;
		case MOTION_MEDIUM:
			VALUE_THRESHOLD = 50;
			NUMBER_THRESHOLD = 10000;
			break;
		case MOTION_HIGH:
			VALUE_THRESHOLD = 40;
			NUMBER_THRESHOLD = 9000;
			break;
		}
		
	}

	/*
	 * (non-Javadoc)
	 * @see me.ziccard.secureit.motiondetection.IMotionDetector#detectMotion(int[], int[], int, int)
	 */
	public List<Integer> detectMotion(int[] oldImage, int[] newImage, int width,
			int height) {
		if (oldImage == null || newImage == null) throw new NullPointerException();
		if (oldImage.length != newImage.length) throw new IllegalArgumentException();
		
		ArrayList<Integer> differentPixels = new ArrayList<Integer>();	
		int differentPixelNumber = 0;
		for (int ij=0; ij < height*width; ij++) {
		  int newPixelValue = newImage[ij];
          int oldPixelValue = oldImage[ij];
          if (Math.abs(newPixelValue - oldPixelValue) >= VALUE_THRESHOLD) {
            differentPixelNumber++;
            differentPixels.add(ij);
          }
		}
		
		if (differentPixelNumber > NUMBER_THRESHOLD) {
		  return differentPixels;
		}
		
		return null;
	}

}
