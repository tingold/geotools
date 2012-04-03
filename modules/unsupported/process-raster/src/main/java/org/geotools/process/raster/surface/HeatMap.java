/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geotools.process.raster.surface;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Computes a Heat Map surface from a set of irregular data points, each containing a positive height value.
 * 
 * @author mdavis
 * 
 */
public class HeatMap {
    /**
         * 
         */
    private static final int GAUSSIAN_APPROX_ITER = 4;

    private Envelope srcEnv;

    private int xSize;

    private int ySize;

    private GridTransform gridTrans;

    private float[][] grid;
    private int kernelRadiusGrid;
    private Envelope envExp;
    
    public HeatMap(int kernelRadius, Envelope srcEnv, int xSize, int ySize) {
        
        this.kernelRadiusGrid = kernelRadius;
        if (this.kernelRadiusGrid < 0) kernelRadiusGrid = 0;
        
        this.srcEnv = srcEnv;
        this.xSize = xSize;
        this.ySize = ySize;
        
        init();
    }

    private void init() {
        gridTrans = new GridTransform(srcEnv, xSize, ySize);

        int xSizeExp = xSize + 2 * kernelRadiusGrid;
        int ySizeExp = ySize + 2 * kernelRadiusGrid;

        grid = new float[xSizeExp][ySizeExp];
    }

    public void addPoint(double x, double y, double value) {
        if (!srcEnv.contains(x, y))
            return;

        /**
         * Input points are converted to grid space, and offset by the grid expansion offset
         */
        int gi = gridTrans.i(x) + kernelRadiusGrid;
        int gj = gridTrans.j(y) + kernelRadiusGrid;

        grid[gi][gj] += value;
        //System.out.println("data[" + gi + ", " + gj + "] <- " + value);
    }

    public float[][] computeSurface() {

        computeHeatmap(grid, kernelRadiusGrid);

        float[][] gridOut = extractGrid(grid, kernelRadiusGrid, kernelRadiusGrid, xSize, ySize);

        return gridOut;
    }

    private float[][] extractGrid(float[][] grid, int xBase, int yBase, int xSize, int ySize) {
        float[][] gridExtract = new float[xSize][ySize];
        for (int i = 0; i < xSize; i++) {
            for (int j = 0; j < ySize; j++) {
                gridExtract[i][j] = grid[xBase + i][yBase + j];
            }
        }
        return gridExtract;
    }

    private float[][] computeHeatmap(float[][] grid, int kernelRadius) {
        int xSize = grid.length;
        int ySize = grid[0].length;
        
        int baseBoxKernelRadius = kernelRadius / GAUSSIAN_APPROX_ITER;
        int radiusIncBreak = kernelRadius - baseBoxKernelRadius * GAUSSIAN_APPROX_ITER;

        /**
         * Since Box Blur is linearly separable, can implement it by doing 2 1-D box blurs in different directions. Using a flipped buffer grid allows
         * the same code to compute each direction, as well as preserving input grid values.
         */
        // holds flipped copy of first box blur pass
        float[][] grid2 = new float[ySize][xSize];
        for (int count = 0; count < GAUSSIAN_APPROX_ITER; count++) {
            int boxKernelRadius = baseBoxKernelRadius;
            /**
             * If required, increment radius to ensure sum of radii equals total kernel radius
             */
            if (count < radiusIncBreak)
                boxKernelRadius++;
            //System.out.println(boxKernelRadius);

            boxBlur(boxKernelRadius, grid, grid2);
            boxBlur(boxKernelRadius, grid2, grid);
        }

        // testNormalizeFactor(baseBoxKernelRadius, radiusIncBreak);
        normalize(grid);
        return grid;
    }

    /**
     * NO! this method is too simplistic to determine normalization factor Would need to use a full 2D grid and smooth it to get correct value
     * 
     * @param baseBoxKernelRadius
     * @param radiusIncBreak
     */
    private void testNormalizeFactor(int baseBoxKernelRadius, int radiusIncBreak) {
        double val = 1.0;
        for (int count = 0; count < GAUSSIAN_APPROX_ITER; count++) {
            int boxKernelRadius = baseBoxKernelRadius;
            /**
             * If required, increment radius to ensure sum of radii equals total kernel radius
             */
            if (count < radiusIncBreak)
                boxKernelRadius++;

            int dia = 2 * boxKernelRadius + 1;
            float kernelVal = kernelVal(boxKernelRadius);
            System.out.println(boxKernelRadius + " kernel val = " + kernelVal);

            if (count == 0) {
                val = val * 1 * kernelVal;
            } else {
                val = val * dia * kernelVal;
            }
            System.out.println("norm val = " + val);
            if (count == 0) {
                val = val * 1 * kernelVal;
            } else {
                val = val * dia * kernelVal;
            }
        }
        System.out.println("norm factor = " + val);
    }

    /**
     * Normalizes grid values to range [0,1]
     * 
     * @param grid
     */
    private void normalize(float[][] grid) {
        float max = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                if (grid[i][j] > max)
                    max = grid[i][j];
            }
        }

        float normFactor = 1.0f / max;

        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                grid[i][j] *= normFactor;
            }
        }
    }

    private float kernelVal(int kernelRadius) {
        // This kernel function has been confirmed to integrate to 1 over the full radius
        float val = (float) (1.0f / (2 * kernelRadius + 1));
        return val;
    }

    private void boxBlur(int kernelRadius, float[][] input, float[][] output) {
        int width = input.length;
        int height = input[0].length;

        // init moving average total
        float kernelVal = kernelVal(kernelRadius);
        // System.out.println("boxblur: radius = " + kernelRadius + " kernel val = " + kernelVal);

        for (int j = 0; j < height; j++) {

            double tot = 0.0;

            for (int i = -kernelRadius; i <= kernelRadius; i++) {
                if (i < 0 || i >= width)
                    continue;
                tot += kernelVal * input[i][j];
            }

            // System.out.println(tot);

            output[j][0] = (float) tot;

            for (int i = 1; i < width; i++) {

                // update box running total
                int iprev = i - 1 - kernelRadius;
                if (iprev >= 0)
                    tot -= kernelVal * input[iprev][j];

                int inext = i + kernelRadius;
                if (inext < width)
                    tot += kernelVal * input[inext][j];

                output[j][i] = (float) tot;
                // if (i==49 && j==147) System.out.println("val[ " + i + ", " + j + "] = " + tot);

            }
        }
    }
}
