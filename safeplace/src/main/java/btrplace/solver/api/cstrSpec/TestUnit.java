package btrplace.solver.api.cstrSpec;

import btrplace.json.JSONConverterException;
import btrplace.json.model.ModelConverter;
import btrplace.json.model.constraint.ConstraintsConverter;
import btrplace.model.Model;
import btrplace.model.constraint.Offline;
import btrplace.model.constraint.Online;
import btrplace.model.constraint.Root;
import btrplace.model.constraint.SatConstraint;
import btrplace.plan.ReconfigurationPlan;
import btrplace.solver.SolverException;
import btrplace.solver.choco.ChocoReconfigurationAlgorithm;
import btrplace.solver.choco.DefaultChocoReconfigurationAlgorithm;
import net.minidev.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Fabien Hermenier
 */
public class TestUnit {

    private Model mo;

    private SatConstraint cstr;

    private boolean isConsistent;

    public TestUnit(Model mo, SatConstraint cstr, boolean c) {
        this.mo = mo;
        this.cstr = cstr;
        this.isConsistent = c;
    }

    public static TestUnit fromJSON(JSONObject o) throws JSONConverterException {
        ConstraintsConverter cv = ConstraintsConverter.newBundle();
        ModelConverter mc = new ModelConverter();
        Model mo = mc.fromJSON((JSONObject) o.get("model"));
        cv.setModel(mo);
        SatConstraint cstr = (SatConstraint) cv.fromJSON((JSONObject) o.get("cstr"));
        boolean c = Boolean.parseBoolean(o.get("isConsistent").toString());
        return new TestUnit(mo, cstr, c);
    }

    public boolean isConsistent() {
        return this.isConsistent;
    }

    public Model getModel() {
        return mo;
    }

    public SatConstraint getSatConstraint() {
        return this.cstr;
    }

    public JSONObject toJSON() throws JSONConverterException {
        JSONObject jo = new JSONObject();
        ConstraintsConverter cv = ConstraintsConverter.newBundle();
        ModelConverter mc = new ModelConverter();
        cv.setModel(mo);
        jo.put("cstr", cv.toJSON(cstr));
        jo.put("model", mc.toJSON(mo));
        jo.put("consistent", isConsistent);
        return jo;
    }

    public TestResult verify() {
        TestResult.ErrorType rc = TestResult.ErrorType.bug, ri = TestResult.ErrorType.bug;
        try {
            rc = verifyChecker();
            ri = verifyImpl();
            return new TestResult(null/*mo*/, cstr, isConsistent, rc, ri);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new TestResult(null/*mo*/, cstr, isConsistent, rc, ri, ex.getMessage());
        } catch (Error err) {
            //err.printStackTrace();
            return new TestResult(null/*mo*/, cstr, isConsistent, rc, ri, err.getMessage());
        }
    }

    public TestResult.ErrorType verifyChecker() {
        if (cstr != null) {
            if (cstr.getChecker().endsWith(this.mo) == isConsistent) {
                return TestResult.ErrorType.succeed;
            }
            if (isConsistent) {
                return TestResult.ErrorType.falseNegative;
            }
            return TestResult.ErrorType.falsePositive;
        } else {
            //Checker for native cstr ? Possible ?
            return TestResult.ErrorType.bug;
        }
    }

    public TestResult.ErrorType verifyImpl() throws SolverException {
        ChocoReconfigurationAlgorithm cra = new DefaultChocoReconfigurationAlgorithm();
        //ROOt every VM
        List<SatConstraint> cstrs = new ArrayList<>();

        cstrs.add(cstr);
        cstrs.add(new Root(mo.getMapping().getRunningVMs()));
        if (mo.getMapping().getOnlineNodes().size() > 0) {
            cstrs.add(new Online(mo.getMapping().getOnlineNodes()));
        }
        if (mo.getMapping().getOfflineNodes().size() > 0) {
            cstrs.add(new Offline(mo.getMapping().getOfflineNodes()));
        }
        cra.doOptimize(true);
        ReconfigurationPlan p = cra.solve(mo, cstr != null ? Collections.singletonList(cstr) : Collections.<SatConstraint>emptyList());
        if (isConsistent) {
            if (p != null && p.getSize() == 0) {
                return TestResult.ErrorType.succeed;
            } else if (p == null) {
                return TestResult.ErrorType.falseNegative;
            }
            return TestResult.ErrorType.falseNegative;
        } else {
            if (p == null) {
                return TestResult.ErrorType.succeed;
            } else if (p.getSize() == 0) {
                return TestResult.ErrorType.falsePositive;
            }
            return TestResult.ErrorType.succeed;
        }
        //Need to root everything ?
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("cstr: ").append(cstr).append("\n");
        b.append("model:\n").append(mo).append("\n");
        b.append("consistent:").append(isConsistent);
        return b.toString();
    }
}

