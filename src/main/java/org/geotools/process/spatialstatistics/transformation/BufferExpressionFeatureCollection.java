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
package org.geotools.process.spatialstatistics.transformation;

import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Buffers a features using a certain distance expression.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class BufferExpressionFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging
            .getLogger(BufferExpressionFeatureCollection.class);

    private static final String BUFFER_FIELD = "buf_dist";

    private Expression distance;

    private int quadrantSegments = 8;

    private SimpleFeatureType schema;

    public BufferExpressionFeatureCollection(SimpleFeatureCollection delegate, double distance,
            int quadrantSegments) {
        this(delegate, ff.literal(distance), quadrantSegments);
    }

    public BufferExpressionFeatureCollection(SimpleFeatureCollection delegate, Expression distance,
            int quadrantSegments) {
        super(delegate);

        if (quadrantSegments <= 0) {
            quadrantSegments = 8;
        }

        this.distance = distance;
        this.quadrantSegments = quadrantSegments;

        String typeName = delegate.getSchema().getTypeName();
        this.schema = FeatureTypes.build(delegate.getSchema(), typeName, Polygon.class);
        this.schema = FeatureTypes.add(schema, BUFFER_FIELD, Double.class, 19);
    }

    @Override
    public SimpleFeatureIterator features() {
        return new BufferExpressionFeatureIterator(delegate.features(), getSchema(), distance,
                quadrantSegments);
    }

    @Override
    public SimpleFeatureType getSchema() {
        return schema;
    }

    @Override
    public ReferencedEnvelope getBounds() {
        return DataUtilities.bounds(features());
    }

    @Override
    public SimpleFeatureCollection subCollection(Filter filter) {
        if (filter == Filter.INCLUDE) {
            return this;
        }
        return new SubFeatureCollection(this, filter);
    }

    static class BufferExpressionFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private Expression distance;

        private int quadrantSegments = 8;

        private int count = 0;

        private SimpleFeatureBuilder builder;

        private SimpleFeature next;

        private String typeName;

        public BufferExpressionFeatureIterator(SimpleFeatureIterator delegate,
                SimpleFeatureType schema, Expression distance, int quadrantSegments) {
            this.delegate = delegate;

            this.distance = distance;
            this.quadrantSegments = quadrantSegments;
            this.builder = new SimpleFeatureBuilder(schema);
            this.typeName = schema.getTypeName();
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            while (next == null && delegate.hasNext()) {
                SimpleFeature source = delegate.next();
                Double eval = distance.evaluate(source, Double.class);
                if (eval != null) {
                    next = builder.buildFeature(buildID(typeName, ++count));

                    // transfer attributes
                    transferAttribute(source, next);

                    // envelope to polygon geometry
                    Geometry geometry = (Geometry) source.getDefaultGeometry();
                    next.setDefaultGeometry(geometry.buffer(eval, quadrantSegments));
                    next.setAttribute(BUFFER_FIELD, eval);

                    builder.reset();
                }
            }
            return next != null;
        }

        public SimpleFeature next() throws NoSuchElementException {
            if (!hasNext()) {
                throw new NoSuchElementException("hasNext() returned false!");
            }
            SimpleFeature result = next;
            next = null;
            return result;
        }
    }
}