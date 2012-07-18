package com.espn;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class SplashLogo {
	private FloatBuffer vertBuffer;
	private float adjust = 2.0f;
	private float logoWidth = 0.880f * adjust;
	private float logoHeight = 0.240f * adjust;
	
	private float logoVertices[] = {
		-0.5f * logoWidth, -0.5f * logoHeight, 0.0f,
		-0.5f * logoWidth, 0.5f * logoHeight, 0.0f,
		0.5f * logoWidth, -0.5f * logoHeight, 0.0f,
		0.5f * logoWidth, 0.5f * logoHeight, 0.0f
	};
	
	public SplashLogo() {
		ByteBuffer vertexByteBuffer = ByteBuffer.allocateDirect(logoVertices.length * 4);
		vertexByteBuffer.order(ByteOrder.nativeOrder());
		
		vertBuffer = vertexByteBuffer.asFloatBuffer();
		vertBuffer.put(logoVertices);
		vertBuffer.position(0);
	}
	
	public void draw(GL10 gl){
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		// todo: color not needed b/c of texture?
		gl.glColor4f(0.0f, 1.0f, 0.0f, 0.5f);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertBuffer);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, logoVertices.length / 3);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);	
	}
}
