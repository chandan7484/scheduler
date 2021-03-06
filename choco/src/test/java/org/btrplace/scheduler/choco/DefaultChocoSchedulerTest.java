/*
 * Copyright  2020 The BtrPlace Authors. All rights reserved.
 * Use of this source code is governed by a LGPL-style
 * license that can be found in the LICENSE.txt file.
 */

package org.btrplace.scheduler.choco;

import org.btrplace.model.DefaultModel;
import org.btrplace.model.Instance;
import org.btrplace.model.Mapping;
import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.VMState;
import org.btrplace.model.constraint.MinMTTR;
import org.btrplace.model.constraint.Offline;
import org.btrplace.model.constraint.Online;
import org.btrplace.model.constraint.Preserve;
import org.btrplace.model.constraint.Ready;
import org.btrplace.model.constraint.Running;
import org.btrplace.model.constraint.SatConstraint;
import org.btrplace.model.view.ShareableResource;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.runner.SolvingStatistics;
import org.btrplace.scheduler.choco.transition.TransitionFactory;
import org.btrplace.scheduler.choco.transition.VMTransitionBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultChocoScheduler}.
 *
 * @author Fabien Hermenier
 */
public class DefaultChocoSchedulerTest {

    @Test
    public void testGetsAndSets() {
        ChocoScheduler cra = new DefaultChocoScheduler();

        cra.setTimeLimit(10);
        Assert.assertEquals(cra.getTimeLimit(), 10);

        cra.setMaxEnd(-5);
        Assert.assertEquals(cra.getMaxEnd(), -5);

        cra.doOptimize(false);
        Assert.assertEquals(cra.doOptimize(), false);

        cra.doRepair(true);
        Assert.assertEquals(cra.doRepair(), true);

        cra.setVerbosity(3);
        Assert.assertEquals(cra.getVerbosity(), 3);
    }

    @Test(expectedExceptions = {SchedulerException.class})
    public void testGetStatisticsWithTimeout() throws SchedulerException {
        Model mo = new DefaultModel();
        Mapping map = mo.getMapping();
        for (int i = 0; i < 1000; i++) {
            Node n = mo.newNode();
            map.addOnlineNode(n);
            for (int j = 0; j < 10; j++) {
                map.addReadyVM(mo.newVM());
            }
        }
        ChocoScheduler cra = new DefaultChocoScheduler();
        cra.setTimeLimit(1);
        try {
            System.err.println(cra.solve(mo, Running.newRunning(map.getAllVMs())));
        } catch (SchedulerException e) {
            SolvingStatistics stats = cra.getStatistics();
            Assert.assertNotNull(stats);
            System.out.println(stats);
            Assert.assertTrue(stats.getSolutions().isEmpty());
            Assert.assertEquals(stats.getInstance().getModel(), mo);
            throw e;
        }
    }

    @Test
    public void testGetStatisticsWithNoSolution() throws SchedulerException {
        Model mo = new DefaultModel();
        Mapping map = mo.getMapping();
        VM v = mo.newVM();
        Node n = mo.newNode();
        map.addReadyVM(v);
        map.addOfflineNode(n);
        ChocoScheduler cra = new DefaultChocoScheduler();
            ReconfigurationPlan p = cra.solve(mo, Arrays.asList(new Running(v), new Offline(n)));
            Assert.assertNull(p);
            SolvingStatistics stats = cra.getStatistics();
            Assert.assertNotNull(stats);
            Assert.assertTrue(stats.getSolutions().isEmpty());
        //       Assert.assertFalse(stats.hitTimeout());
    }

    /*@Test
    public void testGetStatistics() throws SchedulerException {
        Model mo = new DefaultModel();
        Mapping map = mo.getMapping();
        for (int i = 0; i < 10; i++) {
            Node n = mo.newNode();
            map.addOnlineNode(n);
            map.addRunningVM(mo.newVM(), n);
        }
        ChocoScheduler cra = new DefaultChocoScheduler();
        cra.doOptimize(true);
        cra.setTimeLimit(0);

        Assert.assertNull(cra.getStatistics());

        OptConstraint o = new OptConstraint() {
            @Override
            public String id() {
                return "foo";
            }
        };

        ChocoConstraint co =  new ChocoMapperTest.MockCConstraint() {

            public ChocoMapperTest.MockCConstraint(OptConstraint f) {}
            @Override
            public boolean inject(Parameters ps, ReconfigurationProblem rp) throws SchedulerException {
                return false;
            }

            @Override
            public Set<VM> getMisPlacedVMs(Model m) {
                return Collections.emptySet();
            }
        };
        cra.getMapper().register(o.getClass(), co.getClass());

        ReconfigurationPlan p = cra.solve(mo, Collections.<SatConstraint>emptyList(), o);
        Mapping res = p.getResult().getMapping();
        int nbRunning = 0;
        for (Node n : res.getOnlineNodes()) {
            if (!res.getRunningVMs(n).isEmpty()) {
                nbRunning++;
            }
        }
        Assert.assertEquals(nbRunning, 1);
        SolvingStatistics st = cra.getStatistics();
        Assert.assertEquals(st.getSolutions().get(0).getOptValue(), 1);
        Assert.assertEquals(st.getSolutions().size(), 1);
    }
*/
/*    @Test
    public void testSolvableRepair() throws SchedulerException {
        Model mo = new DefaultModel();
        final VM vm1 = mo.newVM();
        final VM vm2 = mo.newVM();
        final VM vm3 = mo.newVM();
        VM vm4 = mo.newVM();
        VM vm5 = mo.newVM();
        Node n1 = mo.newNode();
        Node n2 = mo.newNode();
        Node n3 = mo.newNode();
        Node n4 = mo.newNode();

        mo.on(n1, n2, n3).run(n1, vm1, vm4).run(n2, vm2).run(n3, vm3, vm5).get();

        //A satisfied constraint
        Fence c1 = new Fence(vm1, new HashSet<>(Arrays.asList(n1, n2)));

        //A constraint that is not satisfied. vm2 is misplaced
        Fence c2 = new Fence(vm2, new HashSet<>(Arrays.asList(n1, n3)));

        Set<SatConstraint> cstrs = new HashSet<SatConstraint>(Arrays.asList(c1, c2));
        mo = new DefaultModel();
        mo.on(n1, n2, n3).run(n1, vm1, vm4).run(n2, vm2).run(n3, vm3, vm5).get();
        ChocoScheduler cra = new DefaultChocoScheduler();
        OptConstraint o = new OptConstraint() {
            @Override
            public String id() {
                return "foo";
            }
        };

        ChocoConstraint co = new ChocoConstraint() {
            @Override
            public Set<VM> getMisPlacedVMs(Model m) {
                return new HashSet<>(Arrays.asList(vm2, vm3));
            }
        };
        cra.getMapper().register(o.getClass(), co.getClass());
        cra.doRepair(true);
        cra.doOptimize(false);

        //Solve a problem with the repair mode
        Assert.assertNotNull(cra.solve(mo, cstrs, o));
        SolvingStatistics st = cra.getStatistics();
        System.out.println(st);
        Assert.assertEquals(st.getNbManagedVMs(), 2); //vm2, vm3.
    }
*/
    @Test(expectedExceptions = {SchedulerException.class})
    public void testWithUnknownVMs() throws SchedulerException {
        Model mo = new DefaultModel();
        final VM vm1 = mo.newVM();
        final VM vm2 = mo.newVM();
        final VM vm3 = mo.newVM();
        VM vm4 = mo.newVM();
        VM vm5 = mo.newVM();
        VM vm6 = mo.newVM();
        Node n1 = mo.newNode();
        Node n2 = mo.newNode();
        Node n3 = mo.newNode();
        Node n4 = mo.newNode();
        mo.getMapping().on(n1, n2, n3).run(n1, vm1, vm4).run(n2, vm2).run(n3, vm3, vm5);
        SatConstraint cstr = mock(SatConstraint.class);
        when(cstr.getInvolvedVMs()).thenReturn(Arrays.asList(vm1, vm2, vm6));
        when(cstr.getInvolvedNodes()).thenReturn(Arrays.asList(n1, n4));
        ChocoScheduler cra = new DefaultChocoScheduler();
        cra.solve(mo, Collections.singleton(cstr));
    }

    /**
     * Issue #14
     *
     * @throws org.btrplace.scheduler.SchedulerException
     */
    @Test
    public void testNonHomogeneousIncrease() throws SchedulerException {
        ShareableResource cpu = new ShareableResource("cpu");
        ShareableResource mem = new ShareableResource("mem");
        Model mo = new DefaultModel();
        VM vm1 = mo.newVM();
        VM vm2 = mo.newVM();
        VM vm3 = mo.newVM();
        VM vm4 = mo.newVM();
        Node n1 = mo.newNode();
        Node n2 = mo.newNode();


        cpu.setCapacity(n1, 10);
        mem.setCapacity(n1, 10);
        cpu.setCapacity(n2, 10);
        mem.setCapacity(n2, 10);

        cpu.setConsumption(vm1, 5);
        mem.setConsumption(vm1, 4);

        cpu.setConsumption(vm2, 3);
        mem.setConsumption(vm2, 8);

        cpu.setConsumption(vm3, 5);
        cpu.setConsumption(vm3, 4);

        cpu.setConsumption(vm4, 4);
        cpu.setConsumption(vm4, 5);

        //vm1 requires more cpu resources, but fewer mem resources
        Preserve pCPU = new Preserve(vm1, "cpu", 7);
        Preserve pMem = new Preserve(vm1, "mem", 2);


        mo.getMapping().on(n1, n2)
                .run(n1, vm1)
                .run(n2, vm3, vm4)
                .ready(vm2);

        mo.attach(cpu);
        mo.attach(mem);

        ChocoScheduler cra = new DefaultChocoScheduler();
        cra.setMaxEnd(5);
        ReconfigurationPlan p = cra.solve(mo, Arrays.asList(pCPU, pMem,
                new Online(n1),
                new Running(vm2),
                new Ready(vm3)));
        Assert.assertNotNull(p);
    }

    /**
     * Remove the ready->running transition so the solving process will fail
     *
     * @throws org.btrplace.scheduler.SchedulerException
     */
    @Test
    public void testTransitionFactoryCustomisation() throws SchedulerException {
        ChocoScheduler cra = new DefaultChocoScheduler();
        TransitionFactory tf = cra.getTransitionFactory();
        VMTransitionBuilder b = tf.getBuilder(VMState.READY, VMState.RUNNING);
        Assert.assertTrue(tf.remove(b));
        Model mo = new DefaultModel();
        VM v = mo.newVM();
        mo.getMapping().addReadyVM(v);
        Assert.assertNull(cra.solve(mo, Collections.singletonList(new Running(v))));
    }

    @Test
    public void testOnSolutionHook() {
        ChocoScheduler cra = new DefaultChocoScheduler();
        Model mo = new DefaultModel();
        VM vm = mo.newVM();
        Node node = mo.newNode();
        mo.getMapping().on(node).run(node, vm);
        Instance i = new Instance(mo, Running.newRunning(vm), new MinMTTR());
        List<ReconfigurationPlan> onSolutions = new ArrayList<>();

        cra.addSolutionListener((rp, plan) -> onSolutions.add(plan));
        Assert.assertEquals(1, cra.solutionListeners().size());
        ReconfigurationPlan plan = cra.solve(i);
        Assert.assertEquals(1, onSolutions.size());
        Assert.assertEquals(plan, onSolutions.get(0));

    }
}
