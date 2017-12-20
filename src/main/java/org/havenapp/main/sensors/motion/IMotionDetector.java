/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package org.havenapp.main.sensors.motion;

import java.util.List;

public interface IMotionDetector {
	
	/**
	 * Detects differences between old and new image
	 * and return pixel indexes that differ more than 
	 * a specified threshold
	 * @param oldImage
	 * @param newImage
	 * @param width
	 * @param height
	 * @return
	 */
	public List<Integer> detectMotion(int[] oldImage, int[] newImage, int width, int height);

	/**
	 * Sets the sensitivity
	 * @param thresh
	 */
	public void setThreshold(int thresh);
}
