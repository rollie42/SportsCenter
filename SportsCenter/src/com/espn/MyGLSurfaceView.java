package com.espn;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class MyGLSurfaceView extends GLSurfaceView 
{
	public MyGLSurfaceView(Context context)
	{
		super(context);
				
		//setEGLContextClientVersion(2); // this causes problems - why? OpenGL ES 2 not available on emulator?
		setRenderer(new MyGLRenderer());		
		
		// View should always be dirty, so no need to set dirty-only rendermode		
	}

}
