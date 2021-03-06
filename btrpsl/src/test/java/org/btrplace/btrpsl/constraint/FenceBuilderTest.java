/*
 * Copyright  2020 The BtrPlace Authors. All rights reserved.
 * Use of this source code is governed by a LGPL-style
 * license that can be found in the LICENSE.txt file.
 */

package org.btrplace.btrpsl.constraint;

import org.btrplace.btrpsl.ScriptBuilder;
import org.btrplace.btrpsl.ScriptBuilderException;
import org.btrplace.model.DefaultModel;
import org.btrplace.model.VM;
import org.btrplace.model.constraint.Fence;
import org.btrplace.model.constraint.SatConstraint;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Unit tests for {@link FenceBuilder}.
 *
 * @author Fabien Hermenier
 */
@Test
public class FenceBuilderTest {

    @DataProvider(name = "badFences")
    public Object[][] getBadSignatures() {
        return new String[][]{
                new String[]{">>fence(@N1,@N[1..10]);"},
                new String[]{"fence({@N1},@N[1..10]);"},
                new String[]{"fence({VM1},VM[1..5]);"},
                new String[]{"fence({VM1},@N[1..10],VM1);"},
                new String[]{"fence({VM1},@N[1..10],@N1);"},
                new String[]{"fence({VM1},{@N[1..5], @N[6..10]});"},
                new String[]{"fence({},@N[1..5]);"},
                new String[]{"fence(VM1,{});"},
                new String[]{"fence({},{});"},
        };
    }

    @Test(dataProvider = "badFences", expectedExceptions = {ScriptBuilderException.class})
    public void testBadSignatures(String str) throws ScriptBuilderException {
        ScriptBuilder b = new ScriptBuilder(new DefaultModel());
        try {
            b.build("namespace test; VM[1..10] : tiny;\n@N[1..20] : defaultNode;\n" + str);
        } catch (ScriptBuilderException ex) {
            System.out.println(ex.getMessage());
            throw ex;
        }
    }

    @DataProvider(name = "goodFences")
    public Object[][] getGoodSignatures() {
        return new Object[][]{
                new Object[]{">>fence(VM1,{@N1});", 1, 1, false},
                new Object[]{"fence({VM1},{@N1});", 1, 1, true},
                new Object[]{">>fence(VM1,@N[1..10]);", 1, 10, false},
                new Object[]{"fence({VM1,VM2},@N[1..10]);", 2, 10, true},
        };
    }

    @Test(dataProvider = "goodFences")
    public void testGoodSignatures(String str, int nbVMs, int nbNodes, boolean c) throws Exception {
        ScriptBuilder b = new ScriptBuilder(new DefaultModel());
        Set<SatConstraint> cstrs = b.build("namespace test; VM[1..10] : tiny;\n @N[1..20] : defaultNode;\n" + str).getConstraints();
        Assert.assertEquals(cstrs.size(), nbVMs);
        Set<VM> vms = new HashSet<>();
        for (SatConstraint x : cstrs) {
            Assert.assertTrue(x instanceof Fence);
            Assert.assertEquals(x.getInvolvedNodes().size(), nbNodes);
            Assert.assertTrue(vms.addAll(x.getInvolvedVMs()));
            Assert.assertEquals(x.isContinuous(), c);
        }
    }
}
