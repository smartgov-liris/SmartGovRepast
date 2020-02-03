package microagent;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * AbstractMover decribes the abstract behavior to move on the given class.
 * @author Simon Pageaud
 *
 */
public abstract class AbstractMover<T> {

	public abstract Coordinate moveOn(T objectToMoveOn, Coordinate currentPosition, Coordinate destination, double currentSpeed, AbstractMicroAgentBody agentBody);
	
	public abstract Coordinate moveOn(T objectToMoveOn, Coordinate currentPosition, double remainingDistance, double currentSpeed, AbstractMicroAgentBody agentBody);
	
}
