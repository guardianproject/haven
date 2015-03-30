/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package me.ziccard.secureit.codec;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.graphics.Matrix;

public class ImageCodec {
	
	/**
	 * Extracts the luminance component from the
	 * given YCbCr 420 image
	 */
	public static int[] N21toLuma(byte[] YUVimage, int width, int height) {
		if (YUVimage == null) throw new NullPointerException();
		
		final int frameSize = width*height;
		int[] lumaImage = new int[frameSize];
		
		for (int ij = 0; ij < height*width; ij++) {
		  int luminance = (0xff & ((int) YUVimage[ij])) - 16;
          if (luminance < 0) luminance = 0;
          lumaImage[ij] = luminance;
		}
		return lumaImage;
	}
	
	/**
	 * Converts a luminance matrix to a RGB grayscale bitmap
	 * @param lum
	 * @param width
	 * @param height
	 * @return
	 */
    public static int[] lumaToGreyscale(int[] lum, int width, int height) {
        if (lum==null) throw new NullPointerException();
        
        int[] greyscale = new int[height*width];
        for (int ij=0; ij<greyscale.length; ij++) {
          // create the RGB-grey color corresponding to the specified luma component
          greyscale[ij] = ((((lum[ij]<<8)|lum[ij])<<8)|lum[ij])&0x00FFFFFF;
        }
        return greyscale;
    }
    
    public static Bitmap lumaToBitmapGreyscale(int[] lum, int width, int height) {
      if (lum == null) throw new NullPointerException();
      
      return Bitmap.createBitmap(ImageCodec.lumaToGreyscale(lum, width, height), width, height, Bitmap.Config.RGB_565);
    }
    
    /**
     * Rotates a bitmat of the given degrees
     * @param bmp
     * @param degrees
     * @return
     */
    public static Bitmap rotate(Bitmap bmp, int degrees, boolean reflex) {
        if (bmp==null) throw new NullPointerException();
        
		//getting scales of the image  
		int width = bmp.getWidth();  
		int height = bmp.getHeight();  
		
		//Creating a Matrix and rotating it to specified degrees   
		Matrix matrix = new Matrix();
		matrix.postRotate(degrees);
		if (reflex)   matrix.postScale(-1, 1);
		
		//Getting the rotated Bitmap  
		Bitmap rotatedBmp = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, true);
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		rotatedBmp.compress(Bitmap.CompressFormat.JPEG, 100, stream); 
		return rotatedBmp;
	}

}
