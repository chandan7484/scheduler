package btrplace.solver.api.cstrSpec.reducer;

import btrplace.plan.ReconfigurationPlan;
import btrplace.solver.api.cstrSpec.spec.term.Constant;
import btrplace.solver.api.cstrSpec.verification.TestCase2;

import java.util.List;

/**
 * Reduce a test case as much as possible.
 * <p/>
 * This is done as follow:
 * <ol>
 * <li>Un-necessary actions are removed</li>
 * <li>Un-necessary values in the constraints signature are removed</li>
 * <li>Un-necessary elements in the plan are removed</li>
 * </ol>
 *
 * @author Fabien Hermenier
 */
public class TestCaseReducer {

    private PlanReducer pr;

    private SignatureReducer sr;

    private ElementsReducer er;

    public TestCaseReducer() {
        pr = new PlanReducer();
        sr = new SignatureReducer();
        er = new ElementsReducer();
    }

    TestCase2 reduce(TestCase2 tc) throws Exception {
        ReconfigurationPlan reducedPlan = pr.reduce(tc.getPlan(), tc.getConstraint(), tc.getInputs());
        List<Constant> reducedParams = sr.reduce(reducedPlan, tc.getConstraint(), tc.getInputs());
        ReconfigurationPlan reducedElements = er.reduce(reducedPlan, tc.getConstraint(), reducedParams);
        return new TestCase2(reducedElements, tc.getConstraint(), reducedParams);
    }
}
