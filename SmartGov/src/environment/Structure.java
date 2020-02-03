package environment;

import java.util.List;

import policyagent.Position;

/**
 * A structure is a specific world object use to describe important elements of the environment.
 * @author Simon Pageaud
 *
 */
public interface Structure {
	
	public static final String REWARD = "reward";
	public static final String GAIN   =   "gain";

	public String getID();
	
	public String getClassName();
	
	public Position getLocalPerformances(List<String> labels);
	
}
