package com.espn;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView;
import android.opengl.GLU;

public class MyGLRenderer implements GLSurfaceView.Renderer
{
	private SplashLogo logo;
	
	public MyGLRenderer() {
		logo = new SplashLogo();
	}
	
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //// Set the background frame color
		gl.glClearColor(1f, 0.0f, 0.0f, 1.0f);	
    }

    public void onDrawFrame(GL10 gl) {
        // Redraw background color
    	gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
    	gl.glLoadIdentity();
    	gl.glTranslatef(0.0f, 0.0f, -5.0f);
    	logo.draw(gl);
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
    	if (height == 0)
		{
			height = 1;
		}
		
		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		
		GLU.gluPerspective(gl, 45.0f, (float)width/(float)height, 0.1f, 100.0f);
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
    }
}
