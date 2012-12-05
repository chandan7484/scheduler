/*
 * Copyright (c) 2012 University of Nice Sophia-Antipolis
 *
 * This file is part of btrplace.
 *
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

package btrplace.solver.choco.constraint;


import btrplace.model.Model;
import btrplace.model.SatConstraint;
import btrplace.model.ShareableResource;
import btrplace.model.constraint.Overbook;
import btrplace.plan.ReconfigurationPlan;
import btrplace.solver.SolverException;
import btrplace.solver.choco.*;
import btrplace.solver.choco.chocoUtil.ChocoUtils;
import choco.cp.solver.CPSolver;
import choco.kernel.solver.ContradictionException;
import choco.kernel.solver.variables.integer.IntDomainVar;
import gnu.trove.TIntArrayList;

import java.util.*;

/**
 * Choco implementation of {@link btrplace.model.SatConstraint}.
 *
 * @author Fabien Hermenier
 */
public class COverbook implements ChocoSatConstraint {

    private Overbook cstr;

    /**
     * Make a new constraint.
     *
     * @param o the constraint to rely on
     */
    public COverbook(Overbook o) {
        cstr = o;
    }

    @Override
    public void inject(ReconfigurationProblem rp) throws SolverException {

        CPSolver s = rp.getSolver();
        ResourceMapping rcm = rp.getResourceMapping(cstr.getResource());
        if (rcm == null) {
            throw new SolverException(rp.getSourceModel(), "Unable to get the resource mapping '" + cstr.getResource() + "'");
        }

        IntDomainVar[] rawCapa = rcm.getRawNodeUsage();
        IntDomainVar[] realCapa = rcm.getRealNodeUsage();

        if (cstr.getRatio() == 1) {
            for (UUID u : cstr.getInvolvedNodes()) {
                int nIdx = rp.getNode(u);
                s.post(s.eq(realCapa[nIdx], rawCapa[nIdx]));
            }
        } else {
            double ratio = cstr.getRatio();
            for (UUID u : cstr.getInvolvedNodes()) {
                int nIdx = rp.getNode(u);
                //beware of truncation made by choco: 3 = 7 / 2 while here, 4 pCPU will be used
                //The hack consists in computing the number of free pCPU
                /*
                int maxRaw = ...;
                int maxReal = maxRaw * factor;
                freeReal = var(0,maxReal)
                post(eq(freeReal, minus(maxReal,usageReal))
                freeRaw = div(freeReal,factor);
                eq(usageRaw, minus(maxRaw,freeRaw)
                 */
                //example: 6 pCPU, 7 vCPU, factor= 2
                //freePCpu = ((2 * 6) - 7) / 2 = 2
                //usedPCPU = 6 - 2 = 4 \o/
                int maxRaw = rcm.getSourceResource().get(u);
                int maxReal = (int) (maxRaw * ratio); //Truncation, we ignore partial virtual resource so it's correct
                try {
                    realCapa[nIdx].setSup(maxReal);
                } catch (ContradictionException ex) {
                    throw new SolverException(rp.getSourceModel(), "Unable to restrict the real '" + rcm.getIdentifier() + "' capacity of " + rp.getNode(nIdx) + " to " + maxReal);
                }
                IntDomainVar freeReal = s.createBoundIntVar(rp.makeVarLabel("free_real('" + u + "')"), 0, maxReal);
                s.post(s.eq(freeReal, s.minus(maxReal, realCapa[nIdx])));
                IntDomainVar freeRaw = ChocoUtils.div(s, freeReal, (int) ratio); //TODO: check for the correctness of the truncation
                s.post(s.eq(rawCapa[nIdx], s.minus(maxRaw, freeRaw)));
            }


            //The slice scheduling constraint that is necessary
            //TODO: a slice on both the real and the raw resource usage ?

            TIntArrayList cUse = new TIntArrayList();
            List<IntDomainVar> dUse = new ArrayList<IntDomainVar>();

            for (ActionModel a : rp.getVMActions()) {
                Slice c = a.getCSlice();
                Slice d = a.getDSlice();
                if (c != null) {
                    UUID vmId = c.getSubject();
                    cUse.add(rcm.getSourceResource().get(vmId));
                }
                if (d != null) {
                    UUID vmId = d.getSubject();
                    dUse.add(rcm.getVMConsumption()[rp.getVM(vmId)]);
                }
            }

            IntDomainVar[] capa = new IntDomainVar[rp.getNodes().length];
            for (int i = 0; i < rp.getNodes().length; i++) {
                capa[i] = realCapa[i];
            }

            TaskSchedulerBuilder.getInstance().add(capa, cUse.toNativeArray(), dUse.toArray(new IntDomainVar[cUse.size()]));
        }
    }

    @Override
    public Overbook getAssociatedConstraint() {
        return cstr;
    }

    @Override
    public Set<UUID> getMisPlacedVMs(Model m) {
        ShareableResource rc = m.getResource(cstr.getResource());
        Set<UUID> bads = new HashSet<UUID>();
        if (rc == null) { //Should not occur, if the right model is given
            for (UUID n : cstr.getInvolvedNodes()) {
                bads.addAll(m.getMapping().getRunningVMs(n));
            }
        } else {
            //Check if the node is saturated
            for (UUID n : cstr.getInvolvedNodes()) {
                int overCapa = (int) (cstr.getRatio() * rc.get(n));
                //Minus the VMs usage
                for (UUID vmId : m.getMapping().getRunningVMs(n)) {
                    overCapa -= rc.get(vmId);
                    if (overCapa < 0) {
                        bads.addAll(m.getMapping().getRunningVMs());
                        break;
                    }
                }

            }
        }
        return bads;
    }

    @Override
    public boolean isSatisfied(ReconfigurationPlan plan) {
        return cstr.isSatisfied(plan.getResult()).equals(SatConstraint.Sat.SATISFIED);
    }

    /**
     * Builder associated to the constraint.
     */
    public static class Builder implements ChocoSatConstraintBuilder {
        @Override
        public Class<? extends SatConstraint> getKey() {
            return Overbook.class;
        }

        @Override
        public COverbook build(SatConstraint cstr) {
            return new COverbook((Overbook) cstr);
        }
    }
}
