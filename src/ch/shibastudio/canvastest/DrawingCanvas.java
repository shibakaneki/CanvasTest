package ch.shibastudio.canvastest;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

public class DrawingCanvas extends View{
		
	private final float STROKE_WIDTH = 10f;
	private final float MIN_DISTANCE = 1f;
	private final int SMOOTHING = 8;
	
	private Paint mPaint;
	private Line mCrntLine;
	private Point mPreviousPoint;
	private ArrayList<Path> mPreviousLines;
	private Path mCrntPath;
	private Bitmap mCache;
	
	public DrawingCanvas(Context c){
		super(c);
		mPreviousLines = new ArrayList<Path>();
		mPaint = new Paint();
		mPaint.setStrokeCap(Cap.ROUND);
		mPaint.setStrokeJoin(Join.ROUND);
		mPaint.setAntiAlias(true);
		mPaint.setStyle(Style.STROKE);
		mPaint.setStrokeWidth(STROKE_WIDTH);
	}
	
	@Override
	public void onDraw(Canvas c){
		super.onDraw(c);
		c.drawColor(Color.WHITE);
		drawSmoothed(c);
	}
	
	private void generateCurrentPath(){
		if(null != mCrntLine){
			mCrntPath = new Path();
			Point c0 = new Point();
			Point c1 = new Point();
			if(mCrntLine.points.size() > 1){
		        for(int i = mCrntLine.points.size() - 2; i < mCrntLine.points.size(); i++){
		            if(i >= 0){
		                if(i == 0){
		                    mCrntLine.points.get(i).dx = ((mCrntLine.points.get(i + 1).x - mCrntLine.points.get(i).x) / SMOOTHING);
		                    mCrntLine.points.get(i).dy = ((mCrntLine.points.get(i + 1).y - mCrntLine.points.get(i).y) / SMOOTHING);
		                }
		                else if(i == mCrntLine.points.size() - 1){
		                    mCrntLine.points.get(i).dx = ((mCrntLine.points.get(i).x - mCrntLine.points.get(i - 1).x) / SMOOTHING);
		                    mCrntLine.points.get(i).dy = ((mCrntLine.points.get(i).y - mCrntLine.points.get(i - 1).y) / SMOOTHING);
		                }
		                else{
		                    mCrntLine.points.get(i).dx = ((mCrntLine.points.get(i + 1).x - mCrntLine.points.get(i - 1).x) / SMOOTHING);
		                    mCrntLine.points.get(i).dy = ((mCrntLine.points.get(i + 1).y - mCrntLine.points.get(i - 1).y) / SMOOTHING);
		                }
		            }
		        }
		    }

		    boolean first = true;
		    for(int i = 0; i < mCrntLine.points.size(); i++){
		        Point point = mCrntLine.points.get(i);
		        if(first){
		            first = false;
		            mCrntPath.moveTo(point.x, point.y);
		        }
		        else{
		        	Point prev = mCrntLine.points.get(i - 1);
		        	c0.x = prev.x + prev.dx;
		        	c0.y = prev.y + prev.dy;
		        	c1.x = point.x - point.dx;
		        	c1.y = point.y - point.dy;
		            
		            mCrntPath.cubicTo(c0.x, c0.y, c1.x, c1.y, point.x, point.y);
		        }
		    }
		}
	}
	
	public void drawSmoothed(Canvas c){
		// Render the previous lines
		if(null != mCache){
			c.drawBitmap(mCache, 0, 0, mPaint);
		}
		
		// Render the current line
		if(null != mCrntPath){
			c.drawPath(mCrntPath, mPaint);
		}	
	}
	
	private void refreshCache(){
		mCache = Bitmap.createBitmap(getWidth(), getHeight(), Config.ARGB_8888);
		Canvas cacheCanvas = new Canvas(mCache);
		for(int i=0; i<mPreviousLines.size(); i++){
			cacheCanvas.drawPath(mPreviousLines.get(i), mPaint);
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent ev){
		System.out.println("-- getting onTouchEvent --");
		Point p;
		switch(ev.getAction()){
		case MotionEvent.ACTION_DOWN:
			mCrntLine = new Line();
			p = new Point(ev.getX(), ev.getY());
			mCrntLine.addPoint(p);
			generateCurrentPath();
			mPreviousPoint = p;
			break;
				
		case MotionEvent.ACTION_MOVE:
			p = new Point(ev.getX(), ev.getY());
			if(Math.abs(mPreviousPoint.x - p.x) >= MIN_DISTANCE && Math.abs(mPreviousPoint.y - p.y) >= MIN_DISTANCE){
				mCrntLine.addPoint(p);		
				mPreviousPoint = p;
			}
			generateCurrentPath();
			break;
			
		case MotionEvent.ACTION_UP:
			p = new Point(ev.getX(), ev.getY());
			generateCurrentPath();
			mCrntPath.lineTo(p.x, p.y);
			mCrntLine.addPoint(p);
			mPreviousPoint = p;
			mPreviousLines.add(mCrntPath);
			refreshCache();
			mCrntLine = null;
			mCrntPath = null;
			break;
			
		default:
			break;
		}
		
		invalidate();
		return true;
	}
}
