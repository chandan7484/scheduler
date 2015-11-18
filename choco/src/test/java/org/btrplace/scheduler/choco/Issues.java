/*
 * Copyright (c) 2014 University Nice Sophia Antipolis
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

package org.btrplace.scheduler.choco;

import org.btrplace.json.model.InstanceConverter;
import org.btrplace.model.*;
import org.btrplace.model.constraint.Fence;
import org.btrplace.model.constraint.Offline;
import org.btrplace.model.constraint.SatConstraint;
import org.btrplace.model.constraint.Spread;
import org.btrplace.model.view.ShareableResource;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.constraint.mttr.CMinMTTR;
import org.btrplace.scheduler.choco.extensions.ChocoUtils;
import org.btrplace.scheduler.choco.transition.NodeTransition;
import org.btrplace.scheduler.choco.transition.VMTransition;
import org.chocosolver.solver.Cause;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.IntConstraintFactory;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.VF;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

/**
 * Unit tests related to the opened issues
 *
 * @author Fabien Hermenier
 */
public class Issues {

    /**
     * Another test related to issue #5.
     *
     * @throws org.btrplace.scheduler.SchedulerException
     */
    @Test
    public void testIssue5a() throws SchedulerException, ContradictionException {

        Model model = new DefaultModel();
        Node n1 = model.newNode();
        Node n2 = model.newNode();
        Node n3 = model.newNode();
        VM vm1 = model.newVM();
        VM vm2 = model.newVM();
        ShareableResource resources = new ShareableResource("vcpu", 1, 1);
        resources.setCapacity(n1, 2);
        resources.setCapacity(n2, 2);

        Mapping map = new MappingFiller(model.getMapping()).on(n1, n2).off(n3).run(n1, vm1, vm2).get();
        model.attach(resources);

        ReconfigurationProblem rp = new DefaultReconfigurationProblemBuilder(model)
                .build();

        Solver solver = rp.getSolver();
        IntVar[] VMsOnAllNodes = rp.getNbRunningVMs();

        int NUMBER_OF_NODE = map.getAllNodes().size();

        // Each element is the number of VMs on each node
        IntVar[] vmsOnInvolvedNodes = new IntVar[NUMBER_OF_NODE];

        BoolVar[] busy = new BoolVar[NUMBER_OF_NODE];

        rp.getEnd().updateUpperBound(10, Cause.Null);
        int i = 0;
        int maxVMs = rp.getSourceModel().getMapping().getAllVMs().size();
        for (Node n : map.getAllNodes()) {
            vmsOnInvolvedNodes[i] = VF.bounded("nVMs", -1, maxVMs, rp.getSolver());
            IntVar state = rp.getNodeAction(n).getState();
            // If the node is offline -> the temporary variable is -1, otherwise, it equals the number of VMs on that node
            IntVar[] c = new IntVar[]{VF.fixed(-1, rp.getSolver()), VMsOnAllNodes[rp.getNode(n)],
                    state, vmsOnInvolvedNodes[i]};
            Constraint elem = IntConstraintFactory.element(vmsOnInvolvedNodes[i], new IntVar[]{VF.fixed(-1, solver), VMsOnAllNodes[rp.getNode(n)]}, state, 0);
            //solver.post(new ElementV(c, 0, solver.getEnvironment()));
            solver.post(elem);

            // IF the node is online and hosting VMs -> busy = 1.
            busy[i] = VF.bool("busy" + n, rp.getSolver());
            ChocoUtils.postIfOnlyIf(solver, busy[i], IntConstraintFactory.arithm(vmsOnInvolvedNodes[i], ">=", 1));
            i++;
        }

        // idle is equals the number of vmsOnInvolvedNodes with value 0. (The node without VM)
        IntVar idle = VF.bounded("Nidles", 0, NUMBER_OF_NODE, solver);
        solver.post(IntConstraintFactory.count(0, vmsOnInvolvedNodes, idle));
        // idle should be less than Amount for MaxSN (0, in this case)
        solver.post(IntConstraintFactory.arithm(idle, "<=", 0));

        // Extract all the state of the involved nodes (all nodes in this case)
        IntVar[] states = new IntVar[NUMBER_OF_NODE];
        int j = 0;
        for (Node n : map.getAllNodes()) {
            states[j++] = rp.getNodeAction(n).getState();
        }

        // In case the number of VMs is inferior to the number of online nodes, some nodes have to shutdown
        // to satisfy the constraint. This could be express as:
        // The addition of the idle nodes and busy nodes should be equals the number of online nodes.
        IntVar sumStates = VF.bounded("sumStates", 0, 1000, solver);
        solver.post(IntConstraintFactory.sum(states, sumStates));
        IntVar sumBusy = VF.bounded("sumBusy", 0, 1000, solver);
        solver.post(IntConstraintFactory.sum(states, sumBusy));
        IntVar sumIB = VF.bounded("ib", 0, 1000, solver);
        VF.task(sumBusy, idle, sumIB);
        //solver.post(IntConstraintFactory.arithm(sumBusy, "+", idle));
        solver.post(IntConstraintFactory.arithm(sumStates, "=", sumIB));//solver.eq(sumStates, sumIB));

        ReconfigurationPlan plan = rp.solve(0, false);
        Assert.assertNotNull(plan);
    }

    /**
     * Test a suspicious bug in issue #5
     */
    @Test
    public void testIssue5b() throws SchedulerException {
        Model model = new DefaultModel();
        Node n1 = model.newNode();
        Node n2 = model.newNode();
        Node n3 = model.newNode();
        VM vm1 = model.newVM();
        VM vm2 = model.newVM();
        VM vm3 = model.newVM();
        VM vm4 = model.newVM();

        Mapping map = new MappingFiller(model.getMapping()).on(n1, n2, n3)
                .run(n2, vm1, vm2, vm3, vm4).get();


        ReconfigurationProblem rp = new DefaultReconfigurationProblemBuilder(model)
                .build();

        IntVar[] nodes_state = rp.getNbRunningVMs();
        IntVar[] nodeVM = new IntVar[map.getAllNodes().size()];

        int i = 0;

        for (Node n : map.getAllNodes()) {
            nodeVM[i++] = nodes_state[rp.getNode(n)];
        }
        Solver solver = rp.getSolver();
        IntVar idle = VF.bounded("Nidles", 0, map.getAllNodes().size(), solver);

        solver.post(IntConstraintFactory.count(0, nodeVM, idle));
        // Amount of maxSpareNode =  1
        solver.post(IntConstraintFactory.arithm(idle, "<=", 1));

        ReconfigurationPlan plan = rp.solve(0, false);
        Assert.assertNotNull(plan);
    }

    /**
     * Test a suspicious bug in issue #5
     */
    @Test
    public void testIssue5c() throws SchedulerException, ContradictionException {

        Model model = new DefaultModel();
        Node n1 = model.newNode();
        Node n2 = model.newNode();
        Node n3 = model.newNode();
        VM vm1 = model.newVM();
        VM vm2 = model.newVM();
        VM vm3 = model.newVM();
        VM vm4 = model.newVM();


        Mapping map = new MappingFiller(model.getMapping()).on(n1, n2, n3)
                .run(n2, vm1, vm2, vm3, vm4).get();

        ReconfigurationProblem rp = new DefaultReconfigurationProblemBuilder(model)
                .build();

        IntVar[] nodes_state = rp.getNbRunningVMs();
        IntVar[] nodeVM = new IntVar[map.getAllNodes().size()];

        int i = 0;

        for (Node n : map.getAllNodes()) {
            nodeVM[i++] = nodes_state[rp.getNode(n)];
            //rp.getNodeAction(n).getState().setVal(1);
        }
        Solver solver = rp.getSolver();
        IntVar idle = VF.bounded("Nidles", 0, map.getAllNodes().size(), solver);

        solver.post(IntConstraintFactory.count(0, nodeVM, idle));
        solver.post(IntConstraintFactory.arithm(idle, "<=", 1));
        ReconfigurationPlan plan = rp.solve(0, false);
        Assert.assertNotNull(plan);
    }

    @Test
    public void testIssue10() throws SchedulerException, ContradictionException {
        Model model = new DefaultModel();
        Node n1 = model.newNode();
        Node n2 = model.newNode();
        Node n3 = model.newNode();
        VM vm1 = model.newVM();
        VM vm2 = model.newVM();

        Mapping map = new MappingFiller(model.getMapping()).on(n1, n2).off(n3).run(n1, vm1, vm2).get();
        //model.attach(resources);
        ReconfigurationProblem rp = new DefaultReconfigurationProblemBuilder(model).build();
        Solver solver = rp.getSolver();
        rp.getNodeAction(n3).getState().instantiateTo(1, Cause.Null);  // n3 goes online
        solver.post(IntConstraintFactory.arithm(rp.getEnd(), "<=", 10));
        int NUMBER_OF_NODE = map.getAllNodes().size();
        // Extract all the state of the involved nodes (all nodes in this case)
        IntVar[] VMsOnAllNodes = rp.getNbRunningVMs();
        // Each element is the number of VMs on each node
        IntVar[] vmsOnInvolvedNodes = new IntVar[NUMBER_OF_NODE];
        BoolVar[] idles = new BoolVar[NUMBER_OF_NODE];
        int i = 0;
        int maxVMs = rp.getSourceModel().getMapping().getAllVMs().size();
        List<Constraint> elms = new ArrayList<>();
        for (Node n : map.getAllNodes()) {
            vmsOnInvolvedNodes[i] = VF.bounded("nVMs" + n, -1, maxVMs, solver);
            IntVar state = rp.getNodeAction(n).getState();
            // If the node is offline -> the temporary variable is 1, otherwise, it equals the number of VMs on that node
            IntVar[] c = new IntVar[]{VF.fixed(-1, solver), VMsOnAllNodes[rp.getNode(n)],
                    state, vmsOnInvolvedNodes[i]};
            //new ElementV(c, 0, solver.getEnvironment());
            Constraint elem = IntConstraintFactory.element(vmsOnInvolvedNodes[i], new IntVar[]{VF.fixed(-1, solver), VMsOnAllNodes[rp.getNode(n)]}, state, 0);
            elms.add(elem);
            solver.post(elem);
            // IF number of VMs on a node is 0 -> Idle
            idles[i] = VF.bool("idle" + n, solver);
            ChocoUtils.postIfOnlyIf(solver, idles[i], IntConstraintFactory.arithm(vmsOnInvolvedNodes[i], "=", 0));
            i++;
        }
        IntVar sum = VF.bounded("sum", 0, 1000, solver);
        solver.post(IntConstraintFactory.sum(idles, sum));
        // idle should be less than Amount for MaxSN (0, in this case)
        solver.post(IntConstraintFactory.arithm(sum, "=", 0));
        System.err.flush();
        CMinMTTR obj = new CMinMTTR();
        obj.inject(new DefaultParameters(), rp);
        //System.err.println(solver.toString());
        //ChocoLogging.setLoggingMaxDepth(100);
        ReconfigurationPlan plan = rp.solve(0, false);
        Assert.assertNotNull(plan);
        System.out.println(plan);
        System.out.println(plan.getResult());
    }

    /**
     * Unit test derived from Issue 16.
     *
     * @throws org.btrplace.scheduler.SchedulerException
     */
    @Test
    public void test16b() throws SchedulerException {
        Model model = new DefaultModel();
        Node n1 = model.newNode();
        Node n2 = model.newNode();
        Node n3 = model.newNode();
        Node n4 = model.newNode();
        VM vm1 = model.newVM();
        VM vm2 = model.newVM();
        VM vm3 = model.newVM();
        VM vm4 = model.newVM();
        VM vm5 = model.newVM();
        VM vm6 = model.newVM();


        Mapping map = new MappingFiller(model.getMapping()).on(n1, n2, n3, n4)
                .run(n1, vm1, vm2)
                .run(n2, vm3, vm4)
                .run(n3, vm5, vm6)
                .get();

        Set<SatConstraint> ctrsC = new HashSet<>();
        Set<VM> vms1 = new HashSet<>(Arrays.asList(vm1, vm3, vm5));
        Set<VM> vms2 = new HashSet<>(Arrays.asList(vm2, vm4, vm6));

        ctrsC.add(new Spread(vms1));
        ctrsC.add(new Spread(vms2));
        ctrsC.add(new Fence(vm3, Collections.singleton(n1)));

        Offline off = new Offline(n2);
        ctrsC.add(off);
        ChocoScheduler cra = new DefaultChocoScheduler();
        ReconfigurationPlan dp = cra.solve(model, ctrsC);
    }

    @Test
    public void issue19() {
        Model m = new DefaultModel();
        ShareableResource cpu = new ShareableResource("cpu", 4, 1);
        Node n = m.newNode();
        Node n2 = m.newNode();
        m.attach(cpu);
        Assert.assertEquals(cpu.sumCapacities(Arrays.asList(n, n2), true), 8);
    }

    @Test
    public void issue33() throws SchedulerException, ContradictionException {
        Model mo = new DefaultModel();
        Node n = mo.newNode();
        VM v = mo.newVM();
        mo.getMapping().addOnlineNode(n);
        mo.getMapping().addRunningVM(v, n);

        ReconfigurationProblem rp = new DefaultReconfigurationProblemBuilder(mo)
                .setNextVMsStates(Collections.<VM>emptySet(),
                        Collections.<VM>emptySet(),
                        Collections.singleton(v),
                        Collections.<VM>emptySet())
                .build();

        NodeTransition na = rp.getNodeAction(n);
        na.getStart().instantiateTo(0, Cause.Null);
        na.getEnd().instantiateTo(1, Cause.Null);
        VMTransition vma = rp.getVMAction(v);
        vma.getStart().instantiateTo(0, Cause.Null);
        vma.getEnd().instantiateTo(1, Cause.Null);

        ReconfigurationPlan plan = rp.solve(0, false);
        Assert.assertEquals(plan, null);
    }

    @Test
    public void issue72() throws Exception {
        String input = "{\"model\":{\"mapping\":{\"readyVMs\":[],\"onlineNodes\":{\"0\":{\"sleepingVMs\":[],\"runningVMs\":[9,8,7,6,5,4,3,2,1,0]},\"1\":{\"sleepingVMs\":[],\"runningVMs\":[19,18,17,16,15,14,13,12,11,10]}},\"offlineNodes\":[]},\"attributes\":{\"nodes\":{},\"vms\":{}},\"views\":[{\"defConsumption\":0,\"nodes\":{\"0\":32768,\"1\":32768},\"rcId\":\"mem\",\"id\":\"shareableResource\",\"defCapacity\":8192,\"vms\":{\"11\":1024,\"12\":1024,\"13\":1024,\"14\":1024,\"15\":1024,\"16\":1024,\"17\":1024,\"18\":1024,\"19\":1024,\"0\":1024,\"1\":1024,\"2\":1024,\"3\":1024,\"4\":1024,\"5\":1024,\"6\":1024,\"7\":1024,\"8\":1024,\"9\":1024,\"10\":1024}},{\"defConsumption\":0,\"nodes\":{\"0\":700,\"1\":700},\"rcId\":\"cpu\",\"id\":\"shareableResource\",\"defCapacity\":8000,\"vms\":{\"11\":0,\"12\":0,\"13\":0,\"14\":0,\"15\":0,\"16\":70,\"17\":0,\"18\":60,\"19\":0,\"0\":100,\"1\":0,\"2\":0,\"3\":0,\"4\":0,\"5\":0,\"6\":0,\"7\":0,\"8\":90,\"9\":0,\"10\":0}}]},\"constraints\":[],\"objective\":{\"id\":\"minimizeMTTR\"}}";
        InstanceConverter ic = new InstanceConverter();
        Instance i = ic.fromJSON(input);
        ChocoScheduler s = new DefaultChocoScheduler();
        s.setVerbosity(3);
        s.setTimeLimit(-1);
        s.doOptimize(true);
        i.getModel().detach(i.getModel().getView(ShareableResource.VIEW_ID_BASE + "mem"));
        i.getModel().detach(i.getModel().getView(ShareableResource.VIEW_ID_BASE + "cpu"));
        List<SatConstraint> cstrs = new ArrayList<>();
        ReconfigurationPlan p = s.solve(i.getModel(), cstrs, i.getOptConstraint());
        System.out.println(p);

        Assert.assertTrue(p.getActions().isEmpty());
    }

    @Test
    public void issue72b() throws SchedulerException {
        Model mo = new DefaultModel();
        Mapping ma = mo.getMapping();
        ShareableResource rcCPU = new ShareableResource("cpu", 2, 0);
        mo.attach(rcCPU);
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            nodes.add(mo.newNode());
            ma.addOnlineNode(nodes.get(i));
        }
        List<VM> vms = new ArrayList<>();
        for (int i = 0; i < 1; i++) {
            VM v = mo.newVM();
            vms.add(v);
            ma.addRunningVM(v, nodes.get(0));
        }
        for (int i = 0; i < 2; i++) {
            VM v = mo.newVM();
            vms.add(v);
            ma.addRunningVM(v, nodes.get(1));
        }
        DefaultParameters ps = new DefaultParameters();
        ps.setVerbosity(4);//.doRepair(true).doOptimize(true);
        ReconfigurationPlan p = new DefaultChocoScheduler(ps).solve(mo, new ArrayList<>());
        Assert.assertNotNull(p);
    }
}
