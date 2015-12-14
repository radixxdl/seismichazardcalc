package org.opensha.commons.data.function;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.dom4j.Element;
import org.opensha.commons.data.Named;
//import org.opensha.commons.gui.plot.PlotElement;
import org.opensha.commons.metadata.XMLSaveable;

/**
 * A wrapper for 2D double-valued data that provides access to data points via
 * {@link Point2D}s.
 * 
 * <p><i>Note:</i> Use of the word 'Set' in this class does not imply adherence
 * to the {@link Set} interface. An {@code XY_DataSet} may contain multiple
 * identical points, although subclasses are free to provide alternate behavior.
 * </p>
 * 
 * @author Kevin Milner
 * @author Peter Powers
 * @version $Id: XY_DataSet.java 10931 2015-01-27 22:04:06Z kmilner $
 */
public interface XY_DataSet extends /*PlotElement,*/ Named, XMLSaveable, Serializable, Iterable<Point2D> {

	/* ******************************/
	/* Basic Fields Getters/Setters */
	/* ******************************/

	/** Sets the name of this function. */
	public void setName( String name );

	/** Sets the info string of this function. */
	public void setInfo( String info );
	
	/** Returns the info of this function.  */
	public String getInfo();


	/* ******************************/
	/* Metrics about list as whole  */
	/* ******************************/

	/** returns the number of points in this function list */
	public int size();

	/** return the minimum x value along the x-axis. */
	public double getMinX() throws IndexOutOfBoundsException;

	/** return the maximum x value along the x-axis */
	public double getMaxX() throws IndexOutOfBoundsException;

	/** return the minimum y value along the y-axis */
	public double getMinY() throws IndexOutOfBoundsException;

	/** return the maximum y value along the y-axis */
	public double getMaxY() throws IndexOutOfBoundsException;


	/* ******************/
	/* Point Accessors  */
	/* ******************/

	/** Returns the nth (x,y) point in the Function by index, or null if no such point exists */
	public Point2D get(int index);

	/** Returns the x-value given an index */
	public double getX(int index) throws IndexOutOfBoundsException;

	/** Returns the y-value given an index */
	public double getY(int index) throws IndexOutOfBoundsException;
	
	/**
	 * Get the Y value for the point with closest X. If multiple points are equidistant, the smaller
	 * X will be returned.
	 * 
	 * @param x
	 * @return
	 */
	public double getClosestYtoX(double x);

	/**
	 * Get the X value for the point with closest Y. If multiple points are equidistant, the smaller
	 * X will be returned.
	 * 
	 * @param y
	 * @return
	 */
	public double getClosestXtoY(double y);


	/* ***************/
	/* Point Setters */
	/* ***************/

	/** Either adds a new DataPoint, or replaces an existing one, within tolerance */
	public void set(Point2D point);

	/**
	 * Creates a new DataPoint, then either adds it if it doesn't exist,
	 * or replaces an existing one, within tolerance
	 */
	public void set(double x, double y);

	/** Replaces a DataPoint y-value at the specifed index. */
	public void set(int index, double Y) throws IndexOutOfBoundsException;



	/* **********/
	/* Queries  */
	/* **********/
	
	/**
	 * Determine whether a point exists in the list,
	 * as determined by it's x-value within tolerance (if applicable).
	 */
	public boolean hasX(double x);


	/* ************/
	/* Iterators  */
	/* ************/


	/**
	 * Returns an iterator over all x-values in the list. Results returned
	 * in sorted order.
	 * @return
	 */
	public Iterator<Double> getXValuesIterator();


	/**
	 * Returns an iterator over all y-values in the list. Results returned
	 * in sorted order along the x-axis.
	 * @return
	 */
	public Iterator<Double> getYValuesIterator();



	/* **************************/
	/* Standard Java Functions  */
	/* **************************/

	/**
	 * Standard java function, usually used for debugging, prints out
	 * the state of the list, such as number of points, the value of each point, etc.
	 */
	public String toString();

//	/**
//	 * Determines if two lists are equal. Typical implementation would verify
//	 * same number of points, and the all points are equal, using the DataPoint2D
//	 * equals() function.
//	 */
//	public boolean equals( XY_DataSetAPI function );

	/**
	 * prints out the state of the list, such as number of points,
	 * the value of each point, etc.
	 * @return value of each point in the function in String format
	 */
	public String getMetadataString();
	
	/**
	 * This function returns a new copy of this list, including copies
	 * of all the points. A shallow clone would only create a new DiscretizedFunc
	 * instance, but would maintain a reference to the original points. <p>
	 *
	 * Since this is a clone, you can modify it without changing the original.
	 */
	public XY_DataSet deepClone();

	/**
	 * It finds out whether the X values are within tolerance of an integer value
	 * @param tolerance tolerance value to consider  rounding errors
	 *
	 * @return true if all X values are within the tolerance of an integer value
	 * else returns false
	 */
	public boolean areAllXValuesInteger(double tolerance);
	
	/**
	 * Sets the name of the X Axis
	 * @param xName String
	 */
	public void setXAxisName(String xName);
	
	/**
	 * Gets the name of the X Axis
	 */
	public String getXAxisName();
	
	/**
	 * Sets the name of the X Axis
	 * @param xName String
	 */
	public void setYAxisName(String xName);
	
	/**
	 * Gets the name of the Y Axis
	 */
	public String getYAxisName();
	

	public List<Double> xValues();
	public List<Double> yValues();
	
	public Element toXMLMetadata(Element root, String elName);
	
}
