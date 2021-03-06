/*
 * Copyright  2020 The BtrPlace Authors. All rights reserved.
 * Use of this source code is governed by a LGPL-style
 * license that can be found in the LICENSE.txt file.
 */

package org.btrplace.safeplace.spec.term;

import org.btrplace.safeplace.spec.type.Type;
import org.btrplace.safeplace.testing.verification.spec.Context;

/**
 * @author Fabien Hermenier
 */
public class ProtectedTerm<T> implements Term<T> {

  private final Term<T> t;

    public ProtectedTerm(Term<T> t) {
        this.t = t;
    }

    @Override
    public T eval(Context mo, Object... args) {
        return t.eval(mo);
    }

    @Override
    public Type type() {
        return t.type();
    }


    @Override
    public String toString() {
        return "(" + t.toString() + ")";
    }
}
