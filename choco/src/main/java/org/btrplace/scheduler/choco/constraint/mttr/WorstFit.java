/*
 * Copyright  2020 The BtrPlace Authors. All rights reserved.
 * Use of this source code is governed by a LGPL-style
 * license that can be found in the LICENSE.txt file.
 */

package org.btrplace.scheduler.choco.constraint.mttr;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.scheduler.choco.ReconfigurationProblem;
import org.btrplace.scheduler.choco.constraint.mttr.load.GlobalLoadEstimator;
import org.btrplace.scheduler.choco.view.CShareableResource;
import org.btrplace.scheduler.choco.view.ChocoView;
import org.btrplace.scheduler.choco.view.Packing;
import org.btrplace.scheduler.choco.view.VectorPacking;
import org.chocosolver.memory.IStateInt;
import org.chocosolver.solver.search.strategy.selectors.values.IntValueSelector;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Placement heuristic implementing a worst fit.
 * @author Fabien Hermenier
 */
public class WorstFit implements IntValueSelector {

  private final boolean stayFirst;

  private final Map<IntVar, VM> vmMap;

  private final ReconfigurationProblem rp;

  private final GlobalLoadEstimator globalLoad;

  private final List<CShareableResource> rcs;

  private final VectorPacking packing;

  private final TIntObjectMap<int[]> usages;

  private final TIntObjectMap<int[]> capacities;

  /**
   * New heuristic.
   * Will try to make the VM stay on their current node in prior if possible
   *
   * @param vmMap the VM to placement variable mapping
   * @param rp    the CSP to solve
   * @param load  the load estimator for the node.
   */
  public WorstFit(Map<IntVar, VM> vmMap, ReconfigurationProblem rp, GlobalLoadEstimator load) {
    this(vmMap, rp, load, true);
  }

  /**
   * New heuristic.
   *
   * @param vmMap     the VM to placement variable mapping
   * @param rp        the CSP to solve
   * @param load      the load estimator for the node.
   * @param stayFirst {@code true} to try to let the VM on place first if possible
   */
  public WorstFit(Map<IntVar, VM> vmMap, ReconfigurationProblem rp, GlobalLoadEstimator load, boolean stayFirst) {
    this.stayFirst = stayFirst;
    this.vmMap = vmMap;
    globalLoad = load;
    packing = (VectorPacking) rp.getRequiredView(Packing.VIEW_ID);
    this.rp = rp;
    rcs = new ArrayList<>();
    for (String s : rp.getViews()) {
      ChocoView cv = rp.getRequiredView(s);
      if (cv instanceof CShareableResource) {
        rcs.add((CShareableResource) cv);
      }
    }

    usages = new TIntObjectHashMap<>();
    capacities = new TIntObjectHashMap<>();
    for (Node node : rp.getNodes()) {
      int nIdx = rp.getNode(node);
      int[] capa = new int[rcs.size()];
      for (int i = 0; i < rcs.size(); i++) {
        capa[i] += (int) (rcs.get(i).getFutureNodeCapacity(nIdx) * rcs.get(i).getOverbookRatio(nIdx));
      }
      capacities.put(nIdx, capa);

    }
  }

  @Override
  public int selectValue(IntVar v) {
    VM vm = vmMap.get(v);
    int vmId = rp.getVM(vm);
    Node host = rp.getSourceModel().getMapping().getVMLocation(vm);
    int nodeId = rp.getNode(host);
    if (stayFirst && canStay(nodeId, vmId)) {
      return nodeId;
    }

    //Get the load
    int leastId = v.getLB();
    double minLoad = 2;
    IStateInt[] loads = new IStateInt[rcs.size()];
    for (int nId = v.getLB(); nId <= v.getUB(); nId = v.nextValue(nId)) {
      for (int d = 0; d < rcs.size(); d++) {
        loads[d] = packing.assignedLoad()[d][nId];
      }
      double global = loadWith(nId, loads, vmId);

      if (global < minLoad) {
        leastId = nId;
        minLoad = global;
      }
    }
    return leastId;
  }

  private IStateInt[] load(int nId) {
    IStateInt[] loads = new IStateInt[rcs.size()];
    for (int d = 0; d < rcs.size(); d++) {
      loads[d] = packing.assignedLoad()[d][nId];
    }
    return loads;
  }

  private int[] usage(int vId) {
    int[] usage = usages.get(vId);
    if (usage == null) {
      usage = new int[rcs.size()];
      for (int i = 0; i < rcs.size(); i++) {
        usage[i] += rcs.get(i).getFutureVMAllocation(vId);
      }
      usages.put(vId, usage);
    }
    return usage;

  }

  private double loadWith(int nId, IStateInt[] loads, int vmId) {
    int[] capas = capacities.get(nId);
    double[] normalised = new double[capas.length];
    int[] usage = usage(vmId);
    for (int i = 0; i < capas.length; i++) {
      normalised[i] = (1.0d * loads[i].get() + usage[i]) / capas[i];
    }
    return globalLoad.getLoad(normalised);
  }

  /**
   * Check if a VM can stay on its current node.
   *
   * @param hostId the node identifier. Negative if the VM is not running.
   * @param vmId   the VM identifier.
   * @return {@code true} iff the VM can stay
   */
  private boolean canStay(int hostId, int vmId) {
    if (hostId < 0) {
      return false;
    }
    // The VM is running for sure.
    if (!rp.getVMActions().get(vmId).getDSlice().getHoster().contains(hostId)) {
      return false;
    }
    IStateInt[] loads = load(hostId);
    return loadWith(hostId, loads, vmId) <= 1.0;
  }
}
