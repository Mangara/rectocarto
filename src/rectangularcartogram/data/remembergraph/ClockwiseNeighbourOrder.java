/*
 * Copyright 2010-2016 Sander Verdonschot <sander.verdonschot at gmail.com>.
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
package rectangularcartogram.data.remembergraph;

import java.util.Comparator;

public class ClockwiseNeighbourOrder implements Comparator<RememberVertexEntry> {

    private RememberVertex center;

    public ClockwiseNeighbourOrder(RememberVertex center) {
        this.center = center;
    }

    public int compare(RememberVertexEntry e1, RememberVertexEntry e2) {
        // compare the angles of e1 and e2
        return Double.compare(getAngle(e1), getAngle(e2));
    }

    private double getAngle(RememberVertexEntry e) {
        double vx = e.getNeighbour().getX() - center.getX();
        double vy = e.getNeighbour().getY() - center.getY();

        double angle = Math.acos(vy / Math.sqrt(vx * vx + vy * vy));

        if (vx > 0) {
            return angle;
        } else {
            return -angle;
        }
    }
}
