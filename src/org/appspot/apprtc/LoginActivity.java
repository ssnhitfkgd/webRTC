package org.appspot.apprtc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.xhmm.ftpupload.ftpUploadEngine;
import com.xhmm.xmpp.IXmppListener.IXmppLoginListener;
import com.xhmm.xmpp.xmppAccount;
import com.xhmm.xmpp.xmppEngine;

public class LoginActivity extends Activity {
	private EditText etRegister = null;
	private EditText etLogin	= null;
	private EditText etPsw	= null;
	private EditText etServer	= null;
	private EditText etServer_host	= null;
	
	private EditText etUrl	= null;
	private EditText etUser	= null;
	private EditText etCredential	= null;
	//openfire登录信息
	public static final String SAVE_NAME 		= "login";
	public static final String SAVE_TAG_ACCOUNT = "account";
	public static final String SAVE_TAG_PSW 	= "password";
	public static final String SAVE_TAG_SERVERIP 	= "serverip";
	public static final String SAVE_TAG_SERVERIP_HOST 	= "serverip_host";
	//turn 参数
	public static final String SAVE_TAG_URL 	= "url";
	public static final String SAVE_TAG_USER 	= "user";
	public static final String SAVE_TAG_CREDENTIAL 	= "credential";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        initView();
        
        initSaveData();
    }

    private void initView(){
    	etRegister =null;	//= (EditText) findViewById(R.id.register_account);
    	etLogin 	= (EditText) findViewById(R.id.login_account);
    	etPsw		=(EditText) findViewById(R.id.login_psw);
    	
    	etServer	= (EditText) findViewById(R.id.login_server);
    	etServer_host	= (EditText) findViewById(R.id.login_server_host);
    	
    	etUrl	= (EditText) findViewById(R.id.url);
    	etUser	= (EditText) findViewById(R.id.user);
    	etCredential	= (EditText) findViewById(R.id.credential);
    }
    //初始化XMPPEngine,连接到服务器
    private void initChat(String serverIp,String serverIp_host){
    	if (global.g_xmppEngine == null){
        	global.g_xmppEngine 	 = new xmppEngine(serverIp, 5222, serverIp_host);	
    	}
    }
    //登出
    private void uninitChat(){
    	if (global.g_xmppEngine != null){
			global.g_xmppEngine.logOut(); 
			global.g_xmppEngine = null;
    	}
    }
    //初始化数据
    private void initSaveData(){
    	loadAccount();
    }
	//sharepreference方式保存数据
    private void saveAccount(String account,String psw,String ip,String ip_host,String mUrl,String mUser,String mCredential){
		SharedPreferences.Editor editor = getSharedPreferences(SAVE_NAME, Context.MODE_PRIVATE).edit();
		editor.putString(SAVE_TAG_ACCOUNT, account);
		editor.putString(SAVE_TAG_PSW, psw);
		editor.putString(SAVE_TAG_SERVERIP, ip);
		editor.putString(SAVE_TAG_SERVERIP_HOST, ip_host);
		
		editor.putString(SAVE_TAG_URL, mUrl);
		editor.putString(SAVE_TAG_USER, mUser);
		editor.putString(SAVE_TAG_CREDENTIAL, mCredential);
		editor.commit();
    }
    //sharepreference方式获取数据并显示在编辑框中
    private void loadAccount(){
    	SharedPreferences loginPreference = getSharedPreferences(SAVE_NAME, Context.MODE_PRIVATE);
    	String account = loginPreference.getString(SAVE_TAG_ACCOUNT, "1");
    	String psw = loginPreference.getString(SAVE_TAG_PSW, "123456");
    	String ip = loginPreference.getString(SAVE_TAG_SERVERIP, "172.16.3.107");
    	String ip_host = loginPreference.getString(SAVE_TAG_SERVERIP_HOST, "xhmm.com");
    	
    	String url = loginPreference.getString(SAVE_TAG_URL, "turn:112.124.57.58");
    	String user = loginPreference.getString(SAVE_TAG_USER, "turn");
    	String credential = loginPreference.getString(SAVE_TAG_CREDENTIAL, "turn");
    	
    	etLogin.setText(account);
    	etPsw.setText(psw);
    	etServer.setText(ip);
    	etServer_host.setText(ip_host);
    	
    	etUrl.setText(url);
    	etUser.setText(user);
    	etCredential.setText(credential);
    }
    
	//注册
//    public void OnRegister(View v){
//    	initChat(etServer.getText().toString().trim());
//    	
//    	xmppAccount account = new xmppAccount();
//    	account.setAccount(etRegister.getText().toString().trim());
//    	account.setPassword("123456");
//    	global.g_xmppEngine.setRegisterListener(new IXmppRegisterListener() {
//			public boolean OnRegisterResponse(xmppAccount account, int result) {
//				if (result == 0){
//					saveAccount(account.getAccount());
//					etLogin.setText(account.getAccount());
//					
//				}else{
//					
//				}
//				
//				global.g_xmppEngine.setRegisterListener(null);
//				return false;
//			}
//		});
//		global.g_xmppEngine.setSMSListener(new IXmppSMSListener() {
//			public boolean OnNewMessage(xmppMessage msg) {
//				Toast.makeText(LoginActivity.this, 
//						msg.getAccount() + "said: " + msg.getData(), Toast.LENGTH_LONG).show();
//				return false;
//			}
//		});
//    	
//    	global.g_xmppEngine.Register(account);
//    }
    
	//登录
    public void OnLogin(View v){
    	initChat(etServer.getText().toString().trim(),etServer_host.getText().toString().trim());
    	
    	xmppAccount account = new xmppAccount();
    	account.setAccount(etLogin.getText().toString().trim());
    	account.setPassword(etPsw.getText().toString().trim());
    	global.g_xmppEngine.setLoginListener(new IXmppLoginListener() {
		//登录信息回调
			public boolean OnLoginResponse(xmppAccount account, int result) {
				if (result == 0){
					saveAccount(account.getAccount(),account.getPassword(),etServer.getText().toString().trim(),etServer_host.getText().toString().trim(),etUrl.getText().toString().trim(),etUser.getText().toString().trim(),etCredential.getText().toString().trim());
					//登录成功跳转好友列表
					Intent i = new Intent(LoginActivity.this, Friends.class);
					i.putExtra("url",etUrl.getText().toString().trim());
					i.putExtra("user",etUser.getText().toString().trim());
					i.putExtra("credential",etCredential.getText().toString().trim());
					startActivity(i);
					finish();
				}else{

				}
				global.g_xmppEngine.setLoginListener(null);
				return false;
			}
		});
    	global.g_xmppEngine.Login(account);
    }

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK){
			uninitChat();
			System.exit(0);
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onDestroy() {
		if (global.g_xmppEngine != null){
			global.g_xmppEngine.setLoginListener(null);
			global.g_xmppEngine.setRegisterListener(null);
		}
		super.onDestroy();
	}
}
