/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2011, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2001-2007 TOPP - www.openplans.org.
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
package org.geotools.process.feature.gs;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequenceFactory;

/**
 * A Rendering Transformation process which aggregates features into a smaller set of 
 * visually non-conflicting point features.
 * Sometimes called "point clustering".
 * The created points have attributes which provide the total number of points
 * aggregated, as well as the number of location-unique points.
 * The grid cell size is specified in pixels relative to a requested target image size.
 * 
 * 
 * @author mdavis
 *
 */
@DescribeProcess(title = "PointStacker", description = "Aggregates a collection of points into a set of stacked points.")
public class PointStackerProcess implements GSProcess {

    // no process state is defined, since RenderingTransformation processes must be stateless

    @DescribeResult(name = "result", description = "The collection of stacked points")
    public SimpleFeatureCollection execute(

            // process data
            @DescribeParameter(name = "data", description = "Features containing the data points") SimpleFeatureCollection data,

            // process parameters
            @DescribeParameter(name = "cellSize", description = "Cell size for gridding, in pixels") Integer cellSize,

            // output image parameters
            @DescribeParameter(name = "targetBBOX", description = "Georeferenced bounding box of the target image") ReferencedEnvelope targetEnv,
            @DescribeParameter(name = "targetWidth", description = "Width of the target image, in pixels") Integer targetWidth,
            @DescribeParameter(name = "targetHeight", description = "Height of the target image, in pixels") Integer targetHeight,

            ProgressListener monitor) throws ProcessException {

        CoordinateReferenceSystem srcCRS = data.getSchema().getCoordinateReferenceSystem();

        // TODO: assume same CRS for now...
        double cellSizeSrc = cellSize * targetEnv.getWidth() / targetWidth;

        Collection<StackedPoint> stackedPts = stackPoints(data, cellSizeSrc,
                targetEnv.getMinX(), targetEnv.getMinY());

        SimpleFeatureType schema = createType(srcCRS);
        SimpleFeatureCollection result = new ListFeatureCollection(schema);
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(schema);

        GeometryFactory factory = new GeometryFactory(new PackedCoordinateSequenceFactory());

        for (StackedPoint sp : stackedPts) {
            // create feature for stacked point
            Coordinate pt = sp.getLocation();
            Geometry point = factory.createPoint(pt);
            fb.add(point);
            fb.add(sp.getCount());
            fb.add(sp.getCountUnique());
            
            result.add(fb.buildFeature(null));
        }
        return result;
    }

    /**
     * Computes the stacked points for the given data collection.
     * All geometry types are handled - for non-point geometries, the centroid is used.
     * 
     * @param data
     * @param cellSize
     * @param minX
     * @param minY
     * @return
     */
    private Collection<StackedPoint> stackPoints(SimpleFeatureCollection data,
            double cellSize, double minX, double minY) {
        SimpleFeatureIterator featureIt = data.features();

        Map<Coordinate, StackedPoint> stackedPts = new HashMap<Coordinate, StackedPoint>();

        Coordinate indexPt = new Coordinate();
        try {
            while (featureIt.hasNext()) {
                SimpleFeature feature = featureIt.next();
                // get the point location from the geometry
                Geometry geom = (Geometry) feature.getDefaultGeometry();
                Coordinate p = getPoint(geom);
                indexPt.x = p.x;
                indexPt.y = p.y;
                grid(indexPt, cellSize);

                StackedPoint stkPt = stackedPts.get(indexPt);
                if (stkPt == null) {
                    // no longer used now that window origin is not used as base
                    //double centreX = minX + indexPt.x * cellSize + cellSize / 2;
                    //double centreY = minY + indexPt.y * cellSize + cellSize / 2;
                    
                    double centreX = indexPt.x * cellSize + cellSize / 2;
                    double centreY = indexPt.y * cellSize + cellSize / 2;
                    
                    stkPt = new StackedPoint(indexPt, new Coordinate(centreX, centreY));
                    stackedPts.put(stkPt.getKey(), stkPt);
                }
                stkPt.add(p);
            }

        } finally {
            featureIt.close();
        }
        return stackedPts.values();
    }

    /**
     * Gets a point to represent the Geometry.
     * If the Geometry is a point, this is returned.
     * Otherwise, the centroid is used.
     * 
     * @param g the geometry to find a point for
     * @return a point representing the Geometry
     */
    private static Coordinate getPoint(Geometry g)
    {
        if (g.getNumPoints() == 1)
            return g.getCoordinate();
        return g.getCentroid().getCoordinate();
    }
    
    /**
     * Rounds the <code>griddedPoint</code> to the grid determined by the cellsize.
     * 
     * @param griddedPt the point to grid
     * @param cellSize the grid cell size
     */
    private void grid(Coordinate griddedPt, double cellSize) {
        
        // TODO: is there any situation where this could result in too much loss of precision?  
        /**
         * This should not lose too much precision for any reasonable coordinate system and map size.
         * The worst case is a CRS with small ord values, and a large cell size.
         * The worst case tested is a map in degrees, zoomed out to show about twice the globe - works fine.
         */
        // Use longs to avoid possible overflow issues (e.g. for a very small cell size)
        long ix = (long) ((griddedPt.x) / cellSize);
        long iy = (long) ((griddedPt.y) / cellSize);
        
        // by not basing at the window origin, the cells are stable during panning
        //int x = (int) ((griddedPt.x - minX) / cellSize);
        //int y = (int) ((griddedPt.y - minY) / cellSize);
        
        griddedPt.x = ix;
        griddedPt.y = iy;
    }

    private SimpleFeatureType createType(CoordinateReferenceSystem crs) {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.add("geom", Point.class, crs);
        tb.add("count", Integer.class);
        tb.add("countunique", Integer.class);
        tb.setName("stackedPoint");
        SimpleFeatureType sfType = tb.buildFeatureType();
        return sfType;
    }

    private static class StackedPoint {
        private Coordinate key;

        private Coordinate centerPt;

        private Coordinate location = null;

        private int count = 0;

        private Set<Coordinate> uniquePts;

        public StackedPoint(Coordinate key, Coordinate centerPt) {
            this.key = new Coordinate(key);
            this.centerPt = centerPt;
        }

        public Coordinate getKey() {
            return key;
        }

        public Coordinate getLocation() {
            return location;
        }

        public int getCount() {
            return count;
        }

        public int getCountUnique() {
            if (uniquePts == null)
                return 1;
            return uniquePts.size();
        }

        public void add(Coordinate pt) {
            count++;
            /**
             * Only create set if this is the second point seen
             * (and assum the first pt is in location)
             */
            if (uniquePts == null) {
                uniquePts = new HashSet<Coordinate>();
            }
            uniquePts.add(pt);

            pickNearestLocation(pt);
            //pickCenterLocation(pt);
        }

        /**
         * Picks the location as the point
         * which is nearest to the center of the cell.
         * In addition, the nearest location is averaged with the cell center.
         * This gives the best chance of avoiding conflicts.
         * 
         * @param pt
         */
        private void pickNearestLocation(Coordinate pt) {
            // strategy - pick most central point
            if (location == null) {
                location = average(centerPt, pt);
                return;
            }
            if (pt.distance(centerPt) < location.distance(centerPt)) {
                location = average(centerPt, pt);
            }
        }
        
        /**
         * Picks the location as the centre point of the cell.
         * This does not give a good visualization - the gridding is very obvious
         * 
         * @param pt
         */
        private void pickCenterLocation(Coordinate pt) {
            // strategy - pick first point
            if (location == null) {
                location = new Coordinate(pt);
                return;
            }
            location = centerPt;
        }

        /**
         * Picks the first location encountered as the cell location.
         * This is sub-optimal, since if the first point is near the cell
         * boundary it is likely to collide with neighbouring points.
         * 
         * @param pt
         */
        private void pickFirstLocation(Coordinate pt) {
            // strategy - pick first point
            if (location == null) {
                location = new Coordinate(pt);
            }
        }

        private static Coordinate average(Coordinate p1, Coordinate p2)
        {
            double x = (p1.x + p2.x) / 2;
            double y = (p1.y + p2.y) / 2;
            return new Coordinate(x, y);
        }
    }
}
