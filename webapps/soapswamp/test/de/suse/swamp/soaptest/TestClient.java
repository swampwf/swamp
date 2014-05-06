package de.suse.swamp.soaptest;

import java.util.*;

import org.apache.axis.client.*;
import org.apache.log4j.*;

import SWAMP.*;

public class TestClient {

	 private static String endpoint = "http://localhost:8080/axis/services/swamp";
	 private static String username = "";
	 private static String password = "";
	 private static Logger log = Logger.getLogger("de.suse.swamp.soaptest.TestClient");


   public static void main(String [] args) throws Exception {

	   BasicConfigurator.configure();
	   log.setLevel(Level.DEBUG);

	   SwampSoapBindingStub soapStub =
		   new SwampSoapBindingStub(new java.net.URL(endpoint), new Service());


	   HashMap filter = new HashMap();
	   filter.put("systemfilter_wftemplate", "JobTracker");

	   Object[] ids = soapStub.getWorkflowIdList(filter, username, password);
	   for (int i = 0; i<ids.length; i++) {
		   log.debug("got wfid: " + ids[i]);
	}

   }


   /** Manually do a SOAP call on a remote server
    */
   private static Object doSoapCall(String method, Object [] parameter) throws Exception {
       Service  service = new Service();
       Call call = (Call) service.createCall();
       call.setTargetEndpointAddress( new java.net.URL(endpoint) );
       call.setOperationName( method );
       Object val = null;
       try {
    	   val = call.invoke(parameter);
       } catch (Exception e) {
           System.out.println("Got error : " + e.getMessage());
       }
       return val;
   }

}
