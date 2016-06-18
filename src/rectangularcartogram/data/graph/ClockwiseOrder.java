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
package rectangularcartogram.data.graph;

import java.util.Comparator;

public class ClockwiseOrder implements Comparator<Edge> {

        private Vertex center;

        public ClockwiseOrder(Vertex center) {
            this.center = center;
        }

        /**
         * A clockwise order on the edges around the center vertex.
         *
         * 2     3
         *  \   /
         *   \ /
         *    v
         *   /|\
         *  / | \
         * 1  |  4
         *
         * @param e1
         * @param e2
         * @return
         */
        public int compare(Edge e1, Edge e2) {
            // compare the angles of e1 and e2
            return Double.compare(getAngle(e1), getAngle(e2));
        }

        /**
         * Computes the angle this edge makes with the positive y-axis. The returned angle will always be within [-pi, pi].
         * @param e
         * @return the angle this edge makes with the positive y-axis
         */
        private double getAngle(Edge e) {
            Vertex dest = (e.getVA() == center ? e.getVB() : e.getVA());

            double vx = dest.getX() - center.getX();
            double vy = dest.getY() - center.getY();

            double angle = Math.acos(vy / Math.sqrt(vx * vx + vy * vy));

            if (vx > 0) {
                return angle;
            } else {
                return -angle;
            }
        }
    }
