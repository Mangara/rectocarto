/*
 * Copyright 2016 Sander Verdonschot <sander.verdonschot at gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rectocarto.data;

public class CartogramSettings {

    /**
     * The width (in pixels) of the final cartogram without the borders.
     */
    public double cartogramWidth = 1000;
    /**
     * The height (in pixels) of the final cartogram without the borders.
     */
    public double cartogramHeight = 750;
    /**
     * The width (in pixels) of the four boundary regions (NORTH, EAST, SOUTH,
     * WEST).
     */
    public double boundaryWidth = 30;
    /**
     * The minimum width or height (in pixels) of a sea region.
     */
    public double minimumSeaDimension = 10;
    /**
     * The minimum distance (in pixels) between two features of the cartogram.
     * If two regions are adjacent, they must share at least this much border.
     * This implicitly defines a minimum width and height for all regions.
     */
    public double minimumFeatureSize = 2;
    /**
     * The maximum ratio between the height and width (and vice versa) of
     * non-sea regions.
     */
    public double maximumAspectRatio = 10;
    /**
     * The fraction of the total area that should be covered by sea regions.
     */
    public double seaAreaFraction = 0.4;

    /**
     * The type of objective function that should be used when constructing this
     * cartogram using (bi)linear programs.
     */
    public Objective objective = Objective.MAX_AND_AVERAGE_ERROR;

    public enum Objective {

        /**
         * The maximum cartographic error.
         */
        MAX_ERROR, 
        /**
         * The average cartographic error.
         */
        AVERAGE_ERROR,
        /**
         * The average of the maximum and average cartographic error.
         */
        MAX_AND_AVERAGE_ERROR,
        /**
         * The average of the squares of each region's cartographic error.
         */
        AVERAGE_ERROR_SQUARED,
        /**
         * The average of the maximum cartographic error and the squares of each
         * region's cartographic error.
         */
        MAX_AND_AVERAGE_ERROR_SQUARED
    }
}
