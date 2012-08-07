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
import android.graphics.Path.Direction;
import android.view.MotionEvent;
import android.view.View;

public class DrawingCanvas extends View{
	private final float STROKE_WIDTH = 10f;
	private final float MIN_DISTANCE = 0.5f;
	
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
	
	// -- Interpolation -----------------------------------------------------------------

	private double b0(double t){
		return Math.pow(1-t, 3);
	}
	
	private double b1(double t){
		return t*(1-t)*(1-t)*3;
	}
	
	private double b2(double t){
		return (1-t)*t*t*3;
	}
	
	private double b3(double t){
		return Math.pow(t, 3);
	}
	
	private ArrayList<Double> solvexy(double a, double b, double c, double d, double e, double f){
		ArrayList<Double> coords = new ArrayList<Double>();
		
		double j = (c - a / d * f) / (b - a * e / d);
        double i = (c - (b * j)) / a;
		
        coords.add(new Double(i));
        coords.add(new Double(j));
        
		return coords;
	}
	
	private ArrayList<Point> getControlPointsFrom(Point p0, Point p1, Point p2, Point p3){
		ArrayList<Point> cp = new ArrayList<Point>();
		
		// Get chord lengths
		double c1 = Math.sqrt((p1.x-p0.x)*(p1.x-p0.x)+(p1.y-p0.y)*(p1.y-p0.y));
		double c2 = Math.sqrt((p2.x-p1.x)*(p2.x-p1.x)+(p2.y-p1.y)*(p2.y-p1.y));
		double c3 = Math.sqrt((p3.x-p2.x)*(p3.x-p2.x)+(p3.y-p2.y)*(p3.y-p2.y));
		
		double t1 = c1/(c1+c2+c3);
		double t2 = (c1+c2)/(c1+c2+c3);
		
		ArrayList<Double> xCoords = solvexy(b1(t1), b2(t1), p1.x - (p0.x * b0(t1)) - (p3.x * b3(t1)), b1(t2), b2(t2), p2.x - (p0.x * b0(t2)) - (p3.x * b3(t2)));
		ArrayList<Double> yCoords = solvexy(b1(t1), b2(t1), p1.y - (p0.y * b0(t1)) - (p3.y * b3(t1)), b1(t2), b2(t2), p2.y - (p0.y * b0(t2)) - (p3.y * b3(t2)));
		
		cp.add(new Point((float)xCoords.get(0).doubleValue(), (float)yCoords.get(0).doubleValue()));
		cp.add(new Point((float)xCoords.get(1).doubleValue(), (float)yCoords.get(1).doubleValue()));
		return cp;
	}
	
	// -- Interpolation -----------------------------------------------------------------
	
	private void generateCurrentPath(){
		if(null != mCrntLine){
			mCrntPath = new Path();
			mCrntPath.moveTo(mCrntLine.points.get(0).x, mCrntLine.points.get(0).y);
			
			int nPts = mCrntLine.points.size();
			if(1 == nPts){
				Point pn = mCrntLine.points.get(0);
				mCrntPath.lineTo(pn.x, pn.y);
			}else if(4 > nPts){
				for(int i=1; i<nPts; i++){
					Point pn = mCrntLine.points.get(i);
					mCrntPath.lineTo(pn.x, pn.y);
				}
			}else{
				Point c0;
				Point c1 = new Point();
				for(int i=3; i<nPts; i+=4){
					Point p0 = mCrntLine.points.get(i-3);
					Point p1 = mCrntLine.points.get(i-2);
					Point p2 = mCrntLine.points.get(i-1);
					Point p3 = mCrntLine.points.get(i);
					
					ArrayList<Point> cp = getControlPointsFrom(p0, p1, p2, p3);
					
					if(3 == i){
						c0 = cp.get(0);
					}else{
						// c0 must be the inverse of the previous c1
						c0 = new Point();
						Point prevLast = mCrntLine.points.get(i-4);
						c0.x = prevLast.x + (prevLast.x - c1.x);
						c0.y = prevLast.y + (prevLast.y - c1.y);
					}
					c1 = cp.get(1);
					
					mCrntPath.cubicTo(c0.x, c0.y, c1.x, c1.y, p3.x, p3.y);
				}
			}
		}
	}
	
	public void drawSmoothed(Canvas c){
		// Render the previous lines
		/*for(int i=0; i<mPreviousLines.size(); i++){
			c.drawPath(mPreviousLines.get(i), mPaint);
		}*/
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
				generateCurrentPath();
				mPreviousPoint = p;
			}
			break;
			
		case MotionEvent.ACTION_UP:
			p = new Point(ev.getX(), ev.getY());
			if(Math.abs(mPreviousPoint.x - p.x) >= MIN_DISTANCE && Math.abs(mPreviousPoint.y - p.y) >= MIN_DISTANCE){
				generateCurrentPath();
				mCrntPath.lineTo(p.x, p.y);
				mCrntLine.addPoint(p);
				mPreviousPoint = p;
			}
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
