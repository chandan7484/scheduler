/*
 * Copyright  2020 The BtrPlace Authors. All rights reserved.
 * Use of this source code is governed by a LGPL-style
 * license that can be found in the LICENSE.txt file.
 */

package org.btrplace.safeplace.testing;

/**
 * @author Fabien Hermenier
 */
@FunctionalInterface
public interface Tester  {

    TestCaseResult test(TestCase tc);
}
