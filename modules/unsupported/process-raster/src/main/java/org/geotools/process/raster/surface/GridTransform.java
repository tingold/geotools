/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geotools.process.raster.surface;

import com.vividsolutions.jts.geom.Envelope;

class GridTransform {

    private Envelope env;

    private int xSize;

    private int ySize;

    private double dx;

    private double dy;

    public GridTransform(Envelope env, int xSize, int ySize) {
        this.env = env;
        this.xSize = xSize;
        this.ySize = ySize;
        dx = env.getWidth() / (xSize - 1);
        dy = env.getHeight() / (ySize - 1);
    }

    public double x(int i) {
        if (i >= xSize - 1)
            return env.getMaxX();
        return env.getMinX() + i * dx;
    }

    public double y(int i) {
        if (i >= ySize - 1)
            return env.getMaxY();
        return env.getMinY() + i * dy;
    }

    public int i(double x) {
        if (x > env.getMaxX())
            return xSize;
        if (x < env.getMinX())
            return -1;
        int i = (int) ((x - env.getMinX()) / dx);
        // have already check x is in bounds, so ensure returning a valid value
        if (i >= xSize)
            i = xSize - 1;
        return i;
    }

    public int j(double y) {
        if (y > env.getMaxY())
            return ySize;
        if (y < env.getMinY())
            return -1;
        int j = (int) ((y - env.getMinY()) / dy);
        // have already check x is in bounds, so ensure returning a valid value
        if (j >= ySize)
            j = ySize - 1;
        return j;
    }

}