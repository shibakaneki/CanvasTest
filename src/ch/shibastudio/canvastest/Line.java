package ch.shibastudio.canvastest;

import java.util.ArrayList;

public class Line {
	public ArrayList<Point> points;
	
	public Line(){
		points = new ArrayList<Point>();
	}
	
	public void addPoint(Point p){
		points.add(p);
	}
}
