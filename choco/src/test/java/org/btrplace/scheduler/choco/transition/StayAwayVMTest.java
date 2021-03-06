/*
 * Copyright  2020 The BtrPlace Authors. All rights reserved.
 * Use of this source code is governed by a LGPL-style
 * license that can be found in the LICENSE.txt file.
 */

package org.btrplace.scheduler.choco.transition;

import org.btrplace.model.DefaultModel;
import org.btrplace.model.Mapping;
import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.DefaultReconfigurationProblemBuilder;
import org.btrplace.scheduler.choco.ReconfigurationProblem;
import org.chocosolver.solver.Cause;
import org.chocosolver.solver.exception.ContradictionException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link StayAwayVM}.
 *
 * @author Fabien Hermenier
 */
public class StayAwayVMTest {

    @Test
    public void testBasic() throws SchedulerException, ContradictionException {
        Model mo = new DefaultModel();
        Mapping map = mo.getMapping();
        final VM vm1 = mo.newVM();
        final VM vm2 = mo.newVM();
        Node n1 = mo.newNode();

        map.addOnlineNode(n1);
        map.addSleepingVM(vm1, n1);
        map.addReadyVM(vm2);

        ReconfigurationProblem rp = new DefaultReconfigurationProblemBuilder(mo)
                .build();

        rp.getNodeAction(n1).getState().instantiateTo(1, Cause.Null);
        StayAwayVM ma1 = (StayAwayVM) rp.getVMAction(vm1);
        StayAwayVM ma2 = (StayAwayVM) rp.getVMAction(vm2);
        Assert.assertEquals(vm1, ma1.getVM());
        Assert.assertEquals(vm2, ma2.getVM());

        for (VMTransition am : rp.getVMActions()) {
            Assert.assertTrue(am.getState().isInstantiatedTo(0));
            Assert.assertNull(am.getCSlice());
            Assert.assertNull(am.getDSlice());
            Assert.assertTrue(am.getStart().isInstantiatedTo(0));
            Assert.assertTrue(am.getEnd().isInstantiatedTo(0));
            Assert.assertTrue(am.getDuration().isInstantiatedTo(0));
        }

        ReconfigurationPlan p = rp.solve(0, false);
        Assert.assertNotNull(p);
        Assert.assertEquals(0, p.getSize());
    }
}
