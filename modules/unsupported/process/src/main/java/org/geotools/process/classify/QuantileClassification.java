package org.geotools.process.classify;

import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

public class QuantileClassification extends Classification {

    int[] counts;
    SortedMap<Double, Integer>[] tables;

    public QuantileClassification(int numBands) {
        super(Method.QUANTILE, numBands);
        counts = new int[numBands];
        tables = new SortedMap[numBands];
    }

    public void count(double value, int band) {
        counts[band]++;

        SortedMap<Double, Integer> table = getTable(band);

        Integer count = table.get(value);
        table.put(value, count != null ? new Integer(count + 1) : new Integer(1));
    }

    public SortedMap<Double, Integer> getTable(int band) {
        SortedMap<Double, Integer> table = tables[band];
        if (table == null) {
            table = new TreeMap<Double, Integer>();
            tables[band] = table;
        }
        return table;
    }

    public int getCount(int band) {
        return counts[band];
    }

    void printTable() {
        for (int i = 0; i < tables.length; i++) {
            SortedMap<Double, Integer> table = getTable(i);
            for (Entry<Double, Integer> e : table.entrySet()) {
                System.out.println(String.format("%f: %d", e.getKey(), e.getValue()));
            }
        }
    }
}
