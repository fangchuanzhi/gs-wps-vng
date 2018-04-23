/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2014, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.process.spatialstatistics.gridcoverage;

import java.util.logging.Logger;

import org.geotools.util.logging.Logging;

/**
 * The Radius class defines which of the input points will be used to interpolate the value for each
 * cell in the output raster. The Variable type is the default.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public final class RasterRadius {
    protected static final Logger LOGGER = Logging.getLogger(RasterRadius.class);

    /**
     * There are two types of radius classes: RadiusVariable and RadiusFixed.
     * 
     */
    public enum SearchRadiusType {
        /**
         * The Fixed type uses a specified fixed distance within which all input points will be used
         * for the interpolation.
         */
        Fixed,

        /**
         * A Variable search radius is used to find a specified number of input sample points for
         * the interpolation.
         */
        Variable
    }

    SearchRadiusType radiusType = SearchRadiusType.Variable;

    public SearchRadiusType getRadiusType() {
        return radiusType;
    }

    // the default radius is five times the cell size of the output raster
    public double distance = Double.NaN;

    // the default is 12 points
    public int numberOfPoints = 12;

    public RasterRadius() {
        // default environment
        setVariable(12);
    }

    public void setVariable(int numberOfPoints) {
        // The default maximumDistance value is the length of the extent's diagonal.
        setVariable(numberOfPoints, Double.MAX_VALUE);
    }

    public void setVariable(int numberOfPoints, double maximumDistance) {
        // The default numberOfPoints is 12 points.
        this.numberOfPoints = numberOfPoints;
        this.distance = maximumDistance;
        this.radiusType = SearchRadiusType.Variable;
    }

    public void setFixed(double distance) {
        setFixed(distance, 0);
    }

    public void setFixed(double distance, int minimum_number_of_points) {
        // The default radius is five times the cell size of the output raster.
        // The default minimum_number_of_points is zero
        this.numberOfPoints = minimum_number_of_points;
        this.distance = distance;
        this.radiusType = SearchRadiusType.Fixed;
    }
}
