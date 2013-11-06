package btrplace.solver.api.cstrSpec;

import btrplace.model.Model;
import btrplace.solver.api.cstrSpec.type.Type;

import java.util.Collections;
import java.util.Set;

/**
 * @author Fabien Hermenier
 */
public class Variable implements Term {

    private Type t;

    private String n;

    private Object val = null;
    public Variable (String n, Type t) {
        this.t = t;
        this.n = n;
    }

    //@Override
    public Set<Object> domain() {
        if (val == null) {
            return t.domain();
        }
        return Collections.singleton(val);
    }

    @Override
    public String toString() {
        if (val != null) {
            return val.toString();
        }
        return label();
    }

    public Type type() {
        return t;
    }

    public String label() {
        //if (val == null) {
            return n;
        /*}
        return n + "="+val;*/
    }

    public boolean set(Object v) {
        val = v;
        return true;
    }

    public void unset() {
        val = null;
    }

    @Override
    public Object getValue(Model mo) {
        return val;
    }

    @Override
    public Term plus(Term t2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Term minus(Term t2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Term mult(Term t2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Term div(Term t2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Term inter(Term t2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Term union(Term t2) {
        throw new UnsupportedOperationException();
    }
}
