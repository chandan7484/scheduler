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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.btrplace.scheduler.choco.extensions.env.trail.chuncked;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.btrplace.scheduler.choco.extensions.env.trail.OperationTrail;
import org.chocosolver.memory.structure.Operation;


public class ChunkedOperationTrail implements OperationTrail{


    private ObjectArrayList<OperationWorld> worlds;

    private OperationWorld current;

    /**
     * Constructs a trail with predefined size.
     */
    public ChunkedOperationTrail() {
        worlds = new ObjectArrayList<>();
    }


    /**
     * Moving up to the next world.
     *
     * @param worldIndex current world index
     */

    public void worldPush(int worldIndex) {
        int size = 1024;
        if (current != null) {
            size = Math.max(current.used(), size);
        }
        current = new OperationWorld(size);
        worlds.push(current);
    }


    /**
     * Moving down to the previous world.
     *
     * @param worldIndex current world index
     */

    public void worldPop(int worldIndex) {
        current.revert();
        worlds.pop();
        current = null;
        if (!worlds.isEmpty()) {
            current = worlds.top();
        }
    }

    /**
     * Comits a world: merging it with the previous one.
     */

    public void worldCommit(int worldIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void savePreviousState(Operation oldValue) {
        current.savePreviousState(oldValue);
    }
}
