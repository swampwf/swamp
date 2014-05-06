/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2004 Thomas Schmidt
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

package de.suse.swamp.webswamp;

import java.io.*;
import java.util.*;

import org.apache.commons.lang.*;
import org.apache.turbine.*;
import org.apache.turbine.om.security.*;
import org.apache.turbine.services.pull.*;
import org.apache.turbine.services.pull.util.*;
import org.apache.turbine.util.*;
import org.apache.velocity.*;
import org.apache.velocity.app.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.turbine.services.security.*;
import de.suse.swamp.util.*;

/**
 * @author tschmidt
 *
 * Extended Turbine UIManager Pull-Tool,
 * to be able to handle different Interfaces
 */
public class SwampUIManager extends UIManager implements ApplicationTool,
    Serializable {


    /**
     * Property tag for the Interface that is to be
     * used for the web application.
     */
    private static final String INTERFACE_PROPERTY = "tool.ui.interface";

    /**
     * Default Interface name.
     */
    private static final String INTERFACE_PROPERTY_DEFAULT = "";

    /**
     * Attribute name of InterfaceName value in User's temp hashmap.
     */
    private static final String INTERFACE_ATTRIBUTE =
        SwampUIManager.class.getName()+ ".interface";

    /**
     * The actual Interface being used for the webapp.
     */
    private String interfaceName;

    /**
     * The location of the skins within the application
     * resources directory.
     */
    private static final String SKINS_DIRECTORY = "/ui/skins";
    /**
     * The file within the skin directory that actually
     * contains the name/value pairs for the skin.
     */
    private static final String SKIN_PROPS_FILE = "skin.props";

    private static final String RESOURCES_PROPS_FILE = "resources.props";

    /**
     * The file name for the skin style sheet.
     */
    private static final String SKIN_CSS_FILE = "skin.css";


    /**
     * Properties to hold the name/value pairs
     * of image mappings.
     */
    private Properties resourceProperties;
    /**
     * The skins directory.
     */
    private String skinsDirectory;
    /**
     * Properties to hold the name/value pairs
     * for the skin.
     */
    private Properties skinProperties;


    /**
     * Overwriting init-Method of UIManager
     *
     * @param data assumed to be a RunData object
     */
    public void init(Object data){
        super.init(data);
        skinsDirectory =
            TurbinePull.getAbsolutePathToResourcesDirectory() + SKINS_DIRECTORY;
        loadSkin();

        if (data == null)
        {
            setInterface();
        }
        else if (data instanceof RunData)
        {
            setInterface((RunData) data);
        }
        else if (data instanceof User)
        {
            setInterface((User) data);
        }
    }



    /**
     * @return Returns the interfaceName.
     */
    public String getInterface() {
        return interfaceName;
    }



    /**
     * Set the Interface name to the Interface from the TR.props
     * file. If the property is not present use the
     * default Interface.
     */
    public void setInterface()
    {
        this.interfaceName = Turbine.getConfiguration()
                .getString(INTERFACE_PROPERTY,
                        INTERFACE_PROPERTY_DEFAULT);
    }

    /**
     * Set the Interface name to the specified Interface.
     *
     * @param InterfaceName the Interface name to use.
     */
    public void setInterface(String interfaceName)
    {
        this.interfaceName = interfaceName;
    }

    /**
     * Set the Interface name when the tool is configured to be
     * loaded on a per-request basis. By default it calls getInterface
     * to return the Interface specified in TR.properties.
     *
     * @param data a RunData instance
     */
    protected void setInterface(RunData data)
    {
        setInterface();
    }

    /**
     * Set the Interface name when the tool is configured to be
     * loaded on a per-session basis. It the user's temp hashmap contains
     * a value in the attribute specified by the String constant Interface_ATTRIBUTE
     * then that is returned. Otherwise it calls getInterface to return the skin
     * specified in TR.properties.
     *
     * @param user a User instance
     */
    protected void setInterface(User user)
    {
        if (user.getTemp(INTERFACE_ATTRIBUTE) == null)
        {
            setInterface();
        }
        else
        {
            setInterface((String) user.getTemp(INTERFACE_ATTRIBUTE));
        }
    }

    /**
     * Set the Interface name user's temp hashmap for the current session.
     *
     * @param user a User instance
     * @param interfacename the Interface name for the session
     */
    public static void setInterface(User user, String interfacename)
    {
        user.setTemp(INTERFACE_ATTRIBUTE, interfacename);
    }



    /**Get valid Interface Names from existance of
     * "workflows/$wfname/menutop.vm" files
     * @return - List of valid Interfaces
     */
    public static ArrayList getValidInterfaces() {
        String fs = System.getProperty("file.separator");
        ArrayList interfaces = new ArrayList();
        String swampHome = SWAMP.getInstance().getSWAMPHome();
        String templateDir = swampHome + fs + ".." + fs + "templates" +
            fs + "app" + fs + "screens" + fs + "workflows";
        File[] files = new File(templateDir).listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++){
                if (new File(files[i].getPath() + fs + "menutop.vm").exists()){
                    interfaces.add(files[i].getName());
                }
            }
        }
        return interfaces;
    }



    /**
     * Check if a a wf-template provides a "templatefile" with name "templatename"
     */
    public static boolean hasTemplate(String templateName , String templateFile) {
        String fs = System.getProperty("file.separator");
        String swampHome = SWAMP.getInstance().getSWAMPHome();
        String templateDir = swampHome + fs + ".." + fs + "templates" +
            fs + "app" + fs + "screens" + fs + "workflows" + fs + templateName;
        if (new File(templateDir + fs + templateFile).exists()){
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get the path to the workflows Index page
     */
    public String getIndexPath(String templateName){
        if (hasTemplate(templateName, "WorkflowIndex.vm")){
           return "workflows," + templateName + ",WorkflowIndex.vm";
        } else {
            return "WorkflowIndex.vm";
        }
    }


    public static ArrayList getValidSkins(){
        ArrayList skins = new ArrayList();
	    String resourcesDirectory = TurbinePull.getResourcesDirectory();
        File uiDir = null;
        try {
            uiDir = new File(resourcesDirectory);
        } catch (RuntimeException e) {
            Logger.ERROR("wskin path (" + resourcesDirectory + ") not found. "
                    + "Error: " + e.getMessage());
		}
		if (uiDir != null) {
			String[] userSkins = uiDir.list();
			if (userSkins != null) {
				for (int i = 0; i < userSkins.length; i++) {
					skins.add(userSkins[i]);
				}
			}
		}
		return skins;
    }



    public String getResourcePath(String templateName, String configItem){
        WorkflowManager wfMan = WorkflowManager.getInstance();
         return getResourcePath(wfMan.getWorkflowTemplate(templateName), configItem);
     }


     public String getResourcePath(Workflow wf, String configItem){
         return getResourcePath(wf.getTemplate(), configItem);
     }


     public String getResourcePath(WorkflowTemplate wf, String configItem){
         if (wf == null){
             Logger.ERROR("getResourcePath(): wfTemplate is NULL!");
             return "";
         }
         String path = null;
         String itemPath = wf.getConfigItem(configItem);
         if (itemPath != null){
             path = getAppLink() + "/resources/workflows/" +
             wf.getName() + "/" + wf.getVersion() + "/" + itemPath;
         }
         return path;
     }


     /**
     * check from the velocity template if a workflow template provides
     * a special resource, for example icon.
     */
    public boolean hasConfigValue(WorkflowTemplate wf, String configItem){
        boolean hasResource = false;
        if (wf != null && wf.containsConfigItem(configItem)){
            hasResource = true;
         }
         return hasResource;
     }


    /**
     * check from the velocity template if a workflow template provides
     * a special resource, for example icon.
     */
    public boolean hasConfigValue(String templatename, String configItem){
        WorkflowManager wfman = WorkflowManager.getInstance();
        return hasConfigValue(wfman.getWorkflowTemplate(templatename), configItem);
     }


    public boolean hasConfigValue(Workflow wf, String configItem){
        return hasConfigValue(wf.getTemplate(), configItem);
     }

    /**
     * get a workflow config value from a valocity template
     */
    public String getConfigValue(WorkflowTemplate wf, String configItem){
        return wf.getConfigItem(configItem);
     }

    public String getConfigValue(String templatename, String configItem){
        WorkflowManager wfman = WorkflowManager.getInstance();
        return wfman.getWorkflowTemplate(templatename).getConfigItem(configItem);
     }


    /** Just overwriting Turbines method to get a relative path.
     * So we get the Style + Images loaded from the
     * Clients requested Server and http/https method.
     */
    public String getStylecss() {
		StringBuffer sb = new StringBuffer();
		sb.append(getAppLink()).append("/resources/ui/skins/")
				.append(getSkin()).append("/").append(SKIN_CSS_FILE);
		return sb.toString();
	}


    public String getCommonStylecss() {
		StringBuffer sb = new StringBuffer();
		sb.append(getAppLink()).append("/resources/ui/skins/")
				.append("common/").append(SKIN_CSS_FILE);
		return sb.toString();
	}


    private String getAppLink() {
		return WebSWAMP.relativeAppLink;
	}



    /** Just overwriting Turbines method to get a relative path.
     * So we get the Style + Images loaded from the
     * Clients requested Server and http/https method.
     */
    public String image(String imageId) {
		StringBuffer sb = new StringBuffer();
		if (imageId != null && resourceProperties.containsKey(imageId)){
			String imagePath = resourceProperties.getProperty(imageId);
			sb.append(getAppLink()).append("/resources/ui/skins/").append(imagePath);
			return sb.toString();
		} else {
			Logger.ERROR("Image: " + imageId + " not found for skin " + getSkin());
			return "";
		}
	}





    /**
     * Load the specified skin on top of the "common" skin.
     */
    private void loadSkin() {
        resourceProperties = new Properties();

        try {
            FileInputStream is = new FileInputStream(skinsDirectory + "/common/" + RESOURCES_PROPS_FILE);
            resourceProperties.load(is);
            for (Iterator it = resourceProperties.keySet().iterator(); it.hasNext();) {
                String key = (String) it.next();
                resourceProperties.put(key, "common/images/" + resourceProperties.get(key));
            }
        } catch (Exception e) {
            Logger.ERROR("Cannot load resources for skin: common! " + e.getMessage());
        }

        try {
            FileInputStream is = new FileInputStream(skinsDirectory + "/" + getSkin() + "/" + RESOURCES_PROPS_FILE);
            Properties resourceProperties2 = new Properties();
            resourceProperties2.load(is);
            for (Iterator it = resourceProperties2.keySet().iterator(); it.hasNext();) {
                String key = (String) it.next();
                resourceProperties2.put(key, getSkin() + "/images/" + resourceProperties2.get(key));
            }
            resourceProperties.putAll(resourceProperties2);
        } catch (Exception e) {
            Logger.ERROR("Cannot load resources for skin: " + super.getSkin());
        }

        skinProperties = new Properties();

        try {
            FileInputStream is = new FileInputStream(skinsDirectory + "/common/" + SKIN_PROPS_FILE);
            skinProperties.load(is);
        } catch (Exception e) {
            Logger.ERROR("Cannot load common skin props: " + super.getSkin());
        }

        try {
            FileInputStream is = new FileInputStream(skinsDirectory + "/" + getSkin() + "/" + SKIN_PROPS_FILE);
            Properties skinProperties2 = new Properties();
            skinProperties2.load(is);
            skinProperties.putAll(skinProperties2);
        } catch (Exception e) {
            Logger.ERROR("Cannot load skin props for: " + super.getSkin());
        }
    }


    public String getWorkflowColour(Workflow wf) {
        WorkflowTemplate wftemp = wf.getTemplate();
        String cssColour = "";
        if (hasConfigValue(wftemp, "workflowlist_colour")) {
            String script = getConfigValue(wftemp, "workflowlist_colour");
            VelocityContext context = new VelocityContext();
            context.put("wf", wf);
            context.put("ui", this);
            context.put("now", new Date());
            String ident = "getWfColour";
            StringWriter w = new StringWriter();
            try {
                Velocity.evaluate(context, w, ident, script);
            } catch (Exception e) {
                Logger.ERROR("Could not set wfColour, Reason: " + e.getMessage());
            }
            if (w != null && !w.toString().equals("")){
                cssColour = "background-color: " + w.toString().trim() + "; ";
            }
            //Logger.DEBUG("Setting colour to: " + w.toString());
        // set default colour for inactive wfs:
        } else if (!wf.isRunning()){
            cssColour = "background-color: #cdccbf; ";
        }
        return cssColour;
    }



    /**
     * get diff between 2 dates in days
     */
    public int getDateDiff (Date d1, Date d2){
        long diff = d2.getTime() - d1.getTime();
        int days = new Long(diff / 1000 / 3600 / 24).intValue();
        return days;
    }


    public String htmlEncode(String html){
        html = StringEscapeUtils.escapeHtml(html);
        return html;
    }

    public String newlineEncode(String html){
        html = html.replaceAll("\r", "").replaceAll("\\\\n", "<br />").
        replaceAll("\\\n", "<br />").replaceAll("\\n", "<br />").replaceAll("\n", "<br />");
        return html;
    }

    public String tooltipEncode(String html){
    	html = htmlEncode(html);
        html = newlineEncode(html);
        html = html.replaceAll("'", "").replaceAll("&apos;", "");
        return html;
    }


    /**
     * Translates the String into the users language
     * using the default resource
     */
    public String tr(String text, SWAMPTurbineUser user){
    	I18n i18n = new I18n(getClass(), "de.suse.swamp.webswamp.i18n.Webswamp");
    	return i18n.tr(text, user.getSWAMPUser());
    }

    public String tr(String text, SWAMPTurbineUser user, Object object){
    	I18n i18n = new I18n(getClass(), "de.suse.swamp.webswamp.i18n.Webswamp");
    	return i18n.tr(text, user.getSWAMPUser(), object.toString());
    }

    public String tr(String text, SWAMPTurbineUser user, Object object, Object object2){
    	I18n i18n = new I18n(getClass(), "de.suse.swamp.webswamp.i18n.Webswamp");
    	return i18n.tr(text, user.getSWAMPUser(), object.toString(), object2.toString());
    }

    /**
     * Translates the String into the users language
     * using a given resource (only used in extensions such as suse internal swamp)
     */
    public String trpath(String text, SWAMPTurbineUser user, String path){
    	I18n i18n = new I18n(getClass(), path);
    	return i18n.tr(text, user.getSWAMPUser());
    }

    public String trpath(String text, SWAMPTurbineUser user, String path, Object object){
    	I18n i18n = new I18n(getClass(), path);
    	return i18n.tr(text, user.getSWAMPUser(), object.toString());
    }

    public String trpath(String text, SWAMPTurbineUser user, String path, Object object, Object object2){
        I18n i18n = new I18n(getClass(), path);
        return i18n.tr(text, user.getSWAMPUser(), object.toString(), object2.toString());
    }

    public void log(String message) {
        Logger.DEBUG("webswamp:" + message);
    }


}