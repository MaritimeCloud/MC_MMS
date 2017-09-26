package kr.ac.kaist.message_relaying;
/* -------------------------------------------------------- */
/** 
File name : MessageTypeDecision.java
	It decides type of a message.
Author : Jaehyun Park (jae519@kaist.ac.kr)
	Jin Jung (jungst0001@kaist.ac.kr)
Creation Date : 2017-01-24
Version : 0.3.01

Rev. history : 2017-02-01
	Added log providing features.
	Added locator registering features.
Modifier : Jaehee Ha (jaehee.ha@kaist.ac.kr)

Rev. history : 2017-04-25 
Version : 0.5.0
	Revised class name from MessageTypeDecision to MessageTypeDecider.
Modifier : Jaehee Ha (jaehee.ha@kaist.ac.kr)

Rev. history : 2017-04-29
Version : 0.5.3
	Added system log features
Modifier : Jaehee Ha (jaehee.ha@kaist.ac.kr)

Rev. history : 2017-06-17
Version : 0.5.6
	Added polling method switching features
Modifier : Jaehee Ha (jaehee.ha@kaist.ac.kr) 

Rev. history : 2017-06-19
Version : 0.5.7
	Applied LogBack framework in order to log events
Modifier : Jaehee Ha (jaehee.ha@kaist.ac.kr)

Rev. history : 2017-06-27
Version : 0.5.8
	Added RELAYING_TO_MULTIPLE_SC.
	Added EMTPY_QUEUE_LOGS.
Modifier : Jaehyun Park (jae519@kaist.ac.kr)
		   Jaehee Ha (jaehee.ha@kaist.ac.kr)
		   
Rev. history : 2017-07-28
Version : 0.5.9
	Added null MRN and invalid MRN cases. 
Modifier : Jaehee Ha (jaehee.ha@kaist.ac.kr)

Rev. history : 2017-09-26
Version : 0.6.0
	Added adding mrn entry case.
	Removed empty queue logs case.
	Added enum msgType and removed public integers.
	Replaced from random int SESSION_ID to String SESSION_ID as connection context channel id.
Modifier : Jaehee Ha (jaehee.ha@kaist.ac.kr)

*/
/* -------------------------------------------------------- */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.handler.codec.http.HttpMethod;
import kr.ac.kaist.message_casting.MessageCastingHandler;
import kr.ac.kaist.mms_server.MMSConfiguration;

class MessageTypeDecider {
	
	private static final Logger logger = LoggerFactory.getLogger(MessageTypeDecider.class);
	private String SESSION_ID = "";
	
	
	public static enum msgType {
			POLLING,
			RELAYING_TO_SC,
			RELAYING_TO_SERVER,
			REGISTER_CLIENT,
			UNKNOWN_MRN,
			UNKNOWN_HTTP_TYPE,
			STATUS,EMPTY_MNSDummy,
			REMOVE_MNS_ENTRY,
			ADD_MNS_ENTRY,
			POLLING_METHOD,
			RELAYING_TO_MULTIPLE_SC,
			NULL_SRC_MRN,
			NULL_DST_MRN,
			NULL_MRN,
			INVALID_SRC_MRN,
			INVALID_DST_MRN
	}

	
	MessageTypeDecider(String sessionId) {
		this.SESSION_ID = sessionId;
	}
	
	msgType decideType(MessageParser parser, MessageCastingHandler mch) {
		String srcMRN = parser.getSrcMRN();
		String dstMRN = parser.getDstMRN();
		HttpMethod httpMethod = parser.getHttpMethod();
		String uri = parser.getUri();
		
//		when WEB_LOG_PROVIDING
		if (MMSConfiguration.WEB_LOG_PROVIDING && httpMethod == HttpMethod.GET && uri.equals("/status")){
			return msgType.STATUS;
		}
   	
   	
//		when WEB_MANAGING

	   	else if (MMSConfiguration.WEB_MANAGING && httpMethod == HttpMethod.GET && uri.equals("/emptymnsdummy")){ 
	   		return msgType.EMPTY_MNSDummy;
	   	} 
	   	else if (MMSConfiguration.WEB_MANAGING && httpMethod == HttpMethod.GET && uri.regionMatches(0, "/addmnsentry?mrn", 0, 16)){ 
	   		return msgType.ADD_MNS_ENTRY;
	   	} 
	   	else if (MMSConfiguration.WEB_MANAGING && httpMethod == HttpMethod.GET && uri.regionMatches(0, "/removemnsentry", 0, 15)){ 
	   		return msgType.REMOVE_MNS_ENTRY;
	   	} 
	   	else if (MMSConfiguration.WEB_MANAGING && httpMethod == HttpMethod.GET && uri.regionMatches(0,"/polling?method", 0, 15)){
	   		return msgType.POLLING_METHOD;
	   	} 
		
		
//		When MRN(s) is(are) null
	   	else if (srcMRN == null && dstMRN == null) {
			return msgType.NULL_MRN;
		}
		else if (srcMRN == null) {
			return msgType.NULL_SRC_MRN;
		}
		else if (dstMRN == null) {
			return msgType.NULL_DST_MRN;
		}
		
		
//    	When polling
		else if (httpMethod == HttpMethod.POST && uri.equals("/polling") && dstMRN.equals(MMSConfiguration.MMS_MRN)) {
    		return msgType.POLLING; 
    	}
    	
//		when registering
    	else if (httpMethod == HttpMethod.POST && uri.equals("/registering") && dstMRN.equals(MMSConfiguration.MMS_MRN)) {
    		return msgType.REGISTER_CLIENT;
    	}
    	

    	
//    	When relaying
    	else {
    		String dstInfo = mch.requestDstInfo(dstMRN);
    		
        	if (dstInfo.equals("No")) {
        		return msgType.UNKNOWN_MRN;
        	}
        	if (dstInfo.regionMatches(0, "MULTIPLE_MRN,", 0, 9)){
        		parser.parseMultiDstInfo(dstInfo);
        		return msgType.RELAYING_TO_MULTIPLE_SC;
        	}

        	parser.parseDstInfo(dstInfo);
        	int model = parser.getDstModel();
        	
        	if (model == 2) {//model B (destination MSR, MIR, or MSP as servers)
        		return msgType.RELAYING_TO_SERVER;
        	} 
        	else {//when model A, it puts the message into the queue
        		return msgType.RELAYING_TO_SC;
        	}
    	} /*else {
    		return UNKNOWN_HTTP_TYPE;
    	}*/
	}
	

}
