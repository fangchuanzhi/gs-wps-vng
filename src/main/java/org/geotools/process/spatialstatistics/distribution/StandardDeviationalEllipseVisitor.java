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

import com.vividsolutions.jts.geom.Coordinate;

/**
 * StandardDistanceEllipse Visitor
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class StandardDeviationalEllipseVisitor extends AbstractDistributionVisitor {

    @SuppressWarnings("unchecked")
    public void visit(Coordinate coordinate, Object caseVal, double weightVal) {
        caseVal = caseVal == null ? CASE_ALL : caseVal;
        StandardDeviationalEllipse sde = (StandardDeviationalEllipse) resuleMap.get(caseVal);

        if (sde == null) {
            sde = new StandardDeviationalEllipse();
            resuleMap.put(caseVal, sde);
        }

        sde.addValue(coordinate, weightVal);
    }
}
