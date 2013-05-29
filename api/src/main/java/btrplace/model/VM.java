/*
 * Copyright (c) 2013 University of Nice Sophia-Antipolis
 *
 * This file is part of btrplace.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package btrplace.model;

/**
 * Model a virtual machine.
 * VM should not be instantiated directly. Use {@link btrplace.model.Model#newVM()} instead.
 *
 * @author Fabien Hermenier
 * @see {@link Model#newVM()} ()}
 */
public class VM implements Element {

    private int id;

    /**
     * Make a new VM.
     *
     * @param id the VM identifier.
     */
    VM(int id) {
        this.id = id;
    }

    @Override
    public int id() {
        return this.id;
    }

    @Override
    public String toString() {
        return "VM#" + id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VM)) return false;

        VM vm = (VM) o;

        return id == vm.id();
    }

    @Override
    public int hashCode() {
        return id;
    }
}
