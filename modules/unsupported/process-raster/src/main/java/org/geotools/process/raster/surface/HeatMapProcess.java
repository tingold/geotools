/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geotools.process.raster.surface;

import java.util.ArrayList;
import java.util.List;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.Hints;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateArrays;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * A Process that uses a {@link HeatMap} to compute a heat map surface 
 * over a set of irregular data points as a {@link GridCoverage}.
 * <p>
 * To improve performance, the surface grid can be computed at a lower resolution than the requested output image.
 * The grid is upsampled to match the required image size.  
 * Upsampling uses Bilinear Interpolation to maintain visual quality.
 * This gives a large improvement in performance, with minimal impact 
 * on visual quality for small cell sizes (for instance, 10 pixels or less).
 * 
 * To ensure that the computed surface is stable 
 * (i.e. does not display obvious edge artifacts under zooming and panning), 
 * the data extent should be expanded to be larger than the specified output extent.
 * The expansion distance is equal to the size of <code>radiusPixels</code> in the input CRS.
 * 
 * <h3>Parameters</h3>
 * <i>M = mandatory, O = optional</i>
 * <p>
 * <ul>
 * <li><b>data</b> (M) - the FeatureCollection containing the point observations
 * <li><b>radiusPixels</b> (M)- the density kernel radius, in pixels
 * <li><b>weightAttr</b> (M)- the feature type attribute containing the observed surface value
 * <li><b>pixelsPerCell</b> (O) - The pixels-per-cell value determines the resolution of the computed grid. 
 * Larger values improve performance, but degrade appearance. (Default = 1)
 * <li><b>outputBBOX</b> (M) - The georeferenced bounding box of the output area
 * <li><b>outputWidth</b> (M) - The width of the output raster
 * <li><b>outputHeight</b> (M) - The height of the output raster
 * </ul>
 * The output of the process is a {@linkplain GridCoverage2D} with a single band, 
 * with cell values in the range [0, 1].
 * <p>
 * Computation of the surface takes places in the CRS of the output.
 * If the data CRS is geodetic and the output CRS is planar, or vice-versa,
 * the input points are transformed into the output CRS.
 * 
 * <h3>Using the process as a Rendering Transformation</h3>
 * 
 * This process can be used as a RenderingTransformation, since it
 * implements the <tt>invertQuery(... Query, GridGeometry)</tt> method.
 * In this case the <code>queryBuffer</code> parameter should be specified to expand 
 * the query extent appropriately.
 * The output raster parameters may be provided from the request extents, using the 
 * following SLD environment variables:
 * <ul>
 * <li><b>outputBBOX</b> - env var = <tt>wms_bbox</tt>
 * <li><b>outputWidth</b> - env var = <tt>wms_width</tt>
 * <li><b>outputHeight</b> - env var = <tt>wms_height</tt>
 * </ul>
 * When used as an Rendering Transformation the data query is rewritten to expand the query BBOX,
 * to ensure that enough data points are queried to make the 
 * computed surface stable under panning and zooming.  
 * 
 * <p>
 * @author Martin Davis - OpenGeo
 *
 */
@DescribeProcess(title = "HeatMap", description = "Computes a heat map surface over a set of irregular data points as a GridCoverage.")
public class HeatMapProcess implements GSProcess {

    // no process state is defined, since RenderingTransformation processes must be stateless
    
    @DescribeResult(name = "result", description = "The heat map surface as a raster")
    public GridCoverage2D execute(
            
            // process data
            @DescribeParameter(name = "data", description = "Features containing the data points") SimpleFeatureCollection obsFeatures,
            
            // process parameters
            @DescribeParameter(name = "radius", description = "Radius to use for the kernel, in pixels") Integer argRadius,
            @DescribeParameter(name = "weightAttr", description = "Featuretype attribute containing the point weight value", min=0, max=1) String valueAttr,
            @DescribeParameter(name = "pixelsPerCell", description = "Number of pixels per grid cell (default = 1)", min=0, max=1) Integer argPixelsPerCell,
            
            // output image parameters
            @DescribeParameter(name = "outputBBOX", description = "Georeferenced bounding box of the output") ReferencedEnvelope argOutputEnv,
            @DescribeParameter(name = "outputWidth", description = "Width of the output raster") Integer argOutputWidth,
            @DescribeParameter(name = "outputHeight", description = "Height of the output raster") Integer argOutputHeight,
            
            ProgressListener monitor) throws ProcessException {


        /**---------------------------------------------
         * Set up required information from process arguments.
         * ---------------------------------------------
         */
        int pixelsPerCell = 1;
        if (argPixelsPerCell != null && argPixelsPerCell > 1) {
            pixelsPerCell = argPixelsPerCell;
        }
        int outputWidth = argOutputWidth;
        int outputHeight = argOutputHeight;
        int gridWidth = outputWidth;
        int gridHeight = outputHeight;
        if (pixelsPerCell > 1) {
            gridWidth = outputWidth / pixelsPerCell;
            gridHeight = outputHeight / pixelsPerCell;
        }
        
        CoordinateReferenceSystem srcCRS = obsFeatures.getSchema().getCoordinateReferenceSystem();
        CoordinateReferenceSystem dstCRS = argOutputEnv.getCoordinateReferenceSystem();
        MathTransform trans = null;
        try {
            trans = CRS.findMathTransform(srcCRS, dstCRS);
        } catch (FactoryException e) {
            throw new ProcessException(e);
        }
        
        /*
         // not used for now - only pixel radius values are supported
        double distanceConversionFactor = distanceConversionFactor(srcCRS, dstCRS);
        double dstRadius = argRadius * distanceConversionFactor;
         */
        int radius = 10;
        if (argRadius != null) radius = argRadius;
        
        HeatMap heatMap = new HeatMap(radius, argOutputEnv, gridWidth, gridHeight);
        
        /**---------------------------------------------
         * Extract the input observation points
         * ---------------------------------------------
         */
        try {
            extractPoints(obsFeatures, valueAttr, trans, heatMap);
        } catch (CQLException e) {
            throw new ProcessException(e);
        }

        /**---------------------------------------------
         * Do the processing
         * ---------------------------------------------
         */
        //Stopwatch sw = new Stopwatch();
        // compute the heatmap at the specified resolution
        float[][] heatMapGrid = heatMap.computeSurface();
        
        // flip now, since grid size may be smaller
        heatMapGrid = flipXY(heatMapGrid);
        
        // upsample to output resolution if necessary
        float[][] outGrid = heatMapGrid;
        if (pixelsPerCell > 1)
            outGrid = upsample(heatMapGrid, -999, outputWidth, outputHeight);
        
        // convert to the GridCoverage2D required for output
        GridCoverageFactory gcf = CoverageFactoryFinder.getGridCoverageFactory(null);
        GridCoverage2D gridCov = gcf.create("Process Results", outGrid, argOutputEnv);
        
        //System.out.println("**************  Heatmap computed in " + sw.getTimeString());
        
        return gridCov;
    }    

    /*
     * An approximate value for the length of a degree at the equator in meters.
     * This doesn't have to be precise, since it is only used to convert
     * values which are themselves rough approximations.
     */
    private static final double METRES_PER_DEGREE = 111320;
    
    private static double distanceConversionFactor(CoordinateReferenceSystem srcCRS,CoordinateReferenceSystem dstCRS)
    {
        Unit<?> srcUnit = srcCRS.getCoordinateSystem().getAxis(0).getUnit();
        Unit<?> dstUnit = dstCRS.getCoordinateSystem().getAxis(0).getUnit();
        if (srcUnit == dstUnit) {
            return 1;
        }
        else if (srcUnit == NonSI.DEGREE_ANGLE && dstUnit == SI.METER) {
            return METRES_PER_DEGREE;
        }
        else if (srcUnit == SI.METER && dstUnit == NonSI.DEGREE_ANGLE) {
            return 1.0 / METRES_PER_DEGREE;
        }
        throw new IllegalStateException("Unable to convert distances from " + srcUnit + " to " + dstUnit);
    }
    
    /**
     * Flips an XY matrix along the X=Y axis, and inverts the Y axis.
     * Used to convert from "map orientation" into the "image orientation"
     * used by GridCoverageFactory.
     * The surface interpolation is done on an XY grid, with Y=0 being the bottom of the space.
     * GridCoverages are stored in an image format, in a YX grid with 0 being the top.
     * 
     * @param grid the grid to flip
     * @return the flipped grid
     */
    private float[][] flipXY(float[][] grid)
    {
        int xsize = grid.length;
        int ysize = grid[0].length;

        float[][] grid2 = new float[ysize][xsize];
        for (int ix = 0; ix < xsize; ix++) {
            for (int iy = 0; iy < ysize; iy++) {
                int iy2 = ysize - iy - 1;
                grid2[iy2][ix] = grid[ix][iy];
            }
        }
        return grid2;
    }
    
    private float[][] upsample(float[][] grid,
            float noDataValue, 
            int width,
            int height) {
        BilinearInterpolator bi = new BilinearInterpolator(grid, noDataValue);
        float[][] outGrid = bi.interpolate(width, height, false);
        return outGrid;
    }

    /**
     * Given a target query and a target grid geometry 
     * returns the query to be used to read the input data of the process involved in rendering. In
     * this process this method is used to:
     * <ul>
     * <li>determine the extent & CRS of the output grid
     * <li>expand the query envelope to ensure stable surface generation
     * <li>modify the query hints to ensure point features are returned
     * </ul>
     * Note that in order to pass validation, all parameters named here must also appear 
     * in the parameter list of the <tt>execute</tt> method,
     * even if they are not used there.
     * 
     * @param argRadiusPixels the feature type attribute that contains the observed surface value
     * @param targetQuery the query used against the data source
     * @param targetGridGeometry the grid geometry of the destination image
     * @return The transformed query
     */
    public Query invertQuery(
            @DescribeParameter(name = "radiusPixels", description = "Radius to use for the kernel", min=0, max=1) Integer argRadiusPixels,
            Query targetQuery, GridGeometry targetGridGeometry)
            throws ProcessException {
        
        // default is no expansion
        double queryBuffer = 0;
        /*
        if (argQueryBuffer != null) {
            queryBuffer = argQueryBuffer;
        }
*/
        targetQuery.setFilter(expandBBox(targetQuery.getFilter(), queryBuffer));
        
        // clear properties to force all attributes to be read
        // (required because the SLD processor cannot see the value attribute specified in the transformation)
        // TODO: set the properties to read only the specified value attribute
        targetQuery.setProperties(null);
        
        // set the decimation hint to ensure points are read
        Hints hints = targetQuery.getHints();
        hints.put(Hints.GEOMETRY_DISTANCE, 0.0);

        return targetQuery;
    }

    private Filter expandBBox(Filter filter, double distance) {
        return (Filter) filter.accept(
                new BBOXExpandingFilterVisitor(distance, distance, distance, distance), null);
    }

    public static void extractPoints(SimpleFeatureCollection obsPoints, String attrName, MathTransform trans, HeatMap heatMap) throws CQLException 
    {
        Expression attrExpr = null;
        if (attrName != null) {
            attrExpr = ECQL.toExpression(attrName);   
        }
        
        SimpleFeatureIterator obsIt = obsPoints.features();
        
        double[] srcPt = new double[2];
        double[] dstPt = new double[2];
        
        int i = 0;
        try {
            while (obsIt.hasNext()) {
                SimpleFeature feature = obsIt.next();

                try {
                    // get the weight value, if any
                    double val = 1;
                    if (attrExpr != null)
                        val = getValue(feature, attrExpr);
                    
                    // get the point location from the geometry
                    Geometry geom = (Geometry) feature.getDefaultGeometry();
                    Coordinate p = geom.getCoordinate();
                    srcPt[0] = p.x;
                    srcPt[1] = p.y;
                    trans.transform(srcPt, 0, dstPt, 0, 1);
                    Coordinate pobs = new Coordinate(dstPt[0], dstPt[1], val);
                    
                    heatMap.addPoint(pobs.x, pobs.y, val);
                } catch (Exception e) {
                    // just carry on for now (debugging)
                    // throw new ProcessException("Expression " + attrExpr + " failed to evaluate to a numeric value", e);
                }
            }
        } finally {
            obsIt.close();
        }
    }

    private static double getValue(SimpleFeature feature, Expression attrExpr)
    {
        Object valObj = attrExpr.evaluate(feature);
        if (valObj != null) {
            // System.out.println(evaluate);
            Number valNum = (Number) valObj;
            return valNum.doubleValue();
        }
        return 1;
    }
}
