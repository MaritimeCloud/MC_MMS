package kr.ac.kaist.mms_client;
/* -------------------------------------------------------- */
/** 
File name : SecureMMSSndHandler.java
Author : Jaehee Ha (jaehee.ha@kaist.ac.kr)
Creation Date : 2017-03-21
Version : 0.4.0

Rev. history : 2017-04-20 
Version : 0.5.0
Modifier : Jaehee Ha (jaehee.ha@kaist.ac.kr)

Rev. history : 2017-04-25
Modifier : Jaehee Ha (jaehee.ha@kaist.ac.kr)

Rev. history : 2017-06-18
Version : 0.5.6
	Changed the variable Map<String,String> headerField to Map<String,List<String>>
Modifier : Jaehee Ha (jaehee.ha@kaist.ac.kr)

Rev. history : 2018-04-23
Version : 0.7.1
	Removed IMPROPER_CHECK_FOR_UNUSUAL_OR_EXCEPTIONAL_CONDITION, EXPOSURE_OF_SYSTEM_DATA hazard.
Modifier : Jaehee Ha (jaehee.ha@kaist.ac.kr)

Rev. history : 2018-07-19
Version : 0.7.2
	Added API; message sender guarantees message sequence .
Modifier : Jaehee Ha (jaehee.ha@kaist.ac.kr)

Rev. history : 2018-07-27
Version : 0.7.2
	Revised setting header field function.
Modifier : Jaehee Ha (jaehee.ha@kaist.ac.kr)

Rev. history : 2018-08-01
Version : 0.7.2
	Updated header field setter function.
Modifier : Jaehee Ha (jaehee.ha@kaist.ac.kr)

Rev. history : 2019-03-08
Version : 0.8.1
	Removed locator registration function.
Modifier : Jaehee Ha (jaehee.ha@kaist.ac.kr)

Rev. history : 2019-04-29
Version : 0.8.2
	Revised Base64 Encoder/Decoder.
Modifier : Jaehee Ha (jaehee.ha@kaist.ac.kr)

Rev. history : 2019-07-11
Version : 0.9.3
	Updated exception throw-catch phrases.
Modifier : Jaehee Ha (jaehee.ha@kaist.ac.kr)

Rev. history : 2019-07-21
Version : 0.9.4
	Moved write stream close() to the line before input stream close().
Modifier : Jaehee Ha (jaehee.ha@kaist.ac.kr)

Rev. history : 2019-07-24
Version : 0.9.4
	Added timeout parameter to sendPostMsgWithTimeout() methods.
Modifier : Jaehee Ha (jaehee.ha@kaist.ac.kr)

 Rev. history : 2019-07-26
 Version : 0.9.4
 	Let methods have timeout parameter default.
 Modifier : Jaehee Ha (jaehee.ha@kaist.ac.kr)
*/
/* -------------------------------------------------------- */

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.TrustManager;

import javax.net.ssl.*;

class SecureMMSSndHandler {
	private String TAG = "[SecureMMSSndHandler] ";
	private final String USER_AGENT = MMSConfiguration.USER_AGENT;
	private String clientMRN = null;
	private boolean isRgstLoc = false;
	private SecureMMSClientHandler.ResponseCallback myCallback;
	private HostnameVerifier hv = null;
	
	SecureMMSSndHandler (String clientMRN){
		this.clientMRN = clientMRN;
		hv = getHV();
	}
	
	void setResponseCallback (SecureMMSClientHandler.ResponseCallback callback){
		this.myCallback = callback;
	}
	
	void sendHttpsPostWithTimeout(String dstMRN, String loc, String data, Map<String,List<String>> headerField, int timeout) throws IOException{
		sendHttpsPostWithTimeout(dstMRN, loc, data, headerField, -1, timeout);
	}
	void sendHttpsPostWithTimeout(String dstMRN, String loc, String data, Map<String,List<String>> headerField, int seqNum, int timeout) throws IOException{
        
		String url = "https://"+MMSConfiguration.MMS_URL; // MMS Server
		if (!loc.startsWith("/")) {
			loc = "/" + loc;
		}
		url += loc;
		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
		con.setHostnameVerifier(hv);
		
		
		//add request header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Charset", "UTF-8");
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		con.setRequestProperty("srcMRN", clientMRN);
		if (dstMRN != null) {
			con.setRequestProperty("dstMRN", dstMRN);
		}
		//con.addRequestProperty("Connection","keep-alive");
		if (seqNum != -1) {
			con.setRequestProperty("seqNum", ""+seqNum);
		}
		if (headerField != null) {
			con = addCustomHeaderField(con, headerField);
		} 
		
		if (timeout > 0) {
			con.setConnectTimeout(timeout);
			con.setReadTimeout(timeout);
		}
		
		//load contents
		String urlParameters = data;
		

		if(MMSConfiguration.DEBUG) {System.out.println(TAG+"urlParameters: "+urlParameters);}
		
		// Send post request
		con.setDoOutput(true);
		BufferedWriter wr = new BufferedWriter(
				new OutputStreamWriter(con.getOutputStream(),Charset.forName("UTF-8")));
		wr.write(urlParameters);
		wr.flush();
		//wr.close();

		Map<String,List<String>> inH = con.getHeaderFields();
		inH = getModifiableMap(inH);
		int responseCode = 0;
		InputStream inStream = null;
		if (con.getResponseCode() != HttpURLConnection.HTTP_ENTITY_TOO_LARGE) {
			responseCode = con.getResponseCode();
			inStream = con.getInputStream();
		} else {
		     /* error from server */
			responseCode = con.getResponseCode();
			inStream = new ByteArrayInputStream("HTTP 413 Error: HTTP Entity Too Large".getBytes());
		}
		List<String> responseCodes = new ArrayList<String>();
		responseCodes.add(responseCode+"");
		inH.put("Response-code", responseCodes);
		if(MMSConfiguration.DEBUG){
			System.out.println("\n"+TAG+"Sending 'POST' request to URL : " + url);
			System.out.println(TAG+"Post parameters : " + urlParameters);
			System.out.println(TAG+"Response Code : " + responseCode);
		}
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(inStream,Charset.forName("UTF-8")));
		String inputLine;
		StringBuffer response = new StringBuffer();
		
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		
		wr.close();
		in.close();
		if(MMSConfiguration.DEBUG) {System.out.println(TAG+"Response: " + response.toString() + "\n");}
		
		
		
		receiveResponse(inH, response.toString());
		return;
    } 
	
	//OONI
	String sendHttpsGetFileWithTimeout(String dstMRN, String fileName, Map<String,List<String>> headerField, int timeout) throws IOException {

		String url = "https://"+MMSConfiguration.MMS_URL; // MMS Server
		if (!fileName.startsWith("/")) {
			fileName = "/" + fileName;
		}
		url += fileName;
		URL obj = new URL(url);
		
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		if (timeout > 0) {
			con.setConnectTimeout(timeout);
			con.setReadTimeout(timeout);
		}

		con.setHostnameVerifier(hv);
		
		
		//add request header
		con.setRequestMethod("GET");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Charset", "UTF-8");
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		con.setRequestProperty("srcMRN", clientMRN);
		if (dstMRN != null) {
			con.setRequestProperty("dstMRN", dstMRN);
		}
		if (headerField != null) {
			con = addCustomHeaderField(con, headerField);
		}
		//con.addRequestProperty("Connection","keep-alive");

		int responseCode = con.getResponseCode();
		if(MMSConfiguration.DEBUG) {
			System.out.println("\n"+TAG+"Sending 'GET' request to URL : " + url);
			System.out.println(TAG+"Response Code : " + responseCode + "\n");
		}
		
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer inputMsg = new StringBuffer();
		
		while ((inputLine = in.readLine()) != null) {
			inputMsg.append(inputLine+"\n");
		}
		
		Base64.Decoder base64Decoder = Base64.getDecoder();
        byte[] encoded = inputMsg.toString().getBytes("UTF-8");
        byte[] decoded = null;

        base64Decoder.decode(encoded, decoded);      

        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(System.getProperty("user.dir")+fileName));
        
        bos.write(decoded);
        bos.close();
		in.close();
		return fileName + " is saved";
	}
	//OONI end
	
	//HJH
	void sendHttpsGetWithTimeout(String dstMRN, String loc, String params, Map<String,List<String>> headerField, int timeout) throws IOException {
		sendHttpsGetWithTimeout(dstMRN, loc, params, headerField, -1, timeout);
	}
	void sendHttpsGetWithTimeout(String dstMRN, String loc, String params, Map<String,List<String>> headerField, int seqNum, int timeout) throws IOException {

		String url = "https://"+MMSConfiguration.MMS_URL; // MMS Server
		if (!loc.startsWith("/")) {
			loc = "/" + loc;
		}
		url += loc;
		if (params != null) {
			if (params.equals("")) {
				
			}
			else if (params.startsWith("?")) {
				url += params;
			} else {
				url += "?" + params;
			}
		}
		
		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		if (timeout > 0) {
			con.setConnectTimeout(timeout);
			con.setReadTimeout(timeout);
		}

		con.setHostnameVerifier(hv);
		
		//add request header
		con.setRequestMethod("GET");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Charset", "UTF-8");
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		if (clientMRN != null) {
			con.setRequestProperty("srcMRN", clientMRN);
		}
		if (dstMRN != null) {
			con.setRequestProperty("dstMRN", dstMRN);
		}
		if (seqNum != -1) {
			con.setRequestProperty("seqNum", ""+seqNum);
		}
		if (headerField != null) {
			con = addCustomHeaderField(con, headerField);
		}
		//con.addRequestProperty("Connection","keep-alive");

		Map<String,List<String>> inH = con.getHeaderFields();
		inH = getModifiableMap(inH);
		int responseCode = con.getResponseCode();
		List<String> responseCodes = new ArrayList<String>();
		responseCodes.add(responseCode+"");
		inH.put("Response-code", responseCodes);
		if(MMSConfiguration.DEBUG) {
			System.out.println("\n"+TAG+"Sending 'GET' request to URL : " + url);
			System.out.println(TAG+"Response Code : " + responseCode);
		}
		
		
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream(),Charset.forName("UTF-8")));
		String inputLine;
		StringBuffer response = new StringBuffer();
		
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		
		in.close();
		if(MMSConfiguration.DEBUG) {System.out.println(TAG+"Response: " + response.toString() + "\n");}
		receiveResponse(inH, response.toString());
		return;
	}
	
	HostnameVerifier getHV (){
		// Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
        };
        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (NoSuchAlgorithmException e) {
        	System.out.print(TAG+" Exception: "+ e.getLocalizedMessage());
        	if(MMSConfiguration.DEBUG){e.printStackTrace();}
        } catch (KeyManagementException e) {
        	System.out.print(TAG+" Exception: "+ e.getLocalizedMessage());
        	if(MMSConfiguration.DEBUG){e.printStackTrace();}
        }
        
        HostnameVerifier hv = new HostnameVerifier() {
            public boolean verify(String urlHostName, SSLSession session) {
            	if(MMSConfiguration.DEBUG) {
            		System.out.println(TAG+"Warning: URL Host: " + urlHostName + " vs. "
                        + session.getPeerHost());
            	}
                return true;
            }
        };
        
        return hv;
	}
	
	void receiveResponse (Map<String,List<String>> headerField, String message) {
		
		if (!isRgstLoc){
			isRgstLoc = false;
			try{
				myCallback.callbackMethod(headerField, message);
			} catch (NullPointerException e) {
				System.out.println(TAG+"NullPointerException : Have to set response callback interface! SecureMMSClientHandler.setSender()");
			}
			return;
		}
	}
	
	private Map<String, List<String>> getModifiableMap (Map<String, List<String>> map) {
		Map<String, List<String>> ret = new HashMap<String, List<String>>();
		Set<String> resHeaderKeyset = map.keySet(); 
		for (Iterator<String> resHeaderIterator = resHeaderKeyset.iterator();resHeaderIterator.hasNext();) {
			String key = resHeaderIterator.next();
			List<String> values = map.get(key);
			ret.put(key, values);
		}
	
		return ret;
	}
	
	private HttpsURLConnection addCustomHeaderField (HttpsURLConnection con, Map<String,List<String>> headerField) {
		HttpsURLConnection retCon = con;
		if(MMSConfiguration.DEBUG) {System.out.println(TAG+"set headerfield[");}
		for (Iterator<String> keys = headerField.keySet().iterator() ; keys.hasNext() ;) {
			String key = (String) keys.next();
			List<String> valueList = (List<String>) headerField.get(key);
			if (valueList != null) {
				if (valueList.size() == 1) {
					if(MMSConfiguration.DEBUG) {System.out.println(key+":"+valueList.get(0));}
					retCon.addRequestProperty(key, valueList.get(0));
				}
				else if (valueList.size() > 1) { 
					
					StringBuilder valueBuf = new StringBuilder();
					if(MMSConfiguration.DEBUG) {System.out.print(key+":[");}
					valueBuf.append("[");
					int i = 0;
					for (i = 0 ; i < valueList.size() ; i++) {
						String value = valueList.get(i);
						if(MMSConfiguration.DEBUG) {System.out.print(value);}
						//valueBuf.append("\""+value+"\"");
						valueBuf.append(value);
						if (i != valueList.size()-1) {
							if(MMSConfiguration.DEBUG) {System.out.print(",");}
							valueBuf.append(",");
						}
					}
					if(MMSConfiguration.DEBUG) {System.out.println("]");}
					valueBuf.append("]");
					retCon.addRequestProperty(key, valueBuf.toString());
				}
				else {
					if(MMSConfiguration.DEBUG) {System.out.println(key+":");}
					retCon.addRequestProperty(key, "");
				}
			}
			else if (valueList == null) {
				if(MMSConfiguration.DEBUG) {System.out.println(key+":null");}
				retCon.addRequestProperty(key, null);
			}
		}
		if(MMSConfiguration.DEBUG) {System.out.println("]");}
		return retCon;
	}
	
	//HJH end
	
}
