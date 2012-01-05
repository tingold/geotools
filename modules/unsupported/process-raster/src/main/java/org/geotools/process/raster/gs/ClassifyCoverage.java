package org.geotools.process.raster.gs;

import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.util.Set;

import javax.media.jai.PixelAccessor;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.operator.BandSelectDescriptor;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.visitor.AverageVisitor;
import org.geotools.feature.visitor.CountVisitor;
import org.geotools.feature.visitor.FeatureCalc;
import org.geotools.feature.visitor.MaxVisitor;
import org.geotools.feature.visitor.MinVisitor;
import org.geotools.feature.visitor.SumVisitor;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.gs.GSProcess;
import org.geotools.renderer.i18n.Errors;
import org.geotools.resources.i18n.ErrorKeys;
import org.opengis.filter.expression.Expression;
import org.opengis.util.ProgressListener;

public class ClassifyCoverage implements GSProcess {

    private final static double DEFAULT_NODATA = 0d;

    public static enum Method {

        EQUAL {
        }, 
        QUANTILE {
        }, 
        NATURAL {
        };
    }

    public static enum Stat {
        MIN {
            @Override
            public FeatureCalc calculator(Expression e) {
                return new MinVisitor(e);
            }
        }, 
        MAX {
            @Override
            public FeatureCalc calculator(Expression e) {
                return new MaxVisitor(e);
            }
        }, 
        AVERAGE {
            @Override
            public FeatureCalc calculator(Expression e) {
                return new AverageVisitor(e);
            }
        }, 
        COUNT {
            @Override
            public FeatureCalc calculator(Expression e) {
                return new CountVisitor();
            }
        }, 
        SUM {
            @Override
            public FeatureCalc calculator(Expression e) {
                return new SumVisitor(e);
            }
        };
        public abstract FeatureCalc calculator(Expression e);
    }

    public Results execute(
        @DescribeParameter(name = "coverage", 
          description = "The coverage to classify") GridCoverage2D coverage,
        @DescribeParameter(name = "band", 
          description = "The band to calculate breaks/statistics for") Integer band,
        @DescribeParameter(name = "classes", 
          description = "The number of breaks/classes", min = 0) Integer classes,
        @DescribeParameter(name = "method", 
          description = "Classification method, one of 'equal', 'quantile', 'natural'", min = 0) Method method,
        @DescribeParameter(name = "stats", 
          description = "The statistics to calcualte for each class", collectionType = Stat.class, min = 0) Set<Stat> stats,
        @DescribeParameter(name = "noData", description = "The pixel value to be assigned to input pixels outside any range (defaults to 0)", min = 0 ) Double noData,
        ProgressListener progressListener) throws ProcessException, IOException {
        
        //
        // initial checks
        //
        if(coverage==null){
                throw new ProcessException(Errors.format(ErrorKeys.NULL_ARGUMENT_$1,"coverage"));
        }

        double nd = DEFAULT_NODATA;
        if (noData != null){
            nd = noData.doubleValue();
        }

        RenderedImage sourceImage = coverage.getRenderedImage();
        
        // parse the band
        if (band == null) {
            band = 0;
        }

        final int numbands=sourceImage.getSampleModel().getNumBands();
        if(band<0 || numbands<=band){
            throw new ProcessException(Errors.format(ErrorKeys.ILLEGAL_ARGUMENT_$2,"band",band));
        }

        if(band==0 && numbands>0 || band>0) {
            sourceImage=BandSelectDescriptor.create(sourceImage, new int []{band}, null);
        }

        return null;
    }

    static class Results {
        
    }
}
