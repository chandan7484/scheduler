/*
 * Copyright (c) 2016 University Nice Sophia Antipolis
 *  
 * This file is part of btrplace.
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *  
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *  
 * You should have received a copy of the GNU Lesser General Public License
 * adouble with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.btrplace.scheduler.choco.extensions.env.trail.chuncked;

import org.btrplace.scheduler.choco.extensions.env.StoredDouble;

/**
 * @author Fabien Hermenier
 */
public class DoubleWorld {


    /**
     * Stack of backtrackable search variables.
     */
    private StoredDouble[] variableStack;

    /**
     * Stack of values (former values that need be restored upon backtracking).
     */
    private double[] valueStack;


    /**
     * Stack of timestamps indicating the world where the former value
     * had been written.
     */
    private int[] stampStack;


    private int now;

    public DoubleWorld(int defaultSize) {
        now = 0;
        valueStack = new double[defaultSize];
        stampStack = new int[defaultSize];
        variableStack = new StoredDouble[defaultSize];
    }
    /**
     * Reacts when a Storeddouble is modified: push the former value & timestamp
     * on the stacks.
     */
    public void savePreviousState(StoredDouble v, double oldValue, int oldStamp) {
        valueStack[now] = oldValue;
        variableStack[now] = v;
        stampStack[now] = oldStamp;
        now++;
        if (now == valueStack.length) {
            resizeUpdateCapacity();
        }
    }

    public void revert() {
        for (int i = now - 1; i >= 0; i--) {
            StoredDouble v = variableStack[i];
            v._set(valueStack[i], stampStack[i]);
        }
    }

    private void resizeUpdateCapacity() {
        final int newCapacity = ((variableStack.length * 3) / 2);
        final StoredDouble[] tmp1 = new StoredDouble[newCapacity];
        System.arraycopy(variableStack, 0, tmp1, 0, variableStack.length);
        variableStack = tmp1;
        final double[] tmp2 = new double[newCapacity];
        System.arraycopy(valueStack, 0, tmp2, 0, valueStack.length);
        valueStack = tmp2;
        final int[] tmp3 = new int[newCapacity];
        System.arraycopy(stampStack, 0, tmp3, 0, stampStack.length);
        stampStack = tmp3;
    }

    public int used() {
        return now;
    }
}
