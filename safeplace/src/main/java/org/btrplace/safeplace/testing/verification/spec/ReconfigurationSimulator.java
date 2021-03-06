/*
 * Copyright  2020 The BtrPlace Authors. All rights reserved.
 * Use of this source code is governed by a LGPL-style
 * license that can be found in the LICENSE.txt file.
 */

package org.btrplace.safeplace.testing.verification.spec;

import org.btrplace.model.Node;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.plan.event.Action;
import org.btrplace.plan.event.ActionVisitor;
import org.btrplace.plan.event.Allocate;
import org.btrplace.plan.event.AllocateEvent;
import org.btrplace.plan.event.BootNode;
import org.btrplace.plan.event.BootVM;
import org.btrplace.plan.event.ForgeVM;
import org.btrplace.plan.event.KillVM;
import org.btrplace.plan.event.MigrateVM;
import org.btrplace.plan.event.ResumeVM;
import org.btrplace.plan.event.ShutdownNode;
import org.btrplace.plan.event.ShutdownVM;
import org.btrplace.plan.event.SubstitutedVMEvent;
import org.btrplace.plan.event.SuspendVM;
import org.btrplace.safeplace.spec.prop.Proposition;
import org.btrplace.safeplace.spec.type.NodeStateType;
import org.btrplace.safeplace.spec.type.VMStateType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Simulate a reconfiguration by executing its action over a context.
 * Action are executed wrt. their timestamp.
 *
 * So at time {@code t}, the simulator first commits all the actions that end at {@code t},
 * then it starts all the actions beginning at {@code t}.
 * @author Fabien Hermenier
 */
public class ReconfigurationSimulator implements ActionVisitor {

  private final Context co;

  private final ReconfigurationPlan p;

  private final Map<Integer, List<Action>> starts;
  private final Map<Integer, List<Action>> ends;

  private List<Integer> timeStamps;

  private boolean start = false;

  /**
   * Make a new simulator.
   *
   * @param origin the original context
   * @param p      the plan to apply.
     */
    public ReconfigurationSimulator(Context origin, ReconfigurationPlan p) {
        co = origin;
        starts = new HashMap<>();
        ends = new HashMap<>();
        timeStamps = new ArrayList<>();
        this.p = p;
    }

    /**
     * Evaluate the proposition over a reconfiguration, at any timestamp.
     * @param prop the proposition to evaluate
     * @return the moment the proposition is not valid. {@code -1} if the proposition is correct
     */
    public int start(Proposition prop) {
        //sort actions by timestamp
        Set<Integer> s = new TreeSet<>(Comparator.comparingInt(a -> a));
        for (Action a : p.getActions()) {
            s.add(a.getStart());
            s.add(a.getEnd());
            if (!starts.containsKey(a.getStart())) {
                starts.put(a.getStart(), new ArrayList<>());
            }
            if (!ends.containsKey(a.getEnd())) {
                ends.put(a.getEnd(), new ArrayList<>());
            }

            starts.get(a.getStart()).add(a);
            ends.get(a.getEnd()).add(a);
        }
        timeStamps = s.stream().collect(Collectors.toList());

        for (Integer i : timeStamps) {
            List<Action> st = starts.get(i);
            if (st == null) {
                st = new ArrayList<>();
            }
            List<Action> ed = ends.get(i);
            if (ed == null) {
                ed = new ArrayList<>();
            }

            at(st, ed);
            Boolean res = prop.eval(co);
            if (!Boolean.TRUE.equals(res)) {
                return i;
            }
        }
        return -1;
    }

    private void at(List<Action> starts, List<Action> ends) {
        //Apply all the actions simultaneously, starting by the ending

        start = false;
        for (Action a : ends) {
            a.visit(this);
        }

        start = true;
        for (Action a : starts) {
            a.visit(this);
        }
    }


    //The visitors
    @Override
    public Object visit(Allocate a) {
        return null;
    }

    @Override
    public Object visit(AllocateEvent a) {
        return null;
    }

    @Override
    public Object visit(SubstitutedVMEvent a) {
        return null;
    }

    @Override
    public Object visit(BootNode a) {
        if (start) {
            co.getMapping().state(a.getNode(), NodeStateType.Type.BOOTING);
            return null;
        }
        co.getMapping().state(a.getNode(), NodeStateType.Type.ONLINE);
        return null;
    }

    @Override
    public Object visit(BootVM a) {
        if (start) {
            co.getMapping().state(a.getVM(), VMStateType.Type.BOOTING);
            co.getMapping().host(a.getVM(), a.getDestinationNode());
            return null;
        }
        co.getMapping().state(a.getVM(), VMStateType.Type.RUNNING);
        co.getMapping().activateOn(a.getVM(), a.getDestinationNode());
        return null;
    }

    @Override
    public Object visit(ForgeVM a) {
        return null;
    }

    @Override
    public Object visit(KillVM a) {
        if (start) {
            co.getMapping().state(a.getVM(), VMStateType.Type.TERMINATED);
            return null;
        }

        Node n = a.getNode();
        if (n != null) {
            co.getMapping().unhost(n, a.getVM());
            co.getMapping().desactivate(a.getVM());
            co.getMapping().state(a.getVM(), VMStateType.Type.TERMINATED);
        }

        return null;
    }

    @Override
    public Object visit(MigrateVM a) {
        if (start) {
            co.getMapping().host(a.getVM(), a.getDestinationNode());
            return null;
        }
        co.getMapping().state(a.getVM(), VMStateType.Type.RUNNING);
        co.getMapping().activateOn(a.getVM(), a.getDestinationNode());
        //No longer hosted on the source node
        //running on the new one.
        co.getMapping().unhost(a.getSourceNode(), a.getVM());
        return null;
    }

    @Override
    public Object visit(ResumeVM a) {
        if (start) {
            co.getMapping().state(a.getVM(), VMStateType.Type.RESUMING);
            co.getMapping().host(a.getVM(), a.getDestinationNode());
            return null;
        }
        co.getMapping().state(a.getVM(), VMStateType.Type.RUNNING);
        return null;
    }

    @Override
    public Object visit(ShutdownNode a) {
        if (start) {
            co.getMapping().state(a.getNode(), NodeStateType.Type.HALTING);
            return null;
        }
        co.getMapping().state(a.getNode(), NodeStateType.Type.OFFLINE);
        return null;
    }

    @Override
    public Object visit(ShutdownVM a) {
        if (start) {
            return null;
        }
        co.getMapping().state(a.getVM(), VMStateType.Type.READY);
        co.getMapping().unhost(a.getNode(), a.getVM());
        co.getMapping().desactivate(a.getVM());
        return null;
    }

    @Override
    public Object visit(SuspendVM a) {
        if (start) {
            co.getMapping().state(a.getVM(), VMStateType.Type.SUSPENDING);
            return null;
        }
        co.getMapping().state(a.getVM(), VMStateType.Type.SLEEPING);
        return null;
    }
}
