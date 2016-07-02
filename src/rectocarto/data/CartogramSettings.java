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
    public double boundaryWidth = 30;
    public double minimumSeaDimension = 10;
    public double minimumSeparation = 2;
    public double cartogramWidth = 1000;
    public double cartogramHeight = 750;
    
    public Objective objective = Objective.AVERAGE_ERROR_SQUARED;
    
    public enum Objective {
        MAX_ERROR, AVERAGE_ERROR, MAX_AND_AVERAGE_ERROR, AVERAGE_ERROR_SQUARED, MAX_AND_AVERAGE_ERROR_SQUARED
    }
}
