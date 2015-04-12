package de.mrunde.bachelorthesis.basics;

/**
 * Categories of street furniture (e.g. stop sign)<br/>
 * Roundabouts are already covered by the MapQuest Directions service so there
 * is no need for a special category.
 * 
 * @author Marius Runde
 */
public abstract class StreetFurnitureCategory {

	/**
	 * Bus station
	 */
	public final static String BUS_STATION = "bus_station";

	/**
	 * Railway bridge
	 */
	public final static String RAILWAY_BRIDGE = "railway_bridge";

	/**
	 * Traffic light
	 */
	public final static String TRAFFIC_LIGHT = "traffic_light";

	/**
	 * Check if a category is a valid street furniture category
	 * 
	 * @param category
	 *            Category to be controlled
	 * @return TRUE: <code>category</code> is valid<br/>
	 *         FALSE: <code>category</code> is not valid
	 */
	public static boolean isCategory(String category) {
		if (category.equals(BUS_STATION))
			return true;
		if (category.equals(RAILWAY_BRIDGE))
			return true;
		if (category.equals(TRAFFIC_LIGHT))
			return true;
		return false;
	}

	/**
	 * Get all street furniture categories
	 * 
	 * @return All categories
	 */
	public static String[] getCategories() {
		String[] categories = { BUS_STATION, RAILWAY_BRIDGE, TRAFFIC_LIGHT };
		return categories;
	}
}
