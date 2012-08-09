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
	
	private class ControlPoints{
		public Point c0;
		public Point c1;
		
		public ControlPoints(Point _c0, Point _c1){
			c0 = _c0;
			c1 = _c1;
		}
	}
	
	private final float STROKE_WIDTH = 10f;
	private final float MIN_DISTANCE = 0.5f;
	private final float SMOOTHING = 0.5f;
	
	private Paint mPaint;
	private Line mCrntLine;
	private Point mPreviousPoint;
	private ArrayList<Path> mPreviousLines;
	private ArrayList<ControlPoints> mCrntCtrlPts;
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
	private ArrayList<Point> getControlPointsFrom(Point p0, Point p1, Point p2, Point p3){
		ArrayList<Point> cp = new ArrayList<Point>();
		
		double xc1 = (p0.x + p1.x) / 2.0;
	    double yc1 = (p0.y + p1.y) / 2.0;
	    double xc2 = (p1.x + p2.x) / 2.0;
	    double yc2 = (p1.y + p2.y) / 2.0;
	    double xc3 = (p2.x + p3.x) / 2.0;
	    double yc3 = (p2.y + p3.y) / 2.0;

	    double len1 = Math.sqrt((p1.x-p0.x) * (p1.x-p0.x) + (p1.y-p0.y) * (p1.y-p0.y));
	    double len2 = Math.sqrt((p2.x-p1.x) * (p2.x-p1.x) + (p2.y-p1.y) * (p2.y-p1.y));
	    double len3 = Math.sqrt((p3.x-p2.x) * (p3.x-p2.x) + (p3.y-p2.y) * (p3.y-p2.y));

	    double k1 = len1 / (len1 + len2);
	    double k2 = len2 / (len2 + len3);

	    double xm1 = xc1 + (xc2 - xc1) * k1;
	    double ym1 = yc1 + (yc2 - yc1) * k1;

	    double xm2 = xc2 + (xc3 - xc2) * k2;
	    double ym2 = yc2 + (yc3 - yc2) * k2;

	    // Resulting control points. Here smooth_value is mentioned
	    // above coefficient K whose value should be in range [0...1].
	    Point c0 = new Point();
	    Point c1 = new Point();
	    c0.x = (float)(xm1 + (xc2 - xm1) * SMOOTHING + p1.x - xm1);
	    c0.y = (float)(ym1 + (yc2 - ym1) * SMOOTHING + p1.y - ym1);

	    c1.x = (float)(xm2 + (xc2 - xm2) * SMOOTHING + p2.x - xm2);
	    c1.y = (float)(ym2 + (yc2 - ym2) * SMOOTHING + p2.y - ym2);
		
		cp.add(c0);
		cp.add(c1);
		
		return cp;
	}
	
	// -- Interpolation -----------------------------------------------------------------
	
	private void generateCurrentPath(){	
		if(null != mCrntLine){
			mCrntPath = new Path();
			mCrntPath.moveTo(mCrntLine.points.get(0).x, mCrntLine.points.get(0).y);
			Point c0;
			Point c1;
			Point p0;
			Point p1;
			Point p2;
			Point p3;
			
			int nPts = mCrntLine.points.size();
			int nPreviousCtrlPts = mCrntCtrlPts.size();
			for(int i=0; i<nPreviousCtrlPts; i++){
				c0 = mCrntCtrlPts.get(i).c0;
				c1 = mCrntCtrlPts.get(i).c1;
				p3 = mCrntLine.points.get(((i+1)*4)-1);
				mCrntPath.cubicTo(c0.x, c0.y, c1.x, c1.y, p3.x, p3.y);
			}
			
			int remainingPts = nPts - 4*nPreviousCtrlPts;
			
			if(4 > remainingPts){
				for(int i=4*nPreviousCtrlPts; i<nPts; i++){
					p3 = mCrntLine.points.get(i);
					mCrntPath.lineTo(p3.x, p3.y);
				}
			}else{
				p0 = mCrntLine.points.get(4*nPreviousCtrlPts);
				p1 = mCrntLine.points.get(4*nPreviousCtrlPts+1);
				p2 = mCrntLine.points.get(4*nPreviousCtrlPts+2);
				p3 = mCrntLine.points.get(4*nPreviousCtrlPts+3);
				ArrayList<Point> cp = getControlPointsFrom(p0, p1, p2, p3);
				if(0 < nPreviousCtrlPts){
					c0 = new Point();
					Point prevC1 = mCrntCtrlPts.get(nPreviousCtrlPts-1).c1;
					Point prevLast = mCrntLine.points.get(nPreviousCtrlPts*4-1);
					c0.x = prevLast.x + (prevLast.x - prevC1.x);
					c0.y = prevLast.y + (prevLast.y - prevC1.y);
				}else{
					c0 = cp.get(0);
				}
				c1 = cp.get(1);
				mCrntPath.cubicTo(c0.x, c0.y, c1.x, c1.y, p3.x, p3.y);
				mCrntCtrlPts.add(new ControlPoints(c0, c1));
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
			mCrntCtrlPts = new ArrayList<DrawingCanvas.ControlPoints>();
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
			//if(Math.abs(mPreviousPoint.x - p.x) >= MIN_DISTANCE && Math.abs(mPreviousPoint.y - p.y) >= MIN_DISTANCE){
				generateCurrentPath();
				mCrntPath.lineTo(p.x, p.y);
				mCrntLine.addPoint(p);
				mPreviousPoint = p;
			//}
			mPreviousLines.add(mCrntPath);
			refreshCache();
			mCrntLine = null;
			mCrntPath = null;
			mCrntCtrlPts = null;
			break;
			
		default:
			break;
		}
		
		invalidate();
		return true;
	}
}
