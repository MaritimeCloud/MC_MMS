import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import kr.ac.kaist.mms_client.MMSClientHandler;
import kr.ac.kaist.mms_client.MMSConfiguration;

/** 
File name : Server.java
	Polling messsage authentication tests
Author : Jin Jeong (jungst0001@kaist.ac.kr)
Creation Date : 2019-04-16

Rev. history : 2019-04-22
	Add server which is MMSClientHandler.
Modifier : Jin Jeong (jungst0001@kaist.ac.kr)
*/

public class TS6_Server {
	private String myMRN = null;
	private String dstMRN = null;
	private int port = 8902;
	private MMSClientHandler server = null;
	private MMSClientHandler sender = null;
	
	public TS6_Server() {
		this.myMRN = TS6_Test.serverMRN;
		this.dstMRN = TS6_Test.clientMRN;
		MMSConfiguration.MMS_URL = TS6_Test.MMS_URL;
		
		try {
			sender = new MMSClientHandler(myMRN);
			sender.setSender(new MMSClientHandler.ResponseCallback() {
				
				@Override
				public void callbackMethod(Map<String, List<String>> headerField, String message) {
					// TODO Auto-generated method stub
					
				}
			});
			
			server = new MMSClientHandler(myMRN);
			server.setServerPort(port, new MMSClientHandler.RequestCallback() {
				//Request Callback from the request message
				//it is called when client receives a message
				
				@Override
				public int setResponseCode() {
					// TODO Auto-generated method stub
					return 200;
				}
				
				@Override
				public String respondToClient(Map<String,List<String>> headerField, String message) {
					
					return "OK";
				}

				@Override
				public Map<String, List<String>> setResponseHeader() {
					// TODO Auto-generated method stub
					return null;
				}
			}); 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void sendMessage(String data) {
		try {
			sender.sendPostMsg(dstMRN, data);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String getMyMRN() { return myMRN; }
}
