/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package me.ziccard.secureit.opengl;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class AccelerometerGLSurfaceView extends GLSurfaceView {
	
	public AccelerometerGLRenderer renderer;

    public AccelerometerGLSurfaceView(Context context){
        super(context);
        
        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);
        
        renderer = new AccelerometerGLRenderer(context);

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(renderer);
    }
}
