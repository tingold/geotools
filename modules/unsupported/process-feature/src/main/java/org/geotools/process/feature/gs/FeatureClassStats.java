package org.geotools.process.feature.gs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.Expression;
import org.geotools.filter.function.ClassificationFunction;
import org.geotools.filter.function.EqualIntervalFunction;
import org.geotools.filter.function.JenksNaturalBreaksFunction;
import org.geotools.filter.function.QuantileFunction;
import org.geotools.filter.function.RangedClassifier;
import org.geotools.process.ProcessException;
import org.geotools.process.classify.ClassificationStats;
import org.geotools.process.classify.Classification.Method;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.geotools.resources.i18n.ErrorKeys;
import org.geotools.resources.i18n.Errors;
import org.geotools.util.Converters;
import org.jaitools.numeric.Range;
import org.jaitools.numeric.Statistic;
import org.jaitools.numeric.StreamingSampleStats;
import org.opengis.feature.Feature;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.FilterFactory;
import org.opengis.util.ProgressListener;

@DescribeProcess(title = "featureClassStats", description = "Calculates statistics from feature" +
        " values classified into bins/classes.")
public class FeatureClassStats implements GSProcess {

    static FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();

    @DescribeResult(name = "results", description = "The classified results")
    public Results execute(
        @DescribeParameter(name = "features", 
          description = "The feature collection to analyze") FeatureCollection features,
        @DescribeParameter(name = "attribute", 
          description = "The feature attribute to analyze") String attribute,
        @DescribeParameter(name = "classes", 
          description = "The number of breaks/classes", min = 0) Integer classes,
        @DescribeParameter(name = "method", 
          description = "The classification method", min = 0) Method method,
        @DescribeParameter(name = "stats", 
          description = "The statistics to calculate for each class", collectionType = Statistic.class, min = 0) Set<Statistic> stats,
        @DescribeParameter(name = "noData", 
          description = "The attribute value to be ommitted from any calculation", min = 0 ) Double noData,
        ProgressListener progressListener) throws ProcessException, IOException {

        //
        // initial checks/defaults
        //
        if(features==null){
            throw new ProcessException(Errors.format(ErrorKeys.NULL_ARGUMENT_$1,"features"));
        }
        if(attribute==null){
            throw new ProcessException(Errors.format(ErrorKeys.NULL_ARGUMENT_$1,"attribute"));
        }
        PropertyDescriptor property = features.getSchema().getDescriptor(attribute);
        if (property == null) {
            throw new ProcessException("No such feature attribute '" + attribute + "'");
        }
        if (!Number.class.isAssignableFrom(property.getType().getBinding())) {
            throw new ProcessException("Feature attribute '" + attribute + "' is not numeric");
        }

        if (classes == null) {
            classes = 10;
        }

        if (classes < 1) {
            throw new ProcessException(Errors.format(ErrorKeys.ILLEGAL_ARGUMENT_$2, "classes", classes));
        }

        
        //other defaults
        if (method == null) {
            method = Method.EQUAL_INTERVAL;
        }
        if (stats == null || stats.isEmpty()) {
            stats = Collections.singleton(Statistic.MEAN);
        }

        //choose the classification function
        ClassificationFunction cf= null;
        switch(method) {
            case EQUAL_INTERVAL:
                cf = new EqualIntervalFunction();
                break;
            case QUANTILE:
                cf = new QuantileFunction();
                break;
            case NATURAL_BREAKS:
                cf = new JenksNaturalBreaksFunction();
                break;
            default:
                throw new ProcessException("Unknown method: " + method);
        }
        cf.setClasses(classes);
        cf.setExpression((Expression) filterFactory.property(attribute));

        //compute the breaks
        RangedClassifier rc = (RangedClassifier) cf.evaluate(features);

        //build up the stats
        List<Range<Double>> ranges = new ArrayList<Range<Double>>();
        StreamingSampleStats[] sampleStats = new StreamingSampleStats[rc.getSize()];
        for (int i = 0; i < rc.getSize(); i++) {
            ranges.add(Range.create((Double)rc.getMin(i), true, (Double)rc.getMax(i), i == rc.getSize()-1));
            
            StreamingSampleStats s = new StreamingSampleStats(Range.Type.INCLUDE);
            s.setStatistics(stats.toArray(new Statistic[stats.size()]));
            
            if (noData != null) {
                s.addNoDataValue(noData);
            }
            
            sampleStats[i] = s;
        }

        //calculate all the stats
        FeatureIterator it = features.features();
        try {
            while(it.hasNext()) {
                Feature f  = it.next();
                Object val = f.getProperty(attribute).getValue();
                if (val == null) {
                    continue;
                }

                int slot = rc.classify(val);
                sampleStats[slot].offer(Converters.convert(val, Double.class));
            }
        }
        finally {
            it.close();
        }

        return new Results(ranges, sampleStats);
    }

    public static class Results implements ClassificationStats {

        List<Range<Double>> ranges;
        StreamingSampleStats[] sampleStats;
        Statistic firstStat;

        public Results(List<Range<Double>> ranges, StreamingSampleStats[] sampleStats) {
            this.ranges = ranges;
            this.sampleStats = sampleStats;
            this.firstStat = sampleStats[0].getStatistics().iterator().next();
        }

        public int size() {
            return ranges.size();
        }

        public Set<Statistic> getStats() {
            return sampleStats[0].getStatistics();
        }

        public Range range(int i) {
            return ranges.get(i);
        }

        public Double value(int i, Statistic stat) {
            return sampleStats[i].getStatisticValue(stat);
        }

        public Long count(int i) {
            return sampleStats[i].getNumAccepted(firstStat);
        }

        public void print() {
            for (int i = 0; i < size(); i++) {
                System.out.println(range(i));
                for (Statistic stat : sampleStats[0].getStatistics()) {
                    System.out.println(stat + " = " + value(i, stat));
                }
            }
        }
    }
}
