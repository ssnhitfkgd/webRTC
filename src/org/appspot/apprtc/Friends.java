package org.appspot.apprtc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.ReportedData;
import org.jivesoftware.smackx.ReportedData.Row;
import org.jivesoftware.smackx.search.UserSearchManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.xhmm.xmpp.IXmppListener.IXmppSMSListener;
import com.xhmm.xmpp.xmppEngine;
import com.xhmm.xmpp.xmppMessage;

public class Friends extends Activity implements RosterListener {
	// Handler mHandler;
	ArrayList<Object> list;
	MyBaseAdapter adapter;
	private Toast logToast;
	Friends mactivity;

	String url = null;
	String user = null;
	String credential = null;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mactivity = this;
		Intent i=getIntent();
		url  = i.getStringExtra("url");
		user  = i.getStringExtra("user");
		credential  = i.getStringExtra("credential");
		
		list = new ArrayList<Object>();
		adapter = new MyBaseAdapter(this);
		// Use an existing ListAdapter that will map an array
		// of strings to TextViews
		updateListview();

		//设置消息接受监听
		global.g_xmppEngine.setSMSListener(mXmppSMSListener);
		ListView ls = (ListView) findViewById(R.id.lvlist);
		//设置适配器
		ls.setAdapter(adapter);
		ls.setTextFilterEnabled(true);
		ls.setOnItemClickListener(new OnItemClickListener() {
			//好友列表点击回调
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				//?? TODO
				final int index = arg2;
				//friends list  on click
		        AlertDialog.Builder builder = new AlertDialog.Builder(mactivity);  
		        builder.setIcon(R.drawable.ic_launcher);  
		        builder.setTitle("Action");  
		        builder.setMessage("Select the following three options");  
		        //video option
		        builder.setPositiveButton("Video",  
		                new DialogInterface.OnClickListener() {  
		                    public void onClick(DialogInterface dialog, int whichButton) {  
		                    	//send video_call msg
		                    	sendMessage("video_call",((Entity)adapter.getOBJ(index)).getname(),false);
		                    }  
		                }); 
		        //audio option
		        builder.setNeutralButton("Audio",  
		                new DialogInterface.OnClickListener() {  
		                    public void onClick(DialogInterface dialog, int whichButton) {  
		                    	//send audio_call msg
		                    	sendMessage("audio_call",((Entity)adapter.getOBJ(index)).getname(),false);
		                    }  
		                }); 
		        //chat option
		        builder.setNegativeButton("Chat",  
		                new DialogInterface.OnClickListener() {  
		                    public void onClick(DialogInterface dialog, int whichButton) {  
		                    	 final EditText edtInput=new EditText(mactivity); 
		                        final AlertDialog.Builder builder_msg = new AlertDialog.Builder(mactivity);  
		                        builder_msg.setCancelable(false);  
		                        builder_msg.setIcon(R.drawable.ic_launcher);  
		                        builder_msg.setTitle("Title");  
		                        builder_msg.setView(edtInput);  
		                        builder_msg.setPositiveButton("Send",  
		                                new DialogInterface.OnClickListener() {  
		                                    public void onClick(DialogInterface dialog, int whichButton) {  
		                                    	// send msg
		                                    	sendMessage(edtInput.getText().toString().trim(),((Entity)adapter.getOBJ(index)).getname(),true);
		                                    }  
		                                });  
		                        builder_msg.setNegativeButton("cancel",  
		                                new DialogInterface.OnClickListener() {  
		                                    public void onClick(DialogInterface dialog, int whichButton) {  
		                                    	//no action
		                                    }  
		                                });  
		                        builder_msg.show();

		                    }  
		                });  
		        builder.show();  
			}
		});
	}
	//更新好友列表
	private void updateListview() {
		//获取好友信息 asmack接口操作(非封装接口)
		Roster roster = global.g_xmppEngine.mConnection.getRoster();

		roster.addRosterListener(this);   

		List<RosterEntry> Entrieslist = new ArrayList<RosterEntry>();
		Collection<RosterEntry> rosterEntry = roster.getEntries();
		Iterator<RosterEntry> i = rosterEntry.iterator();

		list.clear();
		int n = 0;
		while (i.hasNext()) {
			Entrieslist.add(i.next());
			String user= Entrieslist.get(n).getUser();
			String name=Entrieslist.get(n).getName();
			Log.e("hj", "friend userid=" + Entrieslist.get(n).getUser()+"  name="+Entrieslist.get(n).getName());
			Presence presence=roster.getPresence(user);
			if(presence.isAvailable()){
				Entity entity=new Entity();
				entity.setname(name);
				list.add(entity);
			}
			n++;
		}
		adapter.setList(list);
		Log.e("hj", "friend count:" + adapter.getSize());

	}
	

	@Override
	public void entriesAdded(Collection<String> arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void entriesDeleted(Collection<String> arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void entriesUpdated(Collection<String> arg0) {
		// TODO Auto-generated method stub
		
	}

	//好友状态回调
	@Override
	public void presenceChanged(Presence arg0) {
		// TODO Auto-generated method stub
       	String fromUser=arg0.getFrom();
    	int index=fromUser.indexOf('@');
    	if(index>0)
    		fromUser=fromUser.substring(0,index);
		//以上操作好友是否在线或者下线，显示在列表中
        if(arg0.isAvailable())
        {
        	boolean isExit = false;
        	for(int i=0;i<adapter.getSize();i++){
        		if(fromUser.compareTo(((Entity)adapter.getOBJ(i)).getname())==0)
        			isExit = true;
        	}
        	if(!isExit){
				Entity entity=new Entity();
				entity.setname(fromUser);
				adapter.addObject(entity);
        	}
        }else
        {
        	for(int i=0;i<adapter.getSize();i++)
        		if(fromUser.compareTo(((Entity)adapter.getOBJ(i)).getname())==0)
        			adapter.removeObject(i);
        	
        }
	}

	private void logAndToast(String msg) {
		Log.d("Friends", msg);
		if (logToast != null) {
			logToast.cancel();
		}
		logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		logToast.show();
	}

	//消息接收
	private IXmppSMSListener mXmppSMSListener = new IXmppSMSListener(){
	public boolean OnNewMessage(xmppMessage msg) {
		// TODO Auto-generated method stub
		String ms = (msg.getAccount() + "(" + msg.getiType() + "): " + msg
				.getData());

		if (msg.getiType() == xmppMessage.SMS_TYPE_AUDIO) {


		} else if (msg.getiType() == xmppMessage.SMS_TYPE_PICTURE) {

		} else if (msg.getiType() == xmppMessage.SMS_TYPE_VIDEOCALL) {
			//video_call audio_call表示被叫方接收消息
			//video_back audio_back表示主叫方接收到被叫方消息
			//被叫方接到消息就马上跳转到通话界面，主叫方要等到被叫方消息回复后才跳转
			//如果是文字就是消息类型是SMS_TYPE_MESSAGE，视频和音频现在暂时用的都是SMS_TYPE_VIDEOCALL消息类型
			if(ms.indexOf("video_call")>=0)//call
			{
				sendMessage("video_back",msg.getAccount(),false);
				Intent i = new Intent(Friends.this, AppRTCDemoActivity.class);
				i.putExtra("Type","video");
				i.putExtra("url",url);
				i.putExtra("user",user);
				i.putExtra("credential",credential);
				startActivity(i);
				global.g_xmppEngine.setSMSListener(null);
			}else if(ms.indexOf("video_back")>=0){
				Intent i = new Intent(Friends.this, AppRTCDemoActivity.class);
				i.putExtra("OFFERMSG","video_offer");
				i.putExtra("Type","video");
				i.putExtra("url",url);
				i.putExtra("user",user);
				i.putExtra("credential",credential);
				global.g_offer_msg=msg;
				startActivity(i);
				global.g_xmppEngine.setSMSListener(null);
			}
			
			if(ms.indexOf("audio_call")>=0)//call
			{
				sendMessage("audio_back",msg.getAccount(),false);
				Intent i = new Intent(Friends.this, AppRTCDemoActivity.class);
				i.putExtra("Type","audio");
				i.putExtra("url",url);
				i.putExtra("user",user);
				i.putExtra("credential",credential);
				startActivity(i);
				global.g_xmppEngine.setSMSListener(null);
			}else if(ms.indexOf("audio_back")>=0){
				Intent i = new Intent(Friends.this, AppRTCDemoActivity.class);
				i.putExtra("Type","audio");
				i.putExtra("OFFERMSG","audio_offer");
				i.putExtra("url",url);
				i.putExtra("user",user);
				i.putExtra("credential",credential);
				global.g_offer_msg=msg;
				startActivity(i);
				global.g_xmppEngine.setSMSListener(null);
			}

		}else if(msg.getiType() == xmppMessage.SMS_TYPE_MESSAGE) {// text msg
				logAndToast("User="+msg.getAccount()+"NewMessageRecive="+ms);
		}
			
		
		return true;
		}
	};
	//发送消息，注意设置消息类型 isChat表示是否是纯文字信息
	private void sendMessage(String msg, String toAcount,Boolean isChat) {
//		Log.e("hj", json.toString()+" touser:"+toAcount);
    	xmppMessage message = new xmppMessage();
    	message.setAccount(toAcount);
    	message.setData(msg);
    	if(!isChat){
        	message.setiType(xmppMessage.SMS_TYPE_VIDEOCALL);
    	}else{
        	message.setiType(xmppMessage.SMS_TYPE_MESSAGE);
    	}

    	int iRet = global.g_xmppEngine.sendSecretMessage(message);    	
    	
    	if (iRet != xmppEngine.ERROR_NONE){
    		Toast.makeText(this, "send failed due to error code: " + iRet, Toast.LENGTH_LONG).show();
    	}else{
    	}

	}
    private void uninitChat(){
    	if (global.g_xmppEngine != null){
			global.g_xmppEngine.logOut(); 
			global.g_xmppEngine = null;
    	}
    }
    //从通话界面返回后activity执行函数，执行设置当前消息界面上操作的消息监听
	@Override
	protected void onResume() {
		global.g_xmppEngine.setSMSListener(mXmppSMSListener);
		super.onResume();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK){
			uninitChat();
			System.exit(0);
		}
		return super.onKeyDown(keyCode, event);
	}
}
