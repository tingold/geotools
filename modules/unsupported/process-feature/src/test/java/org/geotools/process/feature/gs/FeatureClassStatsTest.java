package org.geotools.process.feature.gs;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.property.PropertyDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.visitor.FeatureCalc;
import org.geotools.filter.function.Classifier;
import org.geotools.filter.function.RangedClassifier;
import org.geotools.process.classify.Classification.Method;
import org.geotools.process.feature.gs.FeatureClassStats.Results;
import org.geotools.test.TestData;
import org.jaitools.numeric.Statistic;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;

public class FeatureClassStatsTest {

    Object[][] data = new Object[][] { { 'a', 1 }, { 'b', 1 }, { 'c', 2 }, { 'd', 3 }, { 'e', 3 },
            { 'f', 8 }, { 'g', 8 }, { 'h', 9 }, { 'i', 11 }, { 'j', 14 }, { 'k', 16 }, { 'l', 24 },
            { 'm', 26 }, { 'n', 26 }, { 'o', 45 }, { 'p', 53 } };
    /*Object[][] data = new Object[][] { { 'd', 3 }, { 'e', 3 },
            { 'f', 8 }, { 'g', 8 }, { 'h', 9 }, { 'i', 11 }, { 'j', 14 }, { 'k', 16 }, { 'l', 24 },
            { 'm', 26 }, { 'n', 26 }, { 'o', 45 } };*/

    SimpleFeatureCollection features;

    @Before
    public void setup() throws IOException {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("data");
        tb.add("name", String.class);
        tb.add("valu", Integer.class);

        SimpleFeatureType featureType = tb.buildFeatureType();
        features = new DefaultFeatureCollection(null, featureType);

        SimpleFeatureBuilder b = new SimpleFeatureBuilder(featureType);
        for (Object[] tuple : data) {
            b.add(tuple[0]);
            b.add(tuple[1]);
            features.add(b.buildFeature(null));
        }
    }

    @Test
    public void testEqualInterval() throws Exception {

        Results result = new FeatureClassStats().execute(features, "valu", 4, Method.EQUAL_INTERVAL, 
            new LinkedHashSet<Statistic>(Arrays.asList(Statistic.MEAN, Statistic.SUM)), null, null);

        assertResult(result, 4, new Number[][] { { 1, 14 }, { 14, 27 }, { 27, 40 }, { 40, 53 } },
        /* count */new Integer[] { 9, 5, 0, 2 },
        /* sum */new Number[] { 46d, 106d, Double.NaN, 98d },
        /* average */new Number[] { 5.1, 21.2, Double.NaN, 49.0 });
    }

    @Test
    public void testQuantile() throws Exception {
        Results result = new FeatureClassStats().execute(features, "valu", 4, Method.QUANTILE, 
            new LinkedHashSet<Statistic>(Arrays.asList(Statistic.MEAN, Statistic.SUM)), null, null);

        assertResult(result, 4, new Number[][] { { 1, 3 }, { 3, 11 }, { 11, 26 }, { 26, 53 } },
        /* count */new Integer[] { 3, 5, 4, 4 },
        /* sum */new Number[] {4d, 31d, 65d, 150d },
        /* average */new Number[] { 1.3, 6.2, 16.3, 37.5 });
        
    }

    @Test
    public void testNatural() throws Exception {
        Results result = new FeatureClassStats().execute(features, "valu", 4, Method.NATURAL_BREAKS, 
            new LinkedHashSet<Statistic>(Arrays.asList(Statistic.MEAN, Statistic.SUM)), null, null);

        assertResult(result, 4, new Number[][] { { 1, 3 }, { 3, 16 }, { 16, 26 }, { 26, 53 } },
            /* count */new Integer[] { 3, 7, 2, 4 },
            /* sum */new Number[] {4d, 56d, 40d, 150d },
            /* average */new Number[] { 1.3, 8d, 20d, 37.5 });
        
    }

    void assertResult(Results result, int classes, Number[][] ranges, Integer[] counts,
        Number[] sums, Number[] averages) {

        assertEquals(classes, result.size());

        for (int i = 0; i < result.size(); i++) {
            Number min = result.range(i).getMin();
            Number max = result.range(i).getMax();

            assertNumbersEqual(ranges[i][0], min);
            assertNumbersEqual(ranges[i][1], max);

            assertNumbersEqual(counts[i], result.count(i));
            if (sums != null) {
                assertNumbersEqual(sums[i], result.value(i, Statistic.SUM));
            }
            if (averages != null) {
                assertNumbersEqual(averages[i], result.value(i, Statistic.MEAN));
            }
        }
    }

    void assertNumbersEqual(Number expected, Number actual) {
        if (expected == null) {
            assertNull(actual);
        } else {
            assertEquals(expected.doubleValue(), actual.doubleValue(), 0.1);
        }
    }

}
