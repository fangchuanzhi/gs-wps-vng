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
package org.geotools.process.spatialstatistics.distribution;

import java.util.logging.Logger;

import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.expression.Expression;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Abstract Disribution Operator
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public abstract class AbstractDisributionOperator extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(AbstractDisributionOperator.class);

    protected final String ALL = "ALL";

    protected double getValue(SimpleFeature feature, Expression expression, double defaultValue) {
        if (expression == null) {
            return defaultValue;
        }

        Double dblVal = expression.evaluate(feature, Double.class);
        if (dblVal == null || dblVal.isNaN() || dblVal.isInfinite()) {
            return defaultValue;
        } else {
            return dblVal.doubleValue();
        }
    }

    protected String getCaseValue(SimpleFeature feature, Expression expression) {
        if (expression == null) {
            return ALL;
        }

        String caseValue = expression.evaluate(feature, String.class);
        if (caseValue == null || caseValue.isEmpty()) {
            return ALL;
        } else {
            return caseValue;
        }
    }

    /**
     * The center of gravity for a inputGeometry.
     * 
     * @param inputGeometry
     * @return
     */
    protected Coordinate getTrueCentroid(Geometry inputGeometry) {
        // For line and polygon features, feature centroids are used in distance computations.
        // For multipoints, polylines, or polygons with multiple parts, the centroid is computed
        // using the weighted mean center of all feature parts.
        // The weighting for point features is 1, for line features is length, and for polygon
        // features is area.

        double sumX = 0.0;
        double sumY = 0.0;
        double weightSum = 0.0;

        if (inputGeometry instanceof Point) {
            return inputGeometry.getCentroid().getCoordinate();
        } else if (inputGeometry instanceof LineString) {
            return inputGeometry.getCentroid().getCoordinate();
        } else if (inputGeometry instanceof Polygon) {
            return inputGeometry.getCentroid().getCoordinate();
        } else if (inputGeometry instanceof MultiPoint) {
            MultiPoint mp = (MultiPoint) inputGeometry;
            for (int k = 0; k < mp.getNumGeometries(); k++) {
                Coordinate cen = mp.getGeometryN(k).getCoordinate();

                weightSum += 1;
                sumX += cen.x;
                sumY += cen.y;
            }
            return new Coordinate(sumX / weightSum, sumY / weightSum);
        } else if (inputGeometry instanceof MultiLineString) {
            MultiLineString ml = (MultiLineString) inputGeometry;
            for (int k = 0; k < ml.getNumGeometries(); k++) {
                Geometry lineString = ml.getGeometryN(k);
                Coordinate cen = lineString.getCentroid().getCoordinate();
                final double length = lineString.getLength();

                weightSum += length;
                sumX += cen.x * length;
                sumY += cen.y * length;
            }
            return new Coordinate(sumX / weightSum, sumY / weightSum);
        } else if (inputGeometry instanceof MultiPolygon) {
            MultiPolygon mp = (MultiPolygon) inputGeometry;
            for (int k = 0; k < mp.getNumGeometries(); k++) {
                Geometry polygon = mp.getGeometryN(k);
                Coordinate cen = polygon.getCentroid().getCoordinate();
                final double area = polygon.getArea();

                weightSum += area;
                sumX += cen.x * area;
                sumY += cen.y * area;
            }
            return new Coordinate(sumX / weightSum, sumY / weightSum);
        } else if (inputGeometry instanceof GeometryCollection) {
            GeometryCollection gc = (GeometryCollection) inputGeometry;
            for (int k = 0; k < gc.getNumGeometries(); k++) {
                Coordinate cen = getTrueCentroid(gc.getGeometryN(k));

                weightSum += 1;
                sumX += cen.x;
                sumY += cen.y;
            }
            return new Coordinate(sumX / weightSum, sumY / weightSum);
        }

        return inputGeometry.getCentroid().getCoordinate();
    }

}
