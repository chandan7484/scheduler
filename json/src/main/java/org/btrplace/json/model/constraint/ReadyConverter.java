/*
 * Copyright  2020 The BtrPlace Authors. All rights reserved.
 * Use of this source code is governed by a LGPL-style
 * license that can be found in the LICENSE.txt file.
 */

package org.btrplace.json.model.constraint;

import net.minidev.json.JSONObject;
import org.btrplace.json.JSONConverterException;
import org.btrplace.json.JSONs;
import org.btrplace.model.Model;
import org.btrplace.model.constraint.Ready;

import static org.btrplace.json.JSONs.requiredVM;
/**
 * JSON Converter for the constraint {@link Ready}.
 *
 * @author Fabien Hermenier
 */
public class ReadyConverter implements ConstraintConverter<Ready> {


    @Override
    public Class<Ready> getSupportedConstraint() {
        return Ready.class;
    }

    @Override
    public String getJSONId() {
        return "ready";
    }

    @Override
    public Ready fromJSON(Model mo, JSONObject o) throws JSONConverterException {
        checkId(o);
        return new Ready(requiredVM(mo, o, "vm"));
    }

    @Override
    public JSONObject toJSON(Ready o) {
        JSONObject c = new JSONObject();
        c.put("id", getJSONId());
        c.put("vm", JSONs.elementToJSON(o.getInvolvedVMs().iterator().next()));
        return c;
    }
}
