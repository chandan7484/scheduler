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

package org.btrplace.examples.migration;

import org.btrplace.examples.Example;
import org.btrplace.model.*;
import org.btrplace.model.constraint.Fence;
import org.btrplace.model.constraint.Offline;
import org.btrplace.model.constraint.SatConstraint;
import org.btrplace.model.constraint.migration.Precedence;
import org.btrplace.model.constraint.migration.Sync;
import org.btrplace.model.view.ShareableResource;
import org.btrplace.model.view.network.Network;
import org.btrplace.model.view.network.Switch;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.DefaultChocoScheduler;
import org.btrplace.scheduler.choco.DefaultParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Vincent Kherbache
 */
public class AdvancedScheduling implements Example {

    @Override
    public boolean run() {

        // New default model
        Model mo = new DefaultModel();
        Mapping ma = mo.getMapping();

        // Create and boot 4 source nodes and 2 destination nodes
        Node srcNode1 = mo.newNode(), srcNode2 = mo.newNode(), srcNode3 = mo.newNode(), srcNode4 = mo.newNode(),
                dstNode1 = mo.newNode(), dstNode2 = mo.newNode();
        ma.addOnlineNode(srcNode1);
        ma.addOnlineNode(srcNode2);
        ma.addOnlineNode(srcNode3);
        ma.addOnlineNode(srcNode4);
        ma.addOnlineNode(dstNode1);
        ma.addOnlineNode(dstNode2);

        // Attach a network view
        Network net = new Network();
        mo.attach(net);
        // Connect the nodes through a main non-blocking switch
        // The destination nodes have twice the bandwidth of source nodes
        Switch swMain = net.newSwitch();
        net.connect(1000, swMain, srcNode1, srcNode2, srcNode3, srcNode4);
        net.connect(2000, swMain, dstNode1, dstNode2);
        
        // Create and host 1 VM per source node
        VM vm1 = mo.newVM(), vm2 = mo.newVM(), vm3 = mo.newVM(), vm4 = mo.newVM();
        ma.addRunningVM(vm1, srcNode1);
        ma.addRunningVM(vm2, srcNode2);
        ma.addRunningVM(vm3, srcNode3);
        ma.addRunningVM(vm4, srcNode4);

        // Attach CPU and Mem resource views and assign nodes capacity and VMs consumption
        int mem_vm = 8, cpu_vm = 4, mem_src = 8, cpu_src = 4, mem_dst = 16, cpu_dst = 8;
        ShareableResource rcMem = new ShareableResource("mem", 0, 0), rcCPU = new ShareableResource("cpu", 0, 0);
        mo.attach(rcMem);
        mo.attach(rcCPU);
        // VMs
        rcMem.setConsumption(vm1, mem_vm).setConsumption(vm2, mem_vm).setConsumption(vm3, mem_vm).
                setConsumption(vm4, mem_vm);
        rcCPU.setConsumption(vm1, cpu_vm).setConsumption(vm2, cpu_vm).setConsumption(vm3, cpu_vm).
                setConsumption(vm4, cpu_vm);
        // Nodes
        rcMem.setCapacity(srcNode1, mem_src).setCapacity(srcNode2, mem_src).setCapacity(srcNode3, mem_src).
                setCapacity(srcNode4, mem_src).setCapacity(dstNode1, mem_dst).setCapacity(dstNode2, mem_dst);
        rcCPU.setCapacity(srcNode1, cpu_src).setCapacity(srcNode2, cpu_src).setCapacity(srcNode3, cpu_src).
                setCapacity(srcNode4, cpu_src).setCapacity(dstNode1, cpu_dst).setCapacity(dstNode2, cpu_dst);

        // Set VM attributes 'memory used', 'hot dirty page size', 'hot dirty page duration' and 'cold dirty pages rate'
        int vm_hds = 46, vm_hdd = 2; double vm_cdr = 23.6;
        // vm1 is an 'idle' VM (with no special memory activity) but still consumes 2 GiB of memory
        mo.getAttributes().put(vm1, "memUsed", 2000);
        // vm2 is an 'idle' VM (with no special memory activity) but still consumes 4 GiB of memory
        mo.getAttributes().put(vm2, "memUsed", 4000);
        // vm3 consumes 4 GiB memory and has a memory intensive workload equivalent to "stress --vm 1000 --bytes 50K"
        mo.getAttributes().put(vm3, "memUsed", 4000);
        mo.getAttributes().put(vm3, "hotDirtySize", vm_hds);
        mo.getAttributes().put(vm3, "hotDirtyDuration", vm_hdd);
        mo.getAttributes().put(vm3, "coldDirtyRate", vm_cdr);
        // vm4 consumes 6 GiB memory and has a memory intensive workload equivalent to "stress --vm 1000 --bytes 50K"
        mo.getAttributes().put(vm4, "memUsed", 6000);
        mo.getAttributes().put(vm4, "hotDirtySize", vm_hds);
        mo.getAttributes().put(vm4, "hotDirtyDuration", vm_hdd);
        mo.getAttributes().put(vm4, "coldDirtyRate", vm_cdr);

        // Create constraints
        List<SatConstraint> cstrs = new ArrayList<>();
        // We wan to shutdown the source nodes
        cstrs.add(new Offline(srcNode1));
        cstrs.add(new Offline(srcNode2));
        cstrs.add(new Offline(srcNode3));
        cstrs.add(new Offline(srcNode4));
        // We must force the destination node of the VMs we want to synchronize
        cstrs.add(new Fence(vm1, Collections.singleton(dstNode1))); // Force vm1 to destination node 1
        cstrs.add(new Fence(vm2, Collections.singleton(dstNode2))); // Force vm2 to destination node 2
        
        // Scheduling constraints:
        // We want to first migrate the high memory usage VMs (longer migrations: vm4 -> vm3 -> vm2)
        cstrs.add(new Precedence(vm4, vm3));
        cstrs.add(new Precedence(vm3, vm2));
        // We want vm1 and vm2 to terminate their migration at the same time (so at the end of the reconfiguration plan)
        cstrs.add(new Sync(vm1, vm2));

        // Set parameter: /!\ Optimize the migrations scheduling /!\
        DefaultParameters ps = new DefaultParameters();
        ps.doOptimizeMigScheduling(true);

        // Try to solve, and show the computed plan
        try {
            ReconfigurationPlan p = new DefaultChocoScheduler(ps).solve(mo, cstrs);
            System.out.println(p);
            System.out.flush();
            
        } catch (SchedulerException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
