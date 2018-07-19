package kr.ac.kaist.mms_client;

/* -------------------------------------------------------- */
/** 
File name : SecureMMSRcvHandler.java
Author : Jaehee Ha (jaehee.ha@kaist.ac.kr)
Creation Date : 2017-03-21
Version : 0.4.0

Rev. history : 2017-04-25
Version : 0.5.0
Modifier : Jaehee Ha (jaehee.ha@kaist.ac.kr)
	Moved PollHandler class into MMSPollHandler.java
	
Rev. history : 2017-05-02
Version : 0.5.4
	Added setting response header
Modifier : Jaehee Ha (jaehee.ha@kaist.ac.kr)

Rev. history : 2018-04-23
Version : 0.7.1
	Removed FORWARD_NULL, RESOURCE_LEAK, IMPROPER_CHECK_FOR_UNUSUAL_OR_EXCEPTIONAL_CONDITION hazard.
Modifier : Jaehee Ha (jaehee.ha@kaist.ac.kr)	
*/
/* -------------------------------------------------------- */

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import sun.misc.BASE64Encoder;

class SecureMMSRcvHandler {
	HttpsServer server = null;
	SSLContext sslContext = null;
	
	HttpsReqHandler hrh = null;
	//OONI
	SecureFileReqHandler frh = null;
	//OONI

	private String TAG = "[SecureMMSRcvHandler] ";
	private static final String USER_AGENT = MMSConfiguration.USER_AGENT;
	private String clientMRN = null;
	
	SecureMMSRcvHandler(int port, String jksDirectory, String jksPassword) throws Exception{
		httpsServerConfigure(port, jksDirectory, jksPassword);
		hrh = new HttpsReqHandler();
		if (hrh != null) {
			server.createContext("/", hrh);
		} else {
			throw new NullPointerException();
		}
        if(MMSConfiguration.DEBUG) {System.out.println(TAG+"Context \"/\" is created");}
        
        server.setExecutor(null); // creates a default executor
        //HttpsServer is in java library. Cannot fix FORWARD_NULL
        server.start();
        //HttpsServer is in java library. Cannot fix FORWARD_NULL
	}
	
	SecureMMSRcvHandler(int port, String context, String jksDirectory, String jksPassword) throws Exception {
		httpsServerConfigure(port, jksDirectory, jksPassword);
		hrh = new HttpsReqHandler();
		if (!context.startsWith("/")){
			context = "/" + context;
		}
		
        server.createContext(context, hrh);
        if(MMSConfiguration.DEBUG) {System.out.println(TAG+"Context \""+context+"\" is created");}
        server.setExecutor(null); // creates a default executor
        server.start();
	}
	
	SecureMMSRcvHandler(int port, String fileDirectory, String fileName, String jksDirectory, String jksPassword) throws Exception {
		httpsServerConfigure(port, jksDirectory, jksPassword);
        //OONI
        frh = new SecureFileReqHandler();
        if (!fileDirectory.startsWith("/")){
        	fileDirectory = "/" + fileDirectory;
		}
        if(!fileDirectory.endsWith("/")&&!fileName.startsWith("/")){
        	fileName = "/" + fileName;
        }
        if(fileDirectory.endsWith("/")&&fileName.startsWith("/")){
        	fileName = fileName.substring(1);
        }
        server.createContext(fileDirectory+fileName, frh);
        if(MMSConfiguration.DEBUG) {System.out.println(TAG+"Context \""+fileDirectory+fileName+"\" is created");}
        //OONI
        server.setExecutor(null); // creates a default executor
        server.start();
	}
	
	void httpsServerConfigure (int port, String jksDirectory, String jksPassword) throws Exception{
		
		FileInputStream fis = null;
		 KeyManagerFactory kmf = null;
		try {
			server = HttpsServer.create(new InetSocketAddress(port), 0);
			sslContext = SSLContext.getInstance( "TLS" );
			
			 // initialise the keystore
		    char[] jksPwCharArr = jksPassword.toCharArray ();
		    KeyStore ks = KeyStore.getInstance ( "JKS" );
		    //FileInputStream fis = new FileInputStream ( System.getProperty("user.dir")+"/testkey.jks" );
		    fis = new FileInputStream ( jksDirectory );
		    ks.load ( fis, jksPwCharArr );
	
		    // setup the key manager factory
		    kmf = KeyManagerFactory.getInstance ( "SunX509" );
		    kmf.init ( ks, jksPwCharArr );
		    
		    // setup the trust manager factory
		    TrustManagerFactory tmf = TrustManagerFactory.getInstance ( "SunX509" );
		    tmf.init ( ks );
	
		    // setup the HTTPS context and parameters
		    sslContext.init ( kmf.getKeyManagers (), tmf.getTrustManagers (), null );
		    server.setHttpsConfigurator ( new HttpsConfigurator( sslContext )
		    {
		        public void configure ( HttpsParameters params )
		        {
		            
		                // initialise the SSL context
		                SSLContext c = null;
						try {
							c = SSLContext.getDefault ();
							SSLEngine engine = c.createSSLEngine ();
			                params.setNeedClientAuth ( false );
			                params.setCipherSuites ( engine.getEnabledCipherSuites () );
			                params.setProtocols ( engine.getEnabledProtocols () );
		
			                // get the default parameters
			                SSLParameters defaultSSLParameters = c.getDefaultSSLParameters ();
			                params.setSSLParameters ( defaultSSLParameters );
						} catch (NoSuchAlgorithmException e) {
							// TODO Auto-generated catch block
			                System.err.println( "Failed to create HTTPS port" );
							//e.printStackTrace();
						}
		                
		            
		            
		        }
		    } );
		} finally {
			if (fis != null) {
				fis.close();
			}
			kmf = null;
		}
	    
	}
	
	void addContext (String context) {
		if (server == null) {
			System.out.println(TAG+"Server is not created!");
			return;			
		}
		if (hrh == null) {
			hrh = new HttpsReqHandler();
		}
		if (!context.startsWith("/")){
			context = "/" + context;
		}
        server.createContext(context, hrh);
        if(MMSConfiguration.DEBUG) {System.out.println(TAG+"Context \""+context+"\" is added");}
	}
	
	void addFileContext (String fileDirectory, String fileName) {
		if (server == null) {
			System.out.println(TAG+"Server is not created!");
			return;
		}
		if (frh == null) {
			frh = new SecureFileReqHandler();
		}
        if (!fileDirectory.startsWith("/")){
        	fileDirectory = "/" + fileDirectory;
		}
        if(!fileDirectory.endsWith("/")&&!fileName.startsWith("/")){
        	fileName = "/" + fileName;
        }
        if(fileDirectory.endsWith("/")&&fileName.startsWith("/")){
        	fileName = fileName.substring(1);
        }
        server.createContext(fileDirectory+fileName, frh);
        if(MMSConfiguration.DEBUG) {System.out.println(TAG+"Context \""+fileDirectory+fileName+"\" is added");}
	}
	
	class HttpsReqHandler implements HttpHandler {
    	SecureMMSClientHandler.RequestCallback myReqCallback;
    	
       	public void setRequestCallback(SecureMMSClientHandler.RequestCallback callback){
    		this.myReqCallback = callback;
    	}
    	
        @Override
        public void handle(HttpExchange t) throws IOException {
        	URI uri = t.getRequestURI();
        	List<String> uris = new ArrayList<String>();
        	uris.add(uri.toString());
        	String httpMethod = t.getRequestMethod();
        	List<String> httpMethods = new ArrayList<String>();
        	httpMethods.add(httpMethod);
        	
        	InputStream inB = t.getRequestBody();
        	Map<String,List<String>> inH = t.getRequestHeaders();
            ByteArrayOutputStream _out = new ByteArrayOutputStream();
            byte[] buf = new byte[2048];
            int read = 0;
            while ((read = inB.read(buf)) != -1) {
                _out.write(buf, 0, read);
            }
            
            String receivedData = new String( _out.toByteArray(), Charset.forName("UTF-8")).trim();
            
            String message = receivedData;
            inH.put("Http-method", httpMethods);
            inH.put("Uri", uris);
            
            String response = this.processRequest(inH, message);
            Map<String,List<String>> myHdr = setResponseHeader();
            Map<String,List<String>> resHdr = t.getResponseHeaders();
            
    		if (myHdr != null) {
    			if(MMSConfiguration.DEBUG) {System.out.println(TAG+"set headerfield[");}
    			for (Iterator keys = myHdr.keySet().iterator() ; keys.hasNext() ;) {
    				String key = (String) keys.next();
    				ArrayList<String> value = (ArrayList<String>) myHdr.get(key);
    				if(MMSConfiguration.DEBUG) {System.out.println(key+":"+value);}
    				resHdr.put(key, value);
    			}
    			if(MMSConfiguration.DEBUG) {System.out.println("]");}
    			
    		} 
           
            t.sendResponseHeaders(setResponseCode(), response.length());
            OutputStream os = t.getResponseBody();
            BufferedWriter wr = new BufferedWriter(
    				new OutputStreamWriter(os,Charset.forName("UTF-8")));
            wr.write(response);
            wr.flush();
            wr.close();
            os.close();
        }
        
        private String processRequest(Map<String,List<String>> headerField, String message) {
    		return this.myReqCallback.respondToClient(headerField, message);
    	}
        
        private int setResponseCode() {
        	return this.myReqCallback.setResponseCode();
        }
        
        private Map<String,List<String>> setResponseHeader(){
        	return this.myReqCallback.setResponseHeader();
        }
    }
    //Jaehee created
	//Jaehee modified
    class SecureFileReqHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
        	URI uri = null;
        	InputStream in = null;
        	ByteArrayOutputStream byteOutStream = null;
        	OutputStream os = null;
        	byte encodeBytes[] = null;
        	try {
	        	uri = t.getRequestURI();
	        	String fileName = uri.toString();
	        	if(MMSConfiguration.DEBUG) {System.out.println(TAG+"File request: "+fileName);}
	        	
	            fileName = System.getProperty("user.dir")+fileName.trim();
	            File file = new File (fileName);
	            BASE64Encoder base64Encoder = new BASE64Encoder();
	            in = new FileInputStream(file);
	
	            byteOutStream=new ByteArrayOutputStream();
	
	            int len=0;
	
	            byte[] buf = new byte[1024];
	
	            while((len=in.read(buf)) != -1){
	            	byteOutStream.write(buf, 0, len);
	            }
	
	            byte fileArray[]=byteOutStream.toByteArray();
	            encodeBytes=base64Encoder.encodeBuffer(fileArray).getBytes(); 
        	} finally {
        		if (in != null) {
        			in.close();
        		}
        		if (byteOutStream != null) {
        			byteOutStream.close();
        		}
                // ok, we are ready to send the response.
        	}
        	try {
                t.sendResponseHeaders(200, encodeBytes.length);
                os = t.getResponseBody();
                if (encodeBytes != null) {
                	os.write(encodeBytes,0,encodeBytes.length);
                }
                os.flush();
        	} finally {
        		if (os != null) {
        			os.close();
        		}
        	}
        }
    }
    //Jaehee end
}
