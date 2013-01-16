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

import btrplace.model.*;
import btrplace.model.constraint.Fence;
import btrplace.model.constraint.Online;
import btrplace.model.constraint.Spread;
import btrplace.plan.ReconfigurationPlan;
import btrplace.solver.SolverException;
import btrplace.solver.choco.ChocoReconfigurationAlgorithm;
import btrplace.solver.choco.DefaultChocoReconfigurationAlgorithm;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

/**
 * Unit tests for {@link CSpread}.
 *
 * @author Fabien Hermenier
 */
public class CSpreadTest extends ConstraintTestMaterial {

    /*@Test
    public void testInstantiation() {
        Spread b = new Spread(Collections.singleton(UUID.randomUUID()));
        CSpread c = new CSpread(b);
        Assert.assertEquals(b, c.getAssociatedConstraint());
    } */

    private static Model getModel() {
        Mapping map = new DefaultMapping();
        map.addOnlineNode(n1);
        map.addOnlineNode(n2);
        map.addOnlineNode(n3);
        map.addRunningVM(vm1, n1);
        map.addRunningVM(vm2, n2);
        return new DefaultModel(map);
    }

    @Test
    public void testDiscrete() throws SolverException {
        Model m = getModel();
        List<SatConstraint> cstr = new ArrayList<SatConstraint>();
        ChocoReconfigurationAlgorithm cra = new DefaultChocoReconfigurationAlgorithm();
        cra.labelVariables(true);
        Spread s = new Spread(m.getMapping().getAllVMs());
        s.setContinuous(false);
        cstr.add(s);
        cstr.add(new Online(m.getMapping().getAllNodes()));
        cstr.add(new Fence(Collections.singleton(vm1), Collections.singleton(n2)));
        ReconfigurationPlan p = cra.solve(m, cstr);
        Assert.assertNotNull(p);
        System.err.println(p);
        Mapping res = p.getResult().getMapping();
        Assert.assertEquals(2, p.getSize());
        Assert.assertNotSame(res.getVMLocation(vm1), res.getVMLocation(vm2));
    }

    @Test
    public void testContinuous() throws SolverException {
        Model m = getModel();
        List<SatConstraint> cstr = new ArrayList<SatConstraint>();
        ChocoReconfigurationAlgorithm cra = new DefaultChocoReconfigurationAlgorithm();
        cra.labelVariables(true);
        Spread s = new Spread(m.getMapping().getAllVMs());
        s.setContinuous(true);
        cstr.add(s);
        cstr.add(new Online(m.getMapping().getAllNodes()));
        cstr.add(new Fence(Collections.singleton(vm1), Collections.singleton(n2)));
        ReconfigurationPlan p = cra.solve(m, cstr);
        Assert.assertNotNull(p);
        System.err.println(p);
        Mapping res = p.getResult().getMapping();
        Assert.assertEquals(2, p.getSize());
        Assert.assertNotSame(res.getVMLocation(vm1), res.getVMLocation(vm2));
    }

    @Test
    public void testGetMisplaced() {
        UUID n1 = UUID.randomUUID();
        UUID n2 = UUID.randomUUID();

        UUID vm1 = UUID.randomUUID();
        UUID vm2 = UUID.randomUUID();
        UUID vm3 = UUID.randomUUID();

        Mapping map = new DefaultMapping();
        map.addOnlineNode(n1);
        map.addOnlineNode(n2);
        map.addRunningVM(vm1, n1);
        map.addRunningVM(vm2, n2);
        map.addRunningVM(vm3, n1);
        Set<UUID> vms = new HashSet<UUID>();
        vms.add(vm1);
        vms.add(vm2);
        Spread s = new Spread(vms);
        CSpread cs = new CSpread(s);
        Model mo = new DefaultModel(map);
        Assert.assertTrue(cs.getMisPlacedVMs(mo).isEmpty());
        vms.add(vm3);
        Assert.assertEquals(map.getRunningVMs(n1), cs.getMisPlacedVMs(mo));
    }

    /**
     * 2 VMs are already hosted on a same node, check
     * if separation is working in continuous mode
     */
    @Test
    public void testSeparateWithContinuous() throws SolverException {
        Model m = getModel();
        Mapping map = m.getMapping();
        map.addRunningVM(vm2, n1);
        List<SatConstraint> cstr = new ArrayList<SatConstraint>();
        ChocoReconfigurationAlgorithm cra = new DefaultChocoReconfigurationAlgorithm();
        cra.labelVariables(true);
        Spread s = new Spread(m.getMapping().getAllVMs());
        s.setContinuous(true);
        cstr.add(s);
        cstr.add(new Online(m.getMapping().getAllNodes()));
        cstr.add(new Fence(Collections.singleton(vm1), Collections.singleton(n2)));
        ReconfigurationPlan p = cra.solve(m, cstr);
        Assert.assertNotNull(p);
        Mapping res = p.getResult().getMapping();
        Assert.assertEquals(1, p.getSize());
        Assert.assertNotSame(res.getVMLocation(vm1), res.getVMLocation(vm2));
    }
}
