/*******************************************************************************
 * Copyright 2009 OpenSHA.org in partnership with
 * the Southern California Earthquake Center (SCEC, http://www.scec.org)
 * at the University of Southern California and the UnitedStates Geological
 * Survey (USGS; http://www.usgs.gov)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package org.opensha.sha.earthquake.rupForecastImpl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.opensha.commons.data.Site;
import org.opensha.commons.data.function.DiscretizedFunc;
import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.LocationList;
import org.opensha.commons.geo.LocationUtils;
import org.opensha.sha.earthquake.ProbEqkRupture;
import org.opensha.sha.earthquake.ProbEqkSource;
import org.opensha.sha.faultSurface.AbstractEvenlyGriddedSurfaceWithSubsets;
import org.opensha.sha.faultSurface.AbstractEvenlyGriddedSurface;
import org.opensha.sha.faultSurface.CompoundSurface;
import org.opensha.sha.faultSurface.EvenlyGriddedSurface;
import org.opensha.sha.faultSurface.RuptureSurface;
import org.opensha.sha.magdist.IncrementalMagFreqDist;

/**
 * <p>Title: FaultRuptureSource </p>
 * <p>Description: This implements a basic fault source for arbitrary: <p>
 * <UL>
 * <LI>magnitude (or magnitude-frequncy dist.)
 * <LI>ruptureSurface - any EvenlyDiscretizedSurface
 * <LI>rake - that rake (in degrees) assigned to all ruptures.
 * <LI>probability (or duration)
 * </UL><p>
 * If magnitude/probability are given the source is set as non poissonian (and
 * duration is meaningless); If a mag-freq-dist and duration is given then the source
 * is assumed to be Poissonian.  If a mag-freq-dist and prob is given then the source
 * is non Poissonian, and the mag-freq-dist is treated as a PDF (relative prob of different
 * mag/rups, such that the sum of these matches the total given). 
 * The entire surface ruptures for all cases (no floating of events).  Note that duration 
 * is the only constructor argument saved internally in order to conserve memory (this is 
 * why there are no associated get/set methods for anything besides duration).<p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author Ned Field
 * @date Sept, 2003, modified June 2007.
 * @version 1.0
 */

public class FaultRuptureSource
extends ProbEqkSource {

	//for Debug purposes
	private static String C = new String("FaultRuptureSource");
	private boolean D = false;

	//name for this classs
	protected String NAME = "Fault Rupture Source";

	protected double duration;

	private ArrayList<ProbEqkRupture> ruptureList; // keep this in case we add more mags later
	private ArrayList<Location> faultCornerLocations = new ArrayList<Location>(); // used for the getMinDistance(Site) method


	/**
	 * Constructor - this is for a single mag, non-poissonian rupture.
	 * @param magnitude
	 * @param ruptureSurface - any EvenlyGriddedSurface representation of the fault
	 * @param rake - average rake of the ruptures
	 * @param probability - the probability of the source/rupture
	 */
	public FaultRuptureSource(double magnitude, RuptureSurface ruptureSurface,
			double rake, double probability) {
		this(magnitude,ruptureSurface,rake,probability, false);
	}


	/**
	 * Constructor - this is for a single mag source.
	 * @param magnitude
	 * @param ruptureSurface - any EvenlyGriddedSurface representation of the fault
	 * @param rake - average rake of the ruptures
	 * @param probability - the probability of the source/rupture
	 * @param isPoissonian - whether or not it's a poisson source
	 */
	public FaultRuptureSource(double magnitude,
			RuptureSurface ruptureSurface,
			double rake,
			double probability,
			boolean isPoisson) {

		this.isPoissonian = isPoisson;

		if (D) {
			System.out.println("mag: " + magnitude);
			System.out.println("rake: " + rake);
			System.out.println("probability: " + probability);

		}
		// make a list of a subset of locations on the fault for use in the getMinDistance(site) method
		makeFaultCornerLocs(ruptureSurface);

		// make the rupture list
		ruptureList = new ArrayList<ProbEqkRupture>();

		ProbEqkRupture probEqkRupture = new ProbEqkRupture();
		probEqkRupture.setAveRake(rake);
		probEqkRupture.setRuptureSurface(ruptureSurface);
		probEqkRupture.setMag(magnitude);
		probEqkRupture.setProbability(probability);

		ruptureList.add(probEqkRupture);

	}


	/**
	 * Returns the Source Surface.
	 * As all ruptures of this source have the same dimensions, so Source Surface
	 * is similar to Rupture Surface.
	 * @return GriddedSurfaceAPI
	 */
	public RuptureSurface getSourceSurface() {
		return ( (ProbEqkRupture) ruptureList.get(0)).getRuptureSurface();
	}

	/**
	 * It returns a list of all the locations which make up the surface for this
	 * source.
	 *
	 * @return LocationList - List of all the locations which constitute the surface
	 * of this source
	 */
	public LocationList getAllSourceLocs() {
		LocationList locList = new LocationList();
		Iterator it = ( (AbstractEvenlyGriddedSurfaceWithSubsets) getSourceSurface()).
		getAllByRowsIterator();
		while (it.hasNext()) locList.add( (Location) it.next());
		return locList;
	}

	/**
	 * Constructor - this produces a separate rupture for each mag in the mag-freq-dist.
	 * This source is set as Poissonian.
	 * @param magnitude-frequency distribution
	 * @param ruptureSurface - any EvenlyGriddedSurface representation of the fault
	 * @param rake - average rake of the ruptures
	 * @param duration - the duration in years
	 */
	public FaultRuptureSource(DiscretizedFunc magDist, RuptureSurface  ruptureSurface,
			double rake, double duration) {

		this.isPoissonian = true;
		this.duration = duration;

		if (D) {
			System.out.println("rake: " + rake);
			System.out.println("duration: " + duration);
		}

		// make a list of a subset of locations on the fault for use in the getMinDistance(site) method
		makeFaultCornerLocs(ruptureSurface);

		// make the rupture list
		ruptureList = new ArrayList<ProbEqkRupture>();
		double mag;
		double prob;

		// Make the ruptures
		for (int i = 0; i < magDist.size(); ++i) {
			mag = magDist.getX(i);
			// make sure it has a non-zero rate
			if (magDist.getY(i) > 0) {
				prob = 1 - Math.exp( -duration * magDist.getY(i));
				ProbEqkRupture probEqkRupture = new ProbEqkRupture();
				probEqkRupture.setAveRake(rake);
				probEqkRupture.setRuptureSurface(ruptureSurface);
				probEqkRupture.setMag(mag);
				probEqkRupture.setProbability(prob);
				ruptureList.add(probEqkRupture);
			}
		}

	}


	/**
	 * Constructor - this treats the input "magDist" as a PDF (not absolute rates), and assigns 
	 * a probability to each rupture (one for each mag magnitude) such that the total probabiulity 
	 * is that given (as "prob").
	 * This source is set as non Poissonian.
	 * @param prob - total probability of an event
	 * @param magDist - magnitude-frequency distribution
	 * @param ruptureSurface - any EvenlyGriddedSurface representation of the fault
	 * @param rake - average rake of the ruptures
	 */
	public FaultRuptureSource(double prob, IncrementalMagFreqDist magDist,
			RuptureSurface ruptureSurface,
			double rake) {

		this.isPoissonian = false;

		// make a list of a subset of locations on the fault for use in the getMinDistance(site) method
		makeFaultCornerLocs(ruptureSurface);

		// make the rupture list
		ruptureList = new ArrayList<ProbEqkRupture>();
		double mag;

		// compute total rate of magDist
		double totRate = 0, qkRate, qkProb;
		for (int i = 0; i < magDist.size(); ++i)
			totRate += magDist.getY(i);

		// Make the ruptures
		for (int i = 0; i < magDist.size(); ++i) {
			mag = magDist.getX(i);
			// make sure it has a non-zero rate
			if ((qkRate = magDist.getY(i)) > 0) {
				qkProb = qkRate*prob/totRate;
				ProbEqkRupture probEqkRupture = new ProbEqkRupture();
				probEqkRupture.setAveRake(rake);
				probEqkRupture.setRuptureSurface(ruptureSurface);
				probEqkRupture.setMag(mag);
				probEqkRupture.setProbability(qkProb);
				ruptureList.add(probEqkRupture);
			}
		}

		if (D) {
			double totProb = 0;
			for(int i =0; i< ruptureList.size(); i++)
				totProb += this.getRupture(i).getProbability();
			System.out.println("input prob="+prob+", final tot prob="+totProb+", ratio="+(prob/totProb));
		}
	}




	/**
	 * This changes the duration for the case where a mag-freq dist was given in
	 * the constructor (for the Poisson) case.
	 * @param newDuration
	 */
	public void setDuration(double newDuration) {
		if (this.isPoissonian != true)
			throw new RuntimeException(C +
					" Error - the setDuration method can only be used for the Poisson case");
		ProbEqkRupture eqkRup;
		double oldProb, newProb;
		for (int i = 0; i < ruptureList.size(); i++) {
			eqkRup = (ProbEqkRupture) ruptureList.get(i);
			oldProb = eqkRup.getProbability();
			newProb = 1.0 - Math.pow( (1.0 - oldProb), newDuration / duration);
			eqkRup.setProbability(newProb);
		}
		duration = newDuration;
	}

	/**
	 * @return the total num of rutures for all magnitudes
	 */
	public int getNumRuptures() {
		return ruptureList.size();
	}

	/**
	 * This method returns the nth Rupture in the list
	 */
	public ProbEqkRupture getRupture(int nthRupture) {
		return (ProbEqkRupture) ruptureList.get(nthRupture);
	}

	/**
	 * This returns the shortest dist to either end of the fault trace, or to the
	 * mid point of the fault trace (done also for the bottom edge of the fault).
	 * @param site
	 * @return minimum distance in km
	 */
	public double getMinDistance(Site site) {

		double min = Double.MAX_VALUE;
		double tempMin;

		Iterator it = faultCornerLocations.iterator();

		while (it.hasNext()) {
			tempMin = LocationUtils.horzDistance(site.getLocation(),
					(Location) it.next());
			if (tempMin < min) min = tempMin;
		}
		//System.out.println(C+" minDist for source "+this.NAME+" = "+min);
		return min;
	}

	/**
	 * This makes the vector of fault corner location used by the getMinDistance(site)
	 * method.
	 * @param faultSurface
	 */
	private void makeFaultCornerLocs(RuptureSurface faultSurface) {
		if(faultSurface instanceof AbstractEvenlyGriddedSurface) {
			AbstractEvenlyGriddedSurface griddedSurf = (AbstractEvenlyGriddedSurface) faultSurface;
			int nRows = griddedSurf.getNumRows();
			int nCols = griddedSurf.getNumCols();
			faultCornerLocations.add(griddedSurf.get(0, 0));
			faultCornerLocations.add(griddedSurf.get(0, (int) (nCols / 2)));
			faultCornerLocations.add(griddedSurf.get(0, nCols - 1));
			faultCornerLocations.add(griddedSurf.get(nRows - 1, 0));
			faultCornerLocations.add(griddedSurf.get(nRows - 1, (int) (nCols / 2)));
			faultCornerLocations.add(griddedSurf.get(nRows - 1, nCols - 1));
		}
		else if (faultSurface instanceof CompoundSurface) {
			List<? extends RuptureSurface> surfs = ((CompoundSurface)faultSurface).getSurfaceList();
			boolean gridded = surfs.get(0) instanceof EvenlyGriddedSurface;
			for (int i=0; i<surfs.size(); i++) {
				RuptureSurface surf = surfs.get(i);
				if (gridded) {
					// here we assume each surface is small enough to just take the top and bottom of 
					//  first columns, plus the top and bottom of the last column of the last surface
					EvenlyGriddedSurface griddedSurf = (EvenlyGriddedSurface)surf;
					faultCornerLocations.add(griddedSurf.get(0, 0));
					faultCornerLocations.add(griddedSurf.get(griddedSurf.getNumRows() - 1, 0));
					
					if (i == surfs.size()-1) {
						// add last edge
						int lastCol = griddedSurf.getNumCols() - 1;
						faultCornerLocations.add(griddedSurf.get(0, lastCol));
						faultCornerLocations.add(griddedSurf.get(griddedSurf.getNumRows() - 1, lastCol));
					}
				} else {
					faultCornerLocations.addAll(surf.getPerimeter());
				}
			}
		}
		else
			faultCornerLocations = faultSurface.getPerimeter();

	}

	/**
	 * set the name of this class
	 *
	 * @return
	 */
	public void setName(String name) {
		NAME = name;
	}

	/**
	 * get the name of this class
	 *
	 * @return
	 */
	public String getName() {
		return NAME;
	}

	/**
	 * This method scales (multiplies) the rate of each rupture by the given value.
	 */
	public void scaleRupRates(double value) {
		for(ProbEqkRupture rup: ruptureList) {
			double newMinusRT = value*Math.log(1.0-rup.getProbability());
			rup.setProbability(1.0-Math.exp(newMinusRT));
		}
	}

	/**
	 * This method scales (multiplies) the probability of each rupture by the given value.
	 */
	public void scaleRupProbs(double value) {
		for(ProbEqkRupture rup: ruptureList) {
			double newRupProb = value*rup.getProbability();
			if(newRupProb<=1.0)
				rup.setProbability(newRupProb);
			else
				throw new RuntimeException("Problem: value causes probability to exceed 1.0");
		}
	}

}
