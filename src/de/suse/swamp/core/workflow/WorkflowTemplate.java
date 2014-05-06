/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2003 Sonja Krause-Harder <skh [at] suse.de>
 * Copyright (c) 2006 Novell Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of version 2 of the GNU General Public
 * License as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * In addition, as a special exception, Novell Inc. gives permission to link the
 * code of this program with the following applications:
 *
 * - All applications of the Apache Software Foundation
 *
 * and distribute such linked combinations.
 */

package de.suse.swamp.core.workflow;

/**
 * A workflow template that can produce actual Workflow objects.
 * Interface ExtDescribable marks class as having a longDescription
 *
 * @author Sonja Krause-Harder &lt;skh@suse.de&gt;
 * @author tschmidt@suse.de
 */

import java.io.*;
import java.util.*;

import org.apache.commons.collections.map.*;
import org.apache.commons.configuration.*;
import org.apache.commons.configuration.reloading.*;

import de.suse.swamp.core.actions.*;
import de.suse.swamp.core.container.*;
import de.suse.swamp.core.container.SecurityManager;
import de.suse.swamp.core.data.*;
import de.suse.swamp.core.filter.*;
import de.suse.swamp.core.security.*;
import de.suse.swamp.core.security.roles.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.util.*;

public class WorkflowTemplate implements ExtDescribable {

    // Name of the workflow template as set in XML definition file
    private String name;
    private String version;

    // the workflow.conf properties
    private Configuration wfProps = new PropertiesConfiguration();
    PropertiesConfiguration config;

    private String helpContext;
    private String longDescription;

    // HashMap: Key: Name, Value: Node-object
    private ListOrderedMap nodeTempls = new ListOrderedMap();

    // HashMap: Key: Name, Value: Datasettemplate-Object
    private HashMap datasetTemplates = new HashMap();

    // Description text for the workflow-instance
    private String description;

    // Description text for the workflow-template
    private String templatedescription;
    private String requiredSWAMPVersion;

    // optional parent workflow type
    private String parentWfName;

    // optional parent workflow version
    private String parentWfVersion;
    private WorkflowTemplate parentTemplate;

    // Map containing WorkflowRole Objects; key: name, value=WorkflowRole
    private Hashtable roles = new Hashtable();

    // events that are used in conditions of this template.
    // Used to skip handling of unused events early
    private HashSet waitingForEvents = new HashSet();

    // storing paths that are listening for DATA_CHANGED events so that we can
    // skip all others
    private HashSet listenerPaths = new HashSet();

    public WorkflowTemplate(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    /** Will create the Workflow Object with attached Dsets from its XML-file.
     * The Workflow is already stored in DB.
     * @return
     * @throws Exception
     */
    public Workflow createWorkflow() throws StorageException {
        Workflow wf = this.createWorkflow(createDatasets());
        return wf;
    }

    /** Creating a Workflow Instance with the given Datasets attached.
     * @param Datasets
     * @return
     * @throws Exception
     */
    public Workflow createWorkflow(List datasets) throws StorageException {
        Workflow wf = getWorkflow();
        WorkflowManager.storeWorkflow(wf);
        for (Iterator it = datasets.iterator(); it.hasNext();) {
            DataManager.addDatasetToWorkflow(((Dataset) it.next()).getId(), wf.getId());
        }
        return wf;
    }

    /**
     * @return A new workflow item following this template.
     * Not yet stored in DB, use createWorkflow() for creating a real
     * Workflow in the system.
     */
    public Workflow getWorkflow() {

        ArrayList newNodes = new ArrayList();
        // hashed by id for setting references in edges
        HashMap nodeLookup = new HashMap();

        // first, get all nodes. beware: they still don't have edges. Keep a map
        // with (node.getId(), node) pairs
        for (Iterator iter = nodeTempls.keySet().iterator(); iter.hasNext();) {
            NodeTemplate ntmpl = (NodeTemplate) nodeTempls.get(iter.next());
            Node newNode = ntmpl.getNode();
            newNodes.add(newNode);
            nodeLookup.put(newNode.getName(), newNode);
        }

        // now that all nodes are created, loop over them again so that they may
        // create their edges.
        for (Iterator iter = newNodes.iterator(); iter.hasNext();) {
            Node node = (Node) iter.next();
            node.createEdges(nodeLookup);
        }

        // give it all nodes
        Workflow workflow = new Workflow(name, version);
        workflow.setNodes(newNodes);
        return workflow;
    }

    /** Will create the Datasets from their templates.
     * The Sets will already be stored in DB and have an ID.
     * Method may also be used to do Data preparation in pre Workflow pages,
     * and attach the Dataset at the end of this pages
     *
     * @return List of created Datasets
     */
    public List createDatasets() {
        List sets = new ArrayList();
        for (Iterator it = datasetTemplates.keySet().iterator(); it.hasNext();) {
            DatasetTemplate temp = (DatasetTemplate) datasetTemplates.get(it.next());
            sets.add(temp.createDataset());
        }
        return sets;
    }

    /**
     * @return the Paths of all Databits in all attached Datasets
     */
    public ArrayList getAllDatabitPaths() {
        ArrayList bitPaths = new ArrayList();
        for (Iterator it = this.datasetTemplates.keySet().iterator(); it.hasNext();) {
            DatasetTemplate dset = (DatasetTemplate) this.datasetTemplates.get(it.next());
            for (Iterator it2 = dset.getAllDatabitPaths().iterator(); it2.hasNext();) {
                String val = (String) it2.next();
                bitPaths.add(dset.getName() + "." + val);
            }
        }
        return bitPaths;
    }

    /**
     * Adds a node to the workflow template. Needed by and called from the
     * XML processing objects that read in the workflow description.
     */

    public void addNodeTemplate(NodeTemplate nodeTempl) {
        nodeTempls.put(nodeTempl.getName(), nodeTempl);
    }

    /**
     * Provides sensible debugging output
     */
    public String toString() {
        return "[" + getName() + "]";
    }

    /**
     * Sets the datapack template of this workflow template
     */
    public void addDatasetTemplate(DatasetTemplate dset) {
        datasetTemplates.put(dset.getName(), dset);
    }

    /**
     * @param The description text for this workflow-instance
     */
    public void setDescription(String desc) {
        description = desc;
    }

    public void setTemplateDescription(String desc) {
        templatedescription = desc;
    }

    /**
     * @return Returns the datasetTemplates.
     */
    public HashMap getDatasetTemplates() {
        return this.datasetTemplates;
    }

    /**
     * @param helpText The helpText to set.
     */
    public void setHelpContext(String helpText) {
        this.helpContext = helpText;
    }

    /**
     * @param shortDescription The shortDescription to set.
     */
    public void setLongDescription(String longDescription) {
        this.longDescription = longDescription;
    }

    /**
     * @param version The version to set.
     */
    public void setVersion(String version) {
        this.version = version;
    }

    public String getConfigItem(String item) {
        String itemValue = wfProps.getString(item);
        if (itemValue == null) {
            Logger.ERROR("ConfigItem: " + item + " not found for template " + this.getName());
        } else {
            itemValue = itemValue.trim();
        }
        return itemValue;
    }

    public boolean containsConfigItem(String item) {
        return (wfProps.getProperty(item) == null ? false : true);
    }

    /** This Method searches in the Workflow config file
     * for a comma seperated list of values for the key <i>item</i>
     * @param item
     * @return
     */
    public List getConfigItemAsList(String item) {
        return wfProps.getList(item, new ArrayList());
    }

    /**
     * @return Returns the toplevel attached dataset.
     */
    public String getDefaultDsetName() {
        String name = null;
        if (this.getDatasetTemplates().size() > 0) {
            DatasetTemplate dset = (DatasetTemplate) this.getDatasetTemplates().values().iterator().next();
            name = dset.getName();
        }
        return name;
    }

    /**
     * @return Returns the description.
     */
    public String getDescription() {
        return this.description.trim();
    }

    /**
     * @return Returns the helpContext.
     */
    public String getHelpContext() {
        return this.helpContext;
    }

    /**
     * @return Returns the longDescription.
     */
    public String getLongDescription() {
        return this.longDescription.trim();
    }

    /**
     * @return Returns the templatedescription.
     */
    public String getTemplateDescription() {
        return this.templatedescription.trim();
    }

    /**
     * @return Returns the version.
     */
    public String getVersion() {
        return this.version;
    }

    /**
     * @param wfProps The wfProps to set.
     */
    public void setWfProps(File wfProps) throws Exception {
        config = new PropertiesConfiguration(wfProps);
        config.setReloadingStrategy(new FileChangedReloadingStrategy());
        this.wfProps = new PropertiesConfiguration(wfProps);
    }

    public String getRessourcesPath() {
        return SWAMP.getInstance().getProperty("WORKFLOW_LOCATION") + System.getProperty("file.separator") + this.name
                + System.getProperty("file.separator") + this.version + System.getProperty("file.separator");
    }

    public NodeTemplate getNodeTemplate(String name) {
        NodeTemplate nodeTemp = (NodeTemplate) nodeTempls.get(name);
        if (nodeTemp != null) {
            return nodeTemp;
        }
        Logger.ERROR("NodeTemplate " + name + " not found for Template " + this.getName());
        return null;
    }

    public MileStoneTemplate getMileStoneTemplate(String name) {
        for (Iterator it = nodeTempls.keySet().iterator(); it.hasNext();) {
            NodeTemplate nodeTemp = (NodeTemplate) nodeTempls.get(it.next());
            MileStoneTemplate mileStoneTemplate = nodeTemp.getMileStoneTemplate();
            if (mileStoneTemplate != null && mileStoneTemplate.getName().equals(name)) {
                return mileStoneTemplate;
            }
        }
        Logger.ERROR("MileStoneTemplate " + name + " not found for Template " + this.getName());
        return null;
    }

    /** Get the DatabitTemplate in the given path
     * Notation: datasetname.[datasetname.[databitname]]
     *
     * @param path
     * @return requested Databit if found, null if not.
     */
    public DatabitTemplate getDatabitTemplate(String path) {
        DatabitTemplate dbit = null;
        if (path.startsWith("System.")) {
            path = path.substring(7);
            dbit = DataToWfPropertyMapper.getValueForWorkflow(this, path);
        } else {
            StringTokenizer st = new StringTokenizer(path, ".");
            if (st.countTokens() < 1) {
                Logger.BUG("Empty pathname for getting Databittemplate");
            } else if (st.countTokens() > 1) {
                // assuming that the first item is the datasetname
                String datasetName = st.nextToken();
                DatasetTemplate dataset = (DatasetTemplate) datasetTemplates.get(datasetName);
                if (dataset == null && WorkflowManager.isInstantiated()) {
                    WorkflowManager wfman = WorkflowManager.getInstance();
                    if (wfman.templateExists(parentWfName, parentWfVersion)) {
                        WorkflowTemplate parent = WorkflowManager.getInstance().getWorkflowTemplate(parentWfName,
                                parentWfVersion);
                        dbit = parent.getDatabitTemplate(path);
                    }
                } else if (dataset != null) {
                    String newField = path.substring(datasetName.length() + 1);
                    dbit = dataset.getDatabitTemplate(newField);
                }
            }
        }
        return dbit;
    }

    /**
    * Checks if the User has the given role in the Workflowtemplate.
    * This is configured in the Workflow definition.
    * Example roles are "admin" - "starter" - "user" - "owner"
    *
    * swampadmins as defined in the SWAMP usermanagement have all roles
    * @throws Exception if named role does not exist
    */
    public boolean hasRole(String username, String role) {
        boolean hasRole = false;
        WorkflowRole wfrole = getWorkflowRole(role);

        // check if the user is in the role the regular way,
        // but mind that the needed databit may not yet be attached
        try {
            if (wfrole.hasRole(username, this)) {
                hasRole = true;
            }
        } catch (NoSuchElementException e) {
            // skip exception, because templates often haven't yet attached the needed values
        } catch (Exception e) {
            Logger.ERROR("Error when checking for role " + role + " Msg: " + e.getMessage());
            return false;
        }

        //for anonymous users, we can stop here:
        if (username.equals("anonymous")) {
            return hasRole;
        }

        try {
            if (hasRole == false) {
                SWAMPUser user = null;
                try {
                    user = SecurityManager.getUser(username);
                } catch (StorageException e) {
                    Logger.ERROR("Error fetching user: " + username + ". Reason: " + e.getMessage());
                    return false;
                }
                // SWAMPAdmins, Workflow-Admins and "Owner"
                // always have all permissions in a workflow
                if (SecurityManager.isGroupMember(user, "swampadmins")
                        || getWorkflowRole("admin").hasRole(username, this)) {
                    hasRole = true;
                }
            }

            // at last if the user has a role in any single instance of that workflowtemplate
            // he has that role of that template
            if (!hasRole && !wfrole.isStaticRole(this)) {
                long time = System.currentTimeMillis();
                List ids = new ArrayList();
                PropertyFilter templateFilter = new PropertyFilter();
                templateFilter.addWfTemplate(this.getName());

                // if we have more databitpaths, the id lists need to be merged,
                // because the different roles are like an OR statement
                List paths = new ArrayList();
                if (wfrole instanceof DatabitRole) {
                    paths.add(((DatabitRole) wfrole).getRoleDatabit());
                } else if (wfrole instanceof ReferencesRole) {
                    paths = ((ReferencesRole) wfrole).getAllRoleDatabits(this);
                }

                for (Iterator it = paths.iterator(); it.hasNext();) {
                    ArrayList filters = new ArrayList();
                    filters.add(templateFilter);
                    String datapath = (String) it.next();
                    ContentFilter cfilter = new ContentFilter();
                    cfilter.setDatabitPath(datapath);
                    // names are stored: name1, name2...
                    String regexp = "(" + username + "$|" + username + ",)";
                    cfilter.setDatabitValueRegex(regexp);
                    filters.add(cfilter);
                    ids.addAll(WorkflowManager.getInstance().getWorkflowIds(filters, null));
                    if (ids.size() > 0) {
                        hasRole = true;
                        break;
                    }
                }
                Logger.DEBUG("non-static check for " + role + "/" + username + "/" + this.getName() + " (" + hasRole
                        + ") took " + (System.currentTimeMillis() - time) + "ms");
            }
        } catch (NoSuchElementException e) {
            // skip exception, because templates often haven't yet attached the needed values
        } catch (Exception e) {
            Logger.ERROR("Error when checking for role " + role + " Msg: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return hasRole;
    }

    public ActionTemplate getActionTemplate(String name) {
        ActionTemplate actionTemp = null;
        for (Iterator it = nodeTempls.values().iterator(); it.hasNext();) {
            NodeTemplate nodeTemp = (NodeTemplate) it.next();
            actionTemp = nodeTemp.getActionTemplate(name);
            if (actionTemp != null) {
                return actionTemp;
            }
        }
        Logger.ERROR("ActionTemplate: " + name + " could not be found for" + " Workflow: " + this.getName());
        return actionTemp;
    }

    public void addRole(WorkflowRole role) {
        roles.put(role.getName(), role);
    }

    /**
     * checks if a role with that name is defined in the workflowtemplate
     */
    public boolean roleExists(String role) {
        boolean exists = false;
        if (role.startsWith("parent.") && parentTemplate != null) {
            exists = parentTemplate.roleExists(role.substring(7));
        } else {
            if (getWorkflowRole(role) != null) {
                exists = true;
            }
        }
        return exists;
    }

    public WorkflowRole getWorkflowRole(String role) {
        WorkflowRole foundrole = null;
        if (role.startsWith("parent.") && parentTemplate != null) {
            foundrole = parentTemplate.getWorkflowRole(role.substring(7));
        } else if (roles.containsKey(role)) {
            foundrole = (WorkflowRole) roles.get(role);
        } else {
            Logger.ERROR("Requested role: " + role + " not found for " + getName() + getVersion());
        }
        return foundrole;
    }

    public WorkflowTemplate getTemplateForRole(String roleName) {
        WorkflowTemplate temp = this;
        if (roleName.startsWith("parent.") && parentTemplate != null) {
            temp = parentTemplate.getTemplateForRole(roleName.substring(7));
        }
        return temp;
    }

    /**
     * @return Returns the roles.
     */
    public Collection getRoles() {
        return roles.values();
    }

    public String getRequiredSWAMPVersion() {
        return requiredSWAMPVersion;
    }

    public void setRequiredSWAMPVersion(String requiredSWAMPVersion) {
        this.requiredSWAMPVersion = requiredSWAMPVersion;
    }

    public Map getAllNodeTemplates() {
        return nodeTempls;
    }

    /**
     * Query for all nodes of a type: can be "end" "start" "normal"
     */
    public ArrayList getAllNodeTemplates(String type) {
        ArrayList nodeTemplates = new ArrayList();
        for (Iterator it = nodeTempls.values().iterator(); it.hasNext();) {
            NodeTemplate node = (NodeTemplate) it.next();
            if (node.getType().equalsIgnoreCase(type)) {
                nodeTemplates.add(node);
            }
        }
        return nodeTemplates;
    }

    public ArrayList getAllEdgeTempls() {
        ArrayList edges = new ArrayList();
        for (Iterator it = nodeTempls.values().iterator(); it.hasNext();) {
            NodeTemplate node = (NodeTemplate) it.next();
            edges.addAll(node.getEdgeTempls());
        }
        return edges;
    }

    public ArrayList getAllActionTemplates() {
        ArrayList actions = new ArrayList();
        for (Iterator it = nodeTempls.values().iterator(); it.hasNext();) {
            NodeTemplate node = (NodeTemplate) it.next();
            actions.addAll(node.getActionTemplates());
        }
        return actions;
    }

    /**
     * Evaluate if this WorkflowTemplate has a DatabitTemplate attached at the provided path
     */
    public boolean containsDatabitTemplate(String path) {
        boolean exists = getDatabitTemplate(path) == null ? false : true;
        return exists;
    }

    /**
     * @return a list of templates that refer to this template as parentworkflow
     */
    public List getSubworkflowTemplates() {
        List templates = new ArrayList();
        HashMap templateMap = WorkflowManager.getInstance().getWorkflowTemplates();
        for (Iterator it = templateMap.values().iterator(); it.hasNext();) {
            TreeMap wfversions = (TreeMap) it.next();
            for (Iterator tempIt = wfversions.values().iterator(); tempIt.hasNext();) {
                WorkflowTemplate temp = (WorkflowTemplate) tempIt.next();
                if (temp.getParentWfName() != null && temp.getParentWfName().equals(this.name)
                        && temp.getParentWfVersion().equals(this.version)) {
                    templates.add(temp);
                }
            }
        }

        return templates;
    }

    public String getParentWfName() {
        return parentWfName;
    }

    public void setParentWfName(String parentWfName) {
        this.parentWfName = parentWfName;
    }

    public String getParentWfVersion() {
        return parentWfVersion;
    }

    public void setParentWfVersion(String parentWfVersion) {
        this.parentWfVersion = parentWfVersion;
    }

    /**
     * @return Returns the template-name from the top level parent
     */
    public WorkflowTemplate getMasterParentTemplate() {
        if (parentWfName != null && parentWfVersion != null && !parentWfName.equals(this.name)) {
            return WorkflowManager.getInstance().getWorkflowTemplate(parentWfName, parentWfVersion)
                    .getMasterParentTemplate();
        } else {
            return this;
        }
    }

    public WorkflowTemplate getParentTemplate() {
        return parentTemplate;
    }

    public void setParentTemplate(WorkflowTemplate parentTemplate) {
        this.parentTemplate = parentTemplate;
    }

    public HashSet getWaitingForEvents() {
        return waitingForEvents;
    }

    public void addWaitingForEvents(String eventName) {
        this.waitingForEvents.add(eventName);
    }

    public void addListenerPath(String path) {
        listenerPaths.add(path);
    }

    public boolean hasListenerPath(String path) {
        return listenerPaths.contains(path);
    }

    public HashSet getListenerPaths() {
        return listenerPaths;
    }

}
