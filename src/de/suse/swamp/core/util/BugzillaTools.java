/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2005 Thomas Schmidt <tschmidt [at] suse.de>
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

package de.suse.swamp.core.util;

import java.io.*;
import java.text.*;
import java.util.*;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.*;
import org.apache.commons.httpclient.methods.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.data.*;
import de.suse.swamp.core.data.datatypes.*;
import de.suse.swamp.core.security.*;
import de.suse.swamp.util.*;


public class BugzillaTools {

    private String errormsg = null;
    private Hashtable bugData = null;
    private static Cookie[] cookies = null;
    private SWAMP swamp = SWAMP.getInstance();

    // extra logger for bugzilla stuff
    public static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(
            "de.suse.swamp.core.util.BugzillaTools");

    // cache bug data with same excludefield parameters
    // key: bugid value: Hashtable with keys: excludefields, date, <bugdatafields>
    public static Hashtable bugzillaCache = new Hashtable();
    
    public BugzillaTools () { }


    public void fetchBugzillaInfo(Dataset dataset, int bugid) throws Exception {
        ArrayList excludeFields = new ArrayList();
        if (dataset != null && !dataset.containsDatabit("description")) {
            excludeFields.add("long_desc");
        }

        Hashtable bug = getBugData(bugid, excludeFields);
        // only store if a dataset is provided
        if (dataset != null) {
            for (Iterator it = dataset.getDatabits().iterator(); it.hasNext();) {
                Databit bit = (Databit) it.next();
                String bitName = bit.getName();
                if (bitName.equals("people")) {
                    if (bit.setValue(((SWAMPHashSet) bug.get("people")).toString(", "), SWAMPUser.SYSTEMUSERNAME)) {
						Logger.LOG("Bugzilla copy: " + bitName + "=" + bug.get(bitName), log);
					}
                } else if (bitName.equals("delta_time") && bug.containsKey("delta")) {
                    DateFormat df1 = new SimpleDateFormat(dateDatabit.dateFormat);
                    bit.setValue(df1.format((Date) bug.get("delta")), SWAMPUser.SYSTEMUSERNAME);
                } else if (bug.containsKey(bitName)) {
                    if (bit.setValue((String) bug.get(bitName), SWAMPUser.SYSTEMUSERNAME)) {
                        Logger.DEBUG("Bugzilla copy: " + bitName + "=" + bug.get(bitName), log);
                    }
                }
            }
        }
    }


    /**
     * This method will return a HashMap with key= name of bugzilla xml element
     * value = value of the elements CDDATA.
     * additional values are "people", a SWAMPHashSet of people contained in the bug
     * "delta": Date of the last action in the bug
     * "description": String - the first comment
     *
     * @param bugid
     * @return
     * @throws Exception
     */
    public Hashtable getBugData(int bugid, List excludeFields) throws Exception {
        
        /* Hashtable cacheBugData = getCacheEntry(bugid, excludeFields);
        if (cacheBugData != null) {
            Logger.LOG("Reading Bugzilla XML for bug #" + bugid + " from cache.", log);
            return cacheBugData;
        }
        
        String queryUrl = swamp.getProperty("BUGZILLA_QUERYURL") + bugid;
        queryUrl += "&excludefield=attachment";
        if (excludeFields != null && excludeFields.size() > 0){
            for (Iterator it = excludeFields.iterator(); it.hasNext(); ){
                queryUrl += "&excludefield=" + it.next();
            }
        }
        Logger.LOG("Reading Bugzilla XML for bug #" + bugid, log);
        try {
            xmlToData(queryUrl);
            if (bugData.get("assigned_to") == null) {
                Logger.ERROR("Bugzilla session not valid. Trying new login...");
                BugzillaTools.cookies = null;
                xmlToData(queryUrl);
            }
        } catch (Exception e) {
            if (e.getMessage().indexOf("NotPermitted") >= 0){
                Logger.ERROR("Bugzilla session not valid. Trying new login...");
                BugzillaTools.cookies = null;
                xmlToData(queryUrl);
            } else {
                throw e;
            }
        }
        bugData.put("excludefields", excludeFields);
        bugData.put("date", new Date());
        synchronized (bugzillaCache) {
            bugzillaCache.put(new Integer(bugid), bugData);
        }
        return bugData; */
        Hashtable h = new Hashtable();
        h.put("foo", "bar");
        return h;	
    }

    
    public Hashtable getBugData(String bugid, List excludeFields) throws Exception {
        return getBugData(Integer.valueOf(bugid).intValue(), excludeFields);
    }
    
    
    private Cookie[] getCookies() throws Exception {
        if (cookies == null) {
			cookies = bzConnect(swamp.getProperty("BUGZILLA_USER"),
                    swamp.getProperty("BUGZILLA_PASSWORD"));
		}
        return cookies;
    }



    private Cookie[] bzConnect(String username, String pwd) throws Exception {
        Logger.DEBUG("Performing bugzilla login...");
        HttpState initialState = new HttpState();
        // Do a Login at Bugzilla
        HttpClient httpclient = new HttpClient();
        httpclient.setState(initialState);

        String loginUrl = swamp.getProperty("BUGZILLA_LOGIN_URL");
        // add form fields for logging in:
        String usernameField = swamp.getProperty("BUGZILLA_LOGIN_FORM_USERNAME");
        String passwordField = swamp.getProperty("BUGZILLA_LOGIN_FORM_PWD");
        NameValuePair login = new NameValuePair(usernameField, username);
        NameValuePair pw = new NameValuePair(passwordField, pwd);
        NameValuePair loginid = new NameValuePair("GoAheadAndLogIn", "1");

        PostMethod httppost = new PostMethod(loginUrl);
        httppost.setRequestBody(new NameValuePair[] { login, pw, loginid });
        try {
            httpclient.executeMethod(httppost);
        } catch (Exception e) {
            throw new Exception("Could not connect to " + loginUrl + " (error: " + e.getMessage() + ")");
        }
        Cookie[] cookies = httpclient.getState().getCookies();
        //System.out.println("Response: " + httppost.getResponseBodyAsString());
        if (cookies == null || cookies.length == 0) {
            throw new Exception("Could not login to " + loginUrl);
        }
        httppost.releaseConnection();
        return cookies;
    }


    private synchronized void xmlToData(String url) throws Exception {

        HttpState initialState = new HttpState();

        String authUsername  = swamp.getProperty("BUGZILLA_AUTH_USERNAME");
        String authPassword  = swamp.getProperty("BUGZILLA_AUTH_PWD");

        if (authUsername != null && authUsername.length() != 0) {
            Credentials defaultcreds = new UsernamePasswordCredentials(authUsername, authPassword);
            initialState.setCredentials(AuthScope.ANY, defaultcreds);
        }
        else {
            Cookie[] cookies = getCookies();
            for (int i = 0; i < cookies.length; i++) {
                initialState.addCookie(cookies[i]);
                Logger.DEBUG("Added Cookie: " + cookies[i].getName() + "=" + cookies[i].getValue(), log);
            }
        }
        HttpClient httpclient = new HttpClient();
        httpclient.setState(initialState);
        HttpMethod httpget = new GetMethod(url);
        try {
            httpclient.executeMethod(httpget);
        } catch (Exception e) {
            throw new Exception("Could not get URL " + url);
        }

        String content = httpget.getResponseBodyAsString();
        char [] chars = content.toCharArray();

        // removing illegal characters from bugzilla output.
        for (int i = 0; i<chars.length; i++){
            if (chars[i] < 32 && chars[i] != 9 && chars[i] != 10 && chars[i] != 13) {
                Logger.DEBUG("Removing illegal character: '" + chars[i] + "' on position " + i, log);
                chars[i] = ' ';
            }
        }
        Logger.DEBUG(String.valueOf(chars), log);
        CharArrayReader reader = new CharArrayReader(chars);
        XMLReader parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
        parser.setFeature("http://xml.org/sax/features/validation", false);
        // disable parsing of external dtd
        parser.setFeature("http://xml.org/sax/features/external-general-entities", false);
        parser.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        parser.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        // get XML File
        BugzillaReader handler = new BugzillaReader();
        parser.setContentHandler(handler);
        InputSource source = new InputSource();
        source.setCharacterStream(reader);
        source.setEncoding("utf-8");
        try {
            parser.parse(source);
        } catch (SAXParseException spe) {
            spe.printStackTrace();
            throw spe;
        }
        httpget.releaseConnection();
        if (errormsg != null) {
            throw new Exception(errormsg);
        }
    }

    private Hashtable getCacheEntry(int bugId, List excludefields) {
        synchronized (bugzillaCache) {
            cacheMaintenance();
            Hashtable cacheEntry = (Hashtable) bugzillaCache.get(new Integer(bugId));
            if (cacheEntry != null
                    && (cacheEntry.get("excludefields").equals(excludefields)
                    || cacheEntry.get("excludefields") == null || ((List) cacheEntry.get("excludefields"))
                            .size() == 0)) {
                return (Hashtable) bugzillaCache.get(new Integer(bugId));
            }
        }
        return null;
    }

    
    private void cacheMaintenance() {
        // do maintenance, remove outdated entries
        List removeIds = new ArrayList();
        synchronized (bugzillaCache) {
            for (Iterator it = bugzillaCache.keySet().iterator(); it.hasNext();) {
                Integer id = (Integer) it.next();
                Date date = (Date) ((Hashtable) bugzillaCache.get(id)).get("date");
                Date outDate = new Date(new Date().getTime() - 1000 * 60 * 20);
                if (date == null || date.before(outDate))
                    removeIds.add(id);
            }
            for (Iterator it = removeIds.iterator(); it.hasNext();) {
                Integer id = (Integer) it.next();
                bugzillaCache.remove(id);
                Logger.DEBUG("Bug cache outdated: " + id);
            }
        }
    }
    
    
    /*
     * make that an inner class so that we can access all these private fields
     * defined above to keep track of the parsing process
     */
    private class BugzillaReader extends DefaultHandler {

        // PCDATA is collected in a stack as well...
        private Stack pcdataStack = new Stack();


        private BugzillaReader() {
            super();
        }

        public void startDocument() {
            Logger.DEBUG("Start reading Bugzilla XML.", log);
            bugData = new Hashtable();
            bugData.put("people", new SWAMPHashSet());
            bugData.put("description", "");
            errormsg = null;
        }

        public void endDocument() {
            Logger.DEBUG("Finished reading Bugzilla XML.", log);
        }


        public void startElement(String uri, String name, String qName,
            Attributes atts) {
            pcdataStack.push(new String());
            Logger.DEBUG("Start of Element " + qName, log);
            if (qName.equals("bug")) {
                for (int i = 0; i < atts.getLength(); i++) {
                    if (atts.getQName(i).equals("error")) {
                        Logger.ERROR("Bugzilla Error: " + atts.getValue(i), log);
                        errormsg = "Bugzilla Error: " + atts.getValue(i).toString();
                    }
                }
            }
        }


        public void endElement(String uri, String name, String qName) {
            String pcdata = "";
            if (!pcdataStack.empty()) {
                pcdata = ((String) pcdataStack.pop());
            }
            // adding the values to the hash
            Logger.DEBUG("End of Element " + qName + " with value " + pcdata.trim(), log);
            bugData.put(qName, pcdata);

            if (qName.equals("who") || qName.equals("cc") || qName.equals("reporter") || qName.equals("assigned_to")) {
                SWAMPHashSet people = (SWAMPHashSet) bugData.get("people");
                people.add(pcdata);
            } else if (qName.equals("delta_ts")) {
                DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                try {
                	bugData.put("delta", df1.parse(pcdata));
                } catch (ParseException e) {
                    Logger.ERROR("Cannot parse delta time: " + pcdata, log);
                }
            } else if (qName.equals("thetext")) {
            	if (bugData.get("description").equals("")){
            		bugData.put("description", pcdata);
            	}
            }
        }


        public void characters(char ch[], int start, int length) {
            String oldpcdata = "";
            String pcdata = new String(ch, start, length);
            Logger.DEBUG("Reading characters:" + pcdata.trim(), log);

            if (!pcdataStack.empty()) {
                oldpcdata = (String) pcdataStack.pop();
            }
            pcdata = oldpcdata + pcdata;
            /* Put new value for pcdata on stack */
            pcdataStack.push(pcdata);
        }
    }
}
