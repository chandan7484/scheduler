package btrplace.json.plan;

import btrplace.json.JSONConverter;
import btrplace.json.JSONConverterException;
import btrplace.json.JSONUtils;
import btrplace.plan.Action;
import btrplace.plan.event.*;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * JSON converter for {@link Action}.
 *
 * @author Fabien Hermenier
 */
public class ActionConverter implements JSONConverter<Action>, ActionVisitor {


    @Override
    public Action fromJSON(JSONObject in) throws JSONConverterException {
        String id = in.get("id").toString();
        if (id == null) {
            throw new JSONConverterException("The action identifier is expected on the key 'id'");
        }

        if (id.equals("bootVM")) {
            return bootVMFromJSON(in);
        } else if (id.equals("shutdownVM")) {
            return shutdownVMFromJSON(in);
        } else if (id.equals("shutdownNode")) {
            return shutdownNodeFromJSON(in);
        } else if (id.equals("bootNode")) {
            return bootNodeFromJSON(in);
        } else if (id.equals("forgeVM")) {
            return forgeVMFromJSON(in);
        } else if (id.equals("killVM")) {
            return killVMFromJSON(in);
        } else if (id.equals("migrateVM")) {
            return migrateVMFromJSON(in);
        } else if (id.equals("resumeVM")) {
            return resumeVMFromJSON(in);
        } else if (id.equals("suspendVM")) {
            return suspendVMFromJSON(in);
        } else if (id.equals("allocate")) {
            return allocateFromJSON(in);
        } else {
            throw new JSONConverterException(("Unsupported type of action '" + id + "'"));
        }
    }

    @Override
    public JSONObject visit(BootVM a) {
        JSONObject o = makeSkeleton(a);
        o.put("id", "bootVM");
        o.put("vm", a.getVM().toString());
        o.put("destination", a.getDestinationNode().toString());
        return o;
    }

    private BootVM bootVMFromJSON(JSONObject in) throws JSONConverterException {
        return new BootVM(JSONUtils.requiredUUID(in, "vm"),
                JSONUtils.requiredUUID(in, "destination"),
                (int) JSONUtils.requiredLong(in, "start"),
                (int) JSONUtils.requiredLong(in, "end"));
    }

    @Override
    public JSONObject visit(ShutdownVM a) {
        JSONObject o = makeSkeleton(a);
        o.put("id", "shutdownVM");
        o.put("vm", a.getVM().toString());
        o.put("location", a.getNode().toString());
        return o;
    }

    private ShutdownVM shutdownVMFromJSON(JSONObject in) throws JSONConverterException {
        return new ShutdownVM(JSONUtils.requiredUUID(in, "vm"),
                JSONUtils.requiredUUID(in, "location"),
                (int) JSONUtils.requiredLong(in, "start"),
                (int) JSONUtils.requiredLong(in, "end"));
    }

    @Override
    public JSONObject visit(ShutdownNode a) {
        JSONObject o = makeSkeleton(a);
        o.put("id", "shutdownNode");
        o.put("node", a.getNode().toString());
        return o;
    }

    private ShutdownNode shutdownNodeFromJSON(JSONObject in) throws JSONConverterException {
        return new ShutdownNode(JSONUtils.requiredUUID(in, "node"),
                (int) JSONUtils.requiredLong(in, "start"),
                (int) JSONUtils.requiredLong(in, "end"));
    }

    @Override
    public JSONObject visit(BootNode a) {
        JSONObject o = makeSkeleton(a);
        o.put("id", "bootNode");
        o.put("node", a.getNode().toString());
        return o;
    }

    private BootNode bootNodeFromJSON(JSONObject in) throws JSONConverterException {
        return new BootNode(JSONUtils.requiredUUID(in, "node"),
                (int) JSONUtils.requiredLong(in, "start"),
                (int) JSONUtils.requiredLong(in, "end"));
    }

    @Override
    public JSONObject visit(MigrateVM a) {
        JSONObject o = makeSkeleton(a);
        o.put("id", "migrateVM");
        o.put("vm", a.getVM().toString());
        o.put("destination", a.getDestinationNode().toString());
        o.put("location", a.getSourceNode().toString());
        return o;
    }


    private MigrateVM migrateVMFromJSON(JSONObject in) throws JSONConverterException {
        return new MigrateVM(JSONUtils.requiredUUID(in, "vm"),
                JSONUtils.requiredUUID(in, "location"),
                JSONUtils.requiredUUID(in, "destination"),
                (int) JSONUtils.requiredLong(in, "start"),
                (int) JSONUtils.requiredLong(in, "end"));
    }

    @Override
    public JSONObject visit(SuspendVM a) {
        JSONObject o = makeSkeleton(a);
        o.put("id", "suspendVM");
        o.put("vm", a.getVM().toString());
        o.put("destination", a.getDestinationNode().toString());
        o.put("location", a.getSourceNode().toString());
        return o;
    }

    private SuspendVM suspendVMFromJSON(JSONObject in) throws JSONConverterException {
        return new SuspendVM(JSONUtils.requiredUUID(in, "vm"),
                JSONUtils.requiredUUID(in, "location"),
                JSONUtils.requiredUUID(in, "destination"),
                (int) JSONUtils.requiredLong(in, "start"),
                (int) JSONUtils.requiredLong(in, "end"));
    }

    @Override
    public JSONObject visit(ResumeVM a) {
        JSONObject o = makeSkeleton(a);
        o.put("id", "resumeVM");
        o.put("vm", a.getVM().toString());
        o.put("destination", a.getDestinationNode().toString());
        o.put("location", a.getSourceNode().toString());
        return o;
    }

    private ResumeVM resumeVMFromJSON(JSONObject in) throws JSONConverterException {
        return new ResumeVM(JSONUtils.requiredUUID(in, "vm"),
                JSONUtils.requiredUUID(in, "location"),
                JSONUtils.requiredUUID(in, "destination"),
                (int) JSONUtils.requiredLong(in, "start"),
                (int) JSONUtils.requiredLong(in, "end"));
    }

    @Override
    public JSONObject visit(KillVM a) {
        JSONObject o = makeSkeleton(a);
        o.put("id", "killVM");
        o.put("vm", a.getVM().toString());
        o.put("location", a.getNode().toString());
        return o;
    }

    private KillVM killVMFromJSON(JSONObject in) throws JSONConverterException {
        return new KillVM(JSONUtils.requiredUUID(in, "vm"),
                JSONUtils.requiredUUID(in, "location"),
                (int) JSONUtils.requiredLong(in, "start"),
                (int) JSONUtils.requiredLong(in, "end"));

    }

    @Override
    public JSONObject visit(ForgeVM a) {
        JSONObject o = makeSkeleton(a);
        o.put("id", "forgeVM");
        o.put("vm", a.getVM().toString());
        return o;

    }

    private ForgeVM forgeVMFromJSON(JSONObject in) throws JSONConverterException {
        return new ForgeVM(JSONUtils.requiredUUID(in, "vm"),
                (int) JSONUtils.requiredLong(in, "start"),
                (int) JSONUtils.requiredLong(in, "end"));
    }

    @Override
    public JSONObject visit(Allocate a) {
        JSONObject o = makeSkeleton(a);
        o.put("id", "allocate");
        o.put("vm", a.getVM());
        o.put("rc", a.getResourceId());
        o.put("qty", a.getAmount());
        o.put("location", a.getHost().toString());
        return o;
    }

    private Allocate allocateFromJSON(JSONObject in) throws JSONConverterException {
        return new Allocate(JSONUtils.requiredUUID(in, "vm"),
                JSONUtils.requiredUUID(in, "location"),
                JSONUtils.requiredString(in, "rc"),
                (int) JSONUtils.requiredLong(in, "qty"),
                (int) JSONUtils.requiredLong(in, "start"),
                (int) JSONUtils.requiredLong(in, "end"));
    }

    @Override
    public JSONObject toJSON(Action a) throws JSONConverterException {
        return (JSONObject) a.visit(this);
    }

    /**
     * Convert a collection of actions to an array of JSON objects
     *
     * @param actions the actions to convert
     * @return an array containing all the actions converted into JSON strings
     * @throws JSONConverterException if an error occurred during the conversion
     */
    public JSONArray toJSON(Collection<Action> actions) throws JSONConverterException {
        JSONArray arr = new JSONArray();
        for (Action a : actions) {
            arr.add(toJSON(a));
        }
        return arr;
    }

    /**
     * Convert a collection of serialized actions.
     *
     * @param actions the actions to convert
     * @return a collection of actions
     * @throws JSONConverterException if an error occurred during the conversion
     */
    public Collection<Action> fromJSON(JSONArray actions) throws JSONConverterException {
        List<Action> l = new ArrayList<Action>(actions.size());
        for (Object o : actions) {
            if (o instanceof JSONObject) {
                l.add(fromJSON((JSONObject) o));
            } else {
                throw new JSONConverterException("Unable to extract an action from:" + o.toString());
            }
        }
        return l;
    }

    /**
     * Just create the JSONObject and set the start and the end attribute.
     *
     * @param a the action to convert
     * @return a skeleton JSONObject
     */
    private JSONObject makeSkeleton(Action a) {
        JSONObject o = new JSONObject();
        o.put("start", a.getStart());
        o.put("end", a.getEnd());
        return o;
    }
}
