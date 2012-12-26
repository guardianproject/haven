package me.ziccard.secureit.codec;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.graphics.Color;
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
	 * Converts a luminance matrix to a grayscale bitmap
	 * @param lum
	 * @param width
	 * @param height
	 * @return
	 */
    public static Bitmap lumaToGreyscale(int[] lum, int width, int height) {
        if (lum==null) throw new NullPointerException();
        
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for(int y=0, xy=0; y<bitmap.getHeight(); y++) {
                for(int x=0; x<bitmap.getWidth(); x++, xy++) {
                        int luma = lum[xy];
                        bitmap.setPixel(x,y,Color.rgb(luma,luma,luma));
                }
        }
        return bitmap;
    }
    
    /**
     * Rotates a bitmat of the given degrees
     * @param bmp
     * @param degrees
     * @return
     */
    public static Bitmap rotate(Bitmap bmp, int degrees) {
        if (bmp==null) throw new NullPointerException();
        
		//getting scales of the image  
		int width = bmp.getWidth();  
		int height = bmp.getHeight();  
		
		//Creating a Matrix and rotating it to 90 degrees   
		Matrix matrix = new Matrix();  
		matrix.postRotate(degrees);  
		
		//Getting the rotated Bitmap  
		Bitmap rotatedBmp = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, true);
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		rotatedBmp.compress(Bitmap.CompressFormat.JPEG, 100, stream); 
		return rotatedBmp;
	}

}
