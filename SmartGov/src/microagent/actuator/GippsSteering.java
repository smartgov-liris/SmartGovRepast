package microagent.actuator;

import simulation.GISComputation;

import com.vividsolutions.jts.geom.Coordinate;

import environment.WorldObject;
import environment.graph.Node;

/**
 * Implementation of Gipps steering behavior explained in the following paper :
 * @see <a href="http://turing.iimas.unam.mx/sos/sites/default/files/Gipps_ABehaviouralCarFollowingModel.pdf">Gipps' Steering Behavior</a>
 * @author Simon Pageaud
 */
public class GippsSteering {
	
	private double a_n; //maximum acceleration which the driver of vehicle n wishes to undertake
	private double b_n; //most severe braking that the driver of vehicle n wishes to undertake (b_n < 0)
	private double teta; //apparent reaction time, a constant for all vehicles
	
	public GippsSteering(double teta, double a_n, double b_n){
		this.a_n = a_n;
		this.teta = teta;
		this.b_n = b_n;
	}
	
	public double getSpeed(double v_n_t, double V_n, WorldObject targetType, double v_n_1_t, double s_n_1, double b_n_1, Coordinate x_n_t){
		Coordinate positionOfTarget = targetType.getPosition();
		if(targetType instanceof Node){
			double speed = getAcceleration(v_n_t, V_n);
			return speed < 0 ? 0 : speed;
		} else{
			double speed = Math.min(getAcceleration(v_n_t, V_n), getBraking(positionOfTarget, v_n_1_t, s_n_1, v_n_t, b_n_1, x_n_t));
			return speed < 0 ? 0 : speed;
		}
	}
	
	/**
	 * Compute acceleration for vehicle n
	 * @param v_n_t speed of vehicle n at time t
	 * @param V_n speed at which the driver of vehicle n wishes to travel
	 * @return 
	 */
	public double getAcceleration(double v_n_t, double V_n){
		return (v_n_t + 2.5*a_n*teta*(1 - v_n_t/V_n)*Math.sqrt(0.025+v_n_t/V_n));
	}
	
	/**
	 * Braking speed
	 * @param x_n_1_t location of the front vehicle n-1 at time t
	 * @param v_n_1_t speed of vehicle n-1 at time t
	 * @param s_n_1 effective size of vehicle n-1
	 * @param v_n_t speed of vehicle n at time t
	 * @param b_n_1 assumption of most severe braking of vehicle n-1
	 * @param x_n_t location of the front of vehicle n at time t
	 * @return braking speed to avoid collision in a smooth manner
	 */
	public double getBraking(Coordinate x_n_1_t, double v_n_1_t, double s_n_1, double v_n_t, double b_n_1, Coordinate x_n_t){
		return b_n*teta + Math.sqrt(Math.pow(b_n, 2)*Math.pow(teta, 2) - b_n*(2*distanceBetweenTwoCars(x_n_t, s_n_1, x_n_1_t) - v_n_t*teta - Math.pow(v_n_1_t,2)/b_n_1));
	}
	
	/**
	 * Compute distance between two cars using both front coordinates and the size of the leader car
	 * @param x_n_t location of the vehicle n at time t
	 * @param s_n_1 effective size of vehicle n-1
	 * @param x_n_1_t location of the vehicle n-1 at time t
	 * @return distance in meters
	 */
	public double distanceBetweenTwoCars(Coordinate x_n_t, double s_n_1, Coordinate x_n_1_t){
		return GISComputation.GPS2Meter(x_n_1_t, x_n_t) - s_n_1;
	}

}
