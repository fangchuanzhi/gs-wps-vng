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
package org.geotools.process.spatialstatistics.operations;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.process.spatialstatistics.transformation.ReprojectFeatureCollection;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.strtree.ItemBoundable;
import com.vividsolutions.jts.index.strtree.ItemDistance;
import com.vividsolutions.jts.index.strtree.STRtree;

/**
 * Calculates count between the input features and the closest feature in another features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 * 
 */
public class NearestNeighborCountOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(NearestNeighborCountOperation.class);

    protected static final String COUNT_FIELD = "count";

    protected double searchRadius = Double.MAX_VALUE;

    public void setMaximumDistance(double searchRadius) {
        if (Double.isNaN(searchRadius) || Double.isInfinite(searchRadius) || searchRadius <= 0) {
            this.searchRadius = Double.MAX_VALUE;
        } else {
            this.searchRadius = searchRadius;
        }
    }

    public double getSearchRadius() {
        return searchRadius;
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures,
            String countField, SimpleFeatureCollection nearFeatures, double searchRadius)
            throws IOException {
        this.setMaximumDistance(searchRadius);
        return execute(inputFeatures, countField, nearFeatures);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures,
            String countField, SimpleFeatureCollection nearFeatures) throws IOException {
        if (countField == null || countField.isEmpty()) {
            countField = COUNT_FIELD;
        }

        // check coordinate reference system
        CoordinateReferenceSystem crsT = inputFeatures.getSchema().getCoordinateReferenceSystem();
        CoordinateReferenceSystem crsS = nearFeatures.getSchema().getCoordinateReferenceSystem();
        if (crsT != null && crsS != null && !CRS.equalsIgnoreMetadata(crsT, crsS)) {
            nearFeatures = new ReprojectFeatureCollection(nearFeatures, crsS, crsT, true);
            LOGGER.log(Level.WARNING, "reprojecting features");
        }

        // 1. pre calculate
        Map<String, Integer> map = calculateNearest(inputFeatures, nearFeatures);

        // 2. write features
        SimpleFeatureType featureType = null;
        featureType = FeatureTypes.add(inputFeatures.getSchema(), countField, Integer.class, 6);

        IFeatureInserter featureWriter = getFeatureWriter(featureType);
        SimpleFeatureIterator featureIter = inputFeatures.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();

                // create & insert feature
                SimpleFeature newFeature = featureWriter.buildFeature();
                featureWriter.copyAttributes(feature, newFeature, true);

                Integer count = map.get(feature.getID());
                if (count == null) {
                    count = Integer.valueOf(0);
                }

                newFeature.setAttribute(countField, count);
                featureWriter.write(newFeature);
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(featureIter);
        }

        return featureWriter.getFeatureCollection();
    }

    private Map<String, Integer> calculateNearest(SimpleFeatureCollection inputFeatures,
            SimpleFeatureCollection nearFeatures) {
        Map<String, Integer> map = new HashMap<String, Integer>();
        STRtree spatialIndex = loadNearFeatures(inputFeatures);

        SimpleFeatureIterator nearIter = nearFeatures.features();
        try {
            while (nearIter.hasNext()) {
                SimpleFeature feature = nearIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();

                // find nearest feature
                NearFeature nearest = (NearFeature) spatialIndex.nearestNeighbour(
                        geometry.getEnvelopeInternal(), new NearFeature(geometry, feature.getID()),
                        new ItemDistance() {
                            @Override
                            public double distance(ItemBoundable item1, ItemBoundable item2) {
                                NearFeature s1 = (NearFeature) item1.getItem();
                                NearFeature s2 = (NearFeature) item2.getItem();
                                return s1.location.distance(s2.location);
                            }
                        });

                double nearestDistance = geometry.distance(nearest.location);
                if (nearestDistance > searchRadius) {
                    continue;
                }

                // update count
                Integer count = map.get(nearest.id);
                if (count == null) {
                    map.put(nearest.id, Integer.valueOf(1));
                } else {
                    map.put(nearest.id, Integer.valueOf(count + 1));
                }
            }
        } finally {
            nearIter.close();
        }
        return map;
    }

    private STRtree loadNearFeatures(SimpleFeatureCollection features) {
        STRtree spatialIndex = new STRtree();
        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                NearFeature nearFeature = new NearFeature(geometry, feature.getID());
                spatialIndex.insert(geometry.getEnvelopeInternal(), nearFeature);
            }
        } finally {
            featureIter.close();
        }
        return spatialIndex;
    }

    static final class NearFeature {

        public Geometry location;

        public String id;

        public NearFeature(Geometry location, String id) {
            this.location = location;
            this.id = id;
        }
    }
}