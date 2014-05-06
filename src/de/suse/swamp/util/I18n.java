/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2006 Thomas Schmidt <tschmidt@suse.de> 
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

package de.suse.swamp.util;

import java.util.Locale;
import java.util.MissingResourceException;

import de.suse.swamp.core.container.SWAMP;
import de.suse.swamp.core.security.SWAMPUser;

/**
 * @author  Thomas Schmidt &lt;tschmidt@suse.de&gt;
 */

public class I18n {

	private org.xnap.commons.i18n.I18n myI18n;
	private String defaultLocale;
	private final String fallbackLocale = "en";
	
    public static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(
    	"de.suse.swamp.util.I18n");
	
	public I18n(Class clazz, String baseName){
		myI18n = loadI18n(clazz, baseName);
    }
	
	
	/** org.xnap.commons.i18n.I18n instances are shared and managed by I18nCache
	 */
	private org.xnap.commons.i18n.I18n loadI18n(Class clazz, String baseName){
		defaultLocale = SWAMP.getInstance().getProperty("LOCALE");
		org.xnap.commons.i18n.I18n i18n = null;
		if (defaultLocale == null || !I18nCache.getInstance().contains(baseName)){
			try {
				i18n = new org.xnap.commons.i18n.I18n(baseName, 
						new Locale(defaultLocale), clazz.getClassLoader());
				Logger.LOG("Adding i18n " + baseName + " to cache", log);
				I18nCache.getInstance().add(i18n, baseName);
			} catch (MissingResourceException e) {
				Logger.ERROR("No resource found for " + baseName + " - " + 
						defaultLocale + ". Falling back to " + fallbackLocale);
				defaultLocale = fallbackLocale;
				try {
					i18n = new org.xnap.commons.i18n.I18n(baseName, 
							new Locale(defaultLocale), clazz.getClassLoader());
					Logger.LOG("Adding i18n " + baseName + " to cache", log);
					I18nCache.getInstance().add(i18n, baseName);
				} catch (RuntimeException e1) {
					Logger.ERROR("Loading of fallback resource failed!");
				}
			}
		} else {
			i18n = I18nCache.getInstance().getI18n(baseName);
		}
		return i18n;
	}
	
	/**
     * translate the String using the default language
     */
	public String tr(String content){
    	String value = tr(content, defaultLocale, null) ;
    	return value;
    }
	
    /**
     * translate the String using the provided language
     */
	private String tr(String content, String lang , String[] objects){
		Logger.DEBUG("Translating String: \"" + content + "\" to language: " + lang, log);
		lang = (lang == null) ? defaultLocale : lang;
		String value;
		if (myI18n == null){
			return objectReplace(content, objects);
		}
		try {
			myI18n.setLocale(new Locale(lang));
			value = objectReplace(myI18n.getResources().getString(content), objects);
			Logger.DEBUG("Translated String: \"" + value + "\"", log);
		} catch (MissingResourceException e) {
			Logger.DEBUG("String not found, using default.", log);
			value = objectReplace(content, objects);
		}
    	return value;
    }
	
	
	private String objectReplace(String value, String[] objects) {
		if (objects != null && objects.length > 0) {
			for (int i = 0; i < objects.length; i++)
				value = value.replaceAll("%" + (i + 1), objects[i]);
		}
		return value;
	}
	
    /**
     * translate the String using the users config language
     */
	public String tr(String content, SWAMPUser user){
		String lang = user.getPerm("lang", defaultLocale);
		String value = tr(content, lang, null);
    	return value;
    }
	
    /**
     * translate the String using the users config language
     */
	public String tr(String content, SWAMPUser user, String object){
		String lang = user.getPerm("lang", defaultLocale);
		String value = tr(content, lang, new String[] { object });
    	return value;
    }
	
    /**
     * translate the String using the users config language
     */
	public String tr(String content, SWAMPUser user, String object, String object2){
		String lang = user.getPerm("lang", defaultLocale);
		String value = tr(content, lang, new String[] { object, object2 });
    	return value;
    }
}