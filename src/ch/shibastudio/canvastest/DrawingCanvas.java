package ch.shibastudio.canvastest;

import java.util.ArrayList;

import android.content.Context;
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
	
	private Paint mPaint;
	private ArrayList<Line> mLines;
	private Line mCrntLine;
	
	public DrawingCanvas(Context c){
		super(c);
		mLines = new ArrayList<Line>();
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
		
		//drawFreeHand(c);
		drawTest(c);
	}
	
	// -- Interpolation -----------------------------------------------------------------
	/*
	 * static class DrawingUtility
{
    // linear equation solver utility for ai + bj = c and di + ej = f
    static void solvexy(double a, double b, double c, double d, double e, double f, out double i, out double j)
    {
        j = (c - a / d * f) / (b - a * e / d);
        i = (c - (b * j)) / a;
    }

    static void bez4pts1(double x0, double y0, double x4, double y4, double x5, double y5, double x3, double y3, out double x1, out double y1, out double x2, out double y2)
    {
        // find chord lengths
        double c1 = Math.Sqrt((x4 - x0) * (x4 - x0) + (y4 - y0) * (y4 - y0));
        double c2 = Math.Sqrt((x5 - x4) * (x5 - x4) + (y5 - y4) * (y5 - y4));
        double c3 = Math.Sqrt((x3 - x5) * (x3 - x5) + (y3 - y5) * (y3 - y5));
        // guess "best" t
        double t1 = c1 / (c1 + c2 + c3);
        double t2 = (c1 + c2) / (c1 + c2 + c3);
        // transform x1 and x2
        solvexy(b1(t1), b2(t1), x4 - (x0 * b0(t1)) - (x3 * b3(t1)), b1(t2), b2(t2), x5 - (x0 * b0(t2)) - (x3 * b3(t2)), out x1, out x2);
        // transform y1 and y2
        solvexy(b1(t1), b2(t1), y4 - (y0 * b0(t1)) - (y3 * b3(t1)), b1(t2), b2(t2), y5 - (y0 * b0(t2)) - (y3 * b3(t2)), out y1, out y2);
    }

    static public PathFigure BezierFromIntersection(Point startPt, Point int1, Point int2, Point endPt)
    {
        double x1, y1, x2, y2;
        bez4pts1(startPt.X, startPt.Y, int1.X, int1.Y, int2.X, int2.Y, endPt.X, endPt.Y, out x1, out y1, out x2, out y2);
        PathFigure p = new PathFigure { StartPoint = startPt };
        p.Segments.Add(new BezierSegment { Point1 = new Point(x1, y1), Point2 = new Point(x2, y2), Point3 = endPt } );
        return p;
    }
}
	 * */
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
	
	
	public void drawTest(Canvas c){
		ArrayList<Point> pts = new ArrayList<Point>();
		pts.add(new Point(100, 100));
		pts.add(new Point(320, 230));
		pts.add(new Point(400, 520));
		pts.add(new Point(630, 410));
		
		mPaint.setColor(Color.RED);
		for(int i=0; i<pts.size(); i++){
			Point p = pts.get(i);
			c.drawCircle(p.x, p.y, 5f, mPaint);
		}
		mPaint.setColor(Color.BLACK);
		mPaint.setStrokeWidth(3f);
		
		ArrayList<Point> cp = getControlPointsFrom(pts.get(0), pts.get(1), pts.get(2), pts.get(3));
		Path p = new Path();
		p.moveTo(pts.get(0).x, pts.get(0).y);
		p.cubicTo(cp.get(0).x, cp.get(0).y, cp.get(1).x, cp.get(1).y, pts.get(3).x, pts.get(3).y);
		c.drawPath(p, mPaint);
		
	}
	
	private void drawFreeHand(Canvas c){
		if(!mLines.isEmpty()){
			for(int i=0; i<mLines.size(); i++){
				Line l = mLines.get(i);
				Path p = new Path();
				p.moveTo(l.points.get(0).x, l.points.get(0).y);
				for(int j=1; j<l.points.size(); j++){
					Point p1 = l.points.get(j);
					p.lineTo(p1.x, p1.y);
				}
				//p.close();
				c.drawPath(p, mPaint);
			}
		}
		
		if(null != mCrntLine){
			Path p = new Path();
			p.moveTo(mCrntLine.points.get(0).x, mCrntLine.points.get(0).y);
			for(int k=1; k<mCrntLine.points.size(); k++){
				Point p1 = mCrntLine.points.get(k);

				p.lineTo(p1.x, p1.y);
			}
			//p.close();
			c.drawPath(p, mPaint);
		}	
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent ev){
		Point p;
		switch(ev.getAction()){
		case MotionEvent.ACTION_DOWN:
			mCrntLine = new Line();
			p = new Point(ev.getX(), ev.getY());
			mCrntLine.addPoint(p);
			break;
				
		case MotionEvent.ACTION_MOVE:
			p = new Point(ev.getX(), ev.getY());
			mCrntLine.addPoint(p);
			break;
			
		case MotionEvent.ACTION_UP:
			p = new Point(ev.getX(), ev.getY());
			mCrntLine.addPoint(p);
			mLines.add(mCrntLine);
			mCrntLine = null;
			break;
			
		default:
			break;
		}
		
		invalidate();
		return true;
	}
}
