package org.geotools.process.classify;

import java.util.Set;

import org.jaitools.numeric.Range;
import org.jaitools.numeric.Statistic;

public interface ClassificationStats {

    int size();

    Set<Statistic> getStats();

    Range range(int i);

    Double value(int i, Statistic stat);

    Long count(int i);
}
