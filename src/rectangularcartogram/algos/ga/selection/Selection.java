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
package rectangularcartogram.algos.ga.selection;

public interface Selection {

    /**
     * Prepares the selection process for the current generation.
     * Call this once per generation, before calling select.
     * @param qualities
     */
    public abstract void preprocess(double[] qualities);

    /**
     * Selects an individual from the population.
     * @return
     */
    public abstract int select(double[] qualities);
}
