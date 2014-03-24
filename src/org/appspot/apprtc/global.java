package org.appspot.apprtc;

import com.xhmm.ftpupload.ftpUploadEngine;
import com.xhmm.xmpp.xmppEngine;
import com.xhmm.xmpp.xmppMessage;
//封装XMPP接口全局的变量
public class global {
	public static xmppEngine g_xmppEngine = null;
	public static ftpUploadEngine g_ftpUploadEngine = null;
	public static xmppMessage g_offer_msg=null;
}
