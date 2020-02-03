package simulation;

import com.vividsolutions.jts.geom.Coordinate;

public class Vector2D {

	public double x;
	public double y;
	
	public Vector2D() {
		x = 0.0;
		y = 0.0;
	}
	
	public Vector2D(double x, double y){
		this.x = x;
		this.y = y;
	}
	
	public Vector2D(Coordinate a, Coordinate b){
		this.x = b.x - a.x;
		this.y = b.y - a.y;
	}
	
	public Vector2D normalize(){
		return new Vector2D(this.x/length(), this.y/length());
	}
	
	public double length(){
		return Math.sqrt(Math.pow(x,2) + Math.pow(y,2));
	}	
	
	public double angleInRad(Vector2D vector){
		return Math.atan2(vector.y, vector.x) - Math.atan2(y, x);
	}
	
	//Counter clock wise angle between this vector and a Vector2D vector
	public double angleInDeg(Vector2D vector){
		double angleInRad = Math.atan2(vector.y, vector.x) - Math.atan2(y, x);
		return angleInRad*(180/Math.PI);
	}
	
	@Override
	public String toString() {
		return new String("x: " + x + "\n" + "y: " + y);
	}
}
