package com.espn;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;


public class HeaderSeparator extends View  {
	private final Paint mPaint = new Paint();
	private int mColor = 0xff00bd39; 
	public HeaderSeparator(Context c, AttributeSet attrs) { 
		this(c, attrs, 0);
	}
	
	public HeaderSeparator(Context c, AttributeSet attrs, int defStyle) {
		super(c, attrs, defStyle);
		mPaint.setStrokeWidth(0);
		mPaint.setColor(mColor); //  make customizable
		mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		mPaint.setAntiAlias(true);		
	}
	
	@Override
	public void setPressed(boolean pressed) {
		if (pressed) {
			
		}
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		
		int height = getHeight();		
		int width = height;
		
		float offset =  height * 0.3f;

	    Path path = new Path();
	    path.setFillType(Path.FillType.EVEN_ODD);
	    path.moveTo(offset,offset);
	    path.lineTo(offset,height - offset);
	    path.lineTo(width - offset, height/2);
	    path.lineTo(offset,offset);
	    path.close();

	    canvas.drawLine((width)/2, 0, (width)/2, height, mPaint);
	    canvas.drawPath(path, mPaint);
	    //invalidate();
	}
	
	@Override
	protected void onMeasure(int widthMS, int heightMS) {
		super.onMeasure(widthMS, heightMS);
		
	    int parentHeight = MeasureSpec.getSize(heightMS);
	    setMeasuredDimension((int)(parentHeight * 0.8), parentHeight);
	}
}