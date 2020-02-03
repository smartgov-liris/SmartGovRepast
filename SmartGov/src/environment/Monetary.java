package environment;

/**
 * Implementing this interface allows an object to be a paying object.
 * It displays the current price and can update the current price.
 * @author Simon Pageaud
 *
 */
public interface Monetary {
	
	double getPrice();
	
	/**
	 * Specify the behavior of a price variation of a world object.
	 * @param priceToAdd
	 */
	void updatePrice(double priceToAdd);
	
}
