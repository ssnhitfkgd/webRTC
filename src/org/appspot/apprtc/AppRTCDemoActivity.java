/*
 * libjingle
 * Copyright 2013, Google Inc.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.appspot.apprtc;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.StatsObserver;
import org.webrtc.StatsReport;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRenderer.I420Frame;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.videoengine.ViERenderer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.xhmm.xmpp.IXmppListener.IXmppSMSListener;
import com.xhmm.xmpp.xmppEngine;
import com.xhmm.xmpp.xmppMessage;

/**
 * Main Activity of the AppRTCDemo Android app demonstrating interoperability
 * between the Android/Java implementation of PeerConnection and the
 * apprtc.appspot.com demo webapp.
 */
public class AppRTCDemoActivity extends Activity implements
		AppRTCClient.IceServersObserver  {
	private static final String TAG = "AppRTCDemoActivity";
	private PeerConnection pc;
	private final PCObserver pcObserver = new PCObserver();
	private final SDPObserver sdpObserver = new SDPObserver();
	private final GAEChannelClient.MessageHandler gaeHandler = new GAEHandler();
	private AppRTCClient appRtcClient = new AppRTCClient(this, gaeHandler, this);
	private VideoStreamsView vsv;
	private Toast logToast;
	// private List<HashMap<String, String>>
	private LinkedList<IceCandidate> queuedRemoteCandidates = new LinkedList<IceCandidate>();
	// Synchronize on quit[0] to avoid teardown-related crashes.
	private final Boolean[] quit = new Boolean[] { false };
	private MediaConstraints sdpMediaConstraints;
	private PowerManager.WakeLock wakeLock;
	private String remoteName;

	private boolean mIsInited;
	private boolean mIsCalled;
	PeerConnectionFactory factory;
	VideoCapturer capturer;
	
	private int mType = 0; // 0:video 1:audio
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//设置当前消息监听
		global.g_xmppEngine.setSMSListener(mXmppSMSListener);
		// Since the error-handling of this demo consists of throwing
		// RuntimeExceptions and we assume that'll terminate the app, we install
		// this default handler so it's applied to background threads as well.
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, Throwable e) {
				e.printStackTrace();
				System.exit(-1);
			}
		});

		PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(
				PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "AppRTCDemo");
		wakeLock.acquire();

		 setContentView(R.layout.send);
		 //远程的surfaceview
		 vsv=(VideoStreamsView) findViewById(R.id.viewinfo);
		 //本地的surfaceview
	    SurfaceView svLocal = ViERenderer.CreateLocalRenderer(this);
        LinearLayout mLlLocalSurface = (LinearLayout) findViewById(R.id.llLocalView);

		Intent i=getIntent();
		String type =i.getStringExtra("Type");
		//视频call就设置界面显示，否者就设置背景为audiocall布局
		if(type.indexOf("video")>=0){
	        mLlLocalSurface.addView(svLocal);
		}else{
			mType = 1;     
			setContentView(R.layout.audiocall);
		}
        
		abortUnless(PeerConnectionFactory.initializeAndroidGlobals(this),
				"Failed to initializeAndroidGlobals");

		//这里是听筒或者扬声器设置的地方，根据需求自己固定或者动态设置，(此处固定某种模式后切换其他模式可能导致无法调节音量)，需要动态操作
		AudioManager audioManager = ((AudioManager) getSystemService(AUDIO_SERVICE));
		audioManager
				.setMode(audioManager.isWiredHeadsetOn() ? AudioManager.MODE_IN_CALL
						: AudioManager.MODE_IN_CALL);
		audioManager.setSpeakerphoneOn(!audioManager.isWiredHeadsetOn());

		//Media条件信息SDP接口
		sdpMediaConstraints = new MediaConstraints();
		//接受远程音频
		sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
				"OfferToReceiveAudio", "true"));
		//音频call则不接受远程视频流
		if(mType == 0){
			sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
					"OfferToReceiveVideo", "true"));
		}else{
			sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
					"OfferToReceiveVideo", "false"));
		}
		
		String url  = i.getStringExtra("url");
		String user  = i.getStringExtra("user");
		String credential  = i.getStringExtra("credential");
		
		//iceServer List对象获取
		List<PeerConnection.IceServer> iceServers=appRtcClient.initwebrtc(url,user,credential);
		 factory = new PeerConnectionFactory();

		 //创建peerconnection接口，用于发送offer 或者answer
		pc = factory.createPeerConnection(iceServers,
				appRtcClient.pcConstraints(), pcObserver);
		

		mIsInited = false;
		mIsCalled=false;
		
		String offer=i.getStringExtra("OFFERMSG");
		//offer如果为空表示为主叫方初始化，否则是被叫方初始化
		if(offer == null)
		{
			initialSystem();
		}
		else
			callRemote(global.g_offer_msg.getAccount());
	}

	//返回键操作，断开连接同时给对方发送断开消息
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			// DO SOMETHING
			sendMessage(null,remoteName);
			disconnectAndExit();
		}
		return super.onKeyDown(keyCode, event);
	}


	@Override
	public void onPause() {
		super.onPause();
		vsv.onPause();
		// TODO(fischman): IWBN to support pause/resume, but the WebRTC codebase
		// isn't ready for that yet; e.g.
		// https://code.google.com/p/webrtc/issues/detail?id=1407
		// Instead, simply exit instead of pausing (the alternative leads to
		// system-borking with wedged cameras; e.g. b/8224551)
		// disconnectAndExit();
	}

	@Override
	public void onResume() {
		// The onResume() is a lie! See TODO(fischman) in onPause() above.
		super.onResume();
		vsv.onResume();
	}

	@Override
	public void onIceServers(List<PeerConnection.IceServer> iceServers) {

	//	Log.e("activity", "peer con created success!");

	}

	//初始化
	private void initialSystem() {
		if (mIsInited)
			return;
	
		Log.e("hj", "init in");
		{
			final PeerConnection finalPC = pc;
			final Runnable repeatedStatsLogger = new Runnable() {
				public void run() {
					
					synchronized (quit[0]) {
						if (quit[0]) {
							return;
						}
						final Runnable runnableThis = this;
						boolean success = finalPC.getStats(new StatsObserver() {
							public void onComplete(StatsReport[] reports) {
								for (StatsReport report : reports) {
									Log.d(TAG, "Stats: " + report.toString());
								}
								vsv.postDelayed(runnableThis, 10000);
							}
						}, null);
						if (!success) {
							throw new RuntimeException(
									"getStats() return false!");
						}
					}
				}
			};
			vsv.postDelayed(repeatedStatsLogger, 10000);
		}
		
		{
			// logAndToast("Creating local video source...");
			//本地摄像头接口
			capturer = getVideoCapturer();
			Log.e("hj","init:"+capturer.toString());
			VideoSource videoSource = factory.createVideoSource(capturer,
					appRtcClient.videoConstraints());
			Log.e("hj","init:"+videoSource.toString());
			
			MediaStream lMS = factory.createLocalMediaStream("ARDAMS");
			VideoTrack videoTrack = factory.createVideoTrack("ARDAMSv0",
					videoSource);
			videoTrack.addRenderer(new VideoRenderer(new VideoCallbacks(vsv,
					VideoStreamsView.Endpoint.LOCAL)));
			//音频call则禁止远程视频
			if(mType == 1){
			 videoTrack.setEnabled(false);
			}
			lMS.addTrack(videoTrack);
			lMS.addTrack(factory.createAudioTrack("ARDAMSa0"));

			MediaConstraints stmc = new MediaConstraints();

			pc.addStream(lMS, stmc);

		}
		mIsInited = true;
	}

	//被叫方初始化
	public void callRemote(String toUser) {
		Log.e("hj", "call remote:" + toUser);
		remoteName = toUser;
		initialSystem();
		gaeHandler.onOpen();
	}

	// Cycle through likely device names for the camera and return the first
	// capturer that works, or crash if none do.
	private VideoCapturer getVideoCapturer() {
		String[] cameraFacing = { "front", "back" };
		int[] cameraIndex = { 0, 1 };
		int[] cameraOrientation = { 0, 90, 180, 270 };
		for (String facing : cameraFacing) {
			for (int index : cameraIndex) {
				for (int orientation : cameraOrientation) {
					String name = "Camera " + index + ", Facing " + facing
							+ ", Orientation " + orientation;
					VideoCapturer capturer = VideoCapturer.create(name);
					if (capturer != null) {
//						logAndToast("Using camera: " + name);
						return capturer;
					}
				}
			}
		}
		throw new RuntimeException("Failed to open capturer");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	// Poor-man's assert(): die with |msg| unless |condition| is true.
	private static void abortUnless(boolean condition, String msg) {
		if (!condition) {
			throw new RuntimeException(msg);
		}
	}

	// Log |msg| and Toast about it.
	private void logAndToast(String msg) {
		Log.d(TAG, msg);
		if (logToast != null) {
			logToast.cancel();
		}
		logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		logToast.show();
	}

	// Send |json| to the underlying AppEngine Channel.
	//发送消息
	private void sendMessage(JSONObject json, String toAcount) {
//		logAndToast("发送"+toAcount+"=="+json.toString());
    	xmppMessage message = new xmppMessage();
    	message.setAccount(toAcount);
		//json为空表示发送的退出消息
		if(json == null){
	    	message.setData("stop_call");
		}else
	    	message.setData(json.toString());

    	message.setiType(xmppMessage.SMS_TYPE_VIDEOCALL);
    	int iRet = global.g_xmppEngine.sendSecretMessage(message);
    	
    	if (iRet != xmppEngine.ERROR_NONE){
    		Toast.makeText(this, "send failed due to error code: " + iRet, Toast.LENGTH_LONG).show();
    	}else{
        	//showMsg("I: " + msg, Color.RED);	
    	}

	}

	// Put a |key|->|value| mapping in |json|.
	private static void jsonPut(JSONObject json, String key, Object value) {
		try {
			json.put(key, value);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	// Implementation detail: observe ICE & stream changes and react
	// accordingly.
	private class PCObserver implements PeerConnection.Observer {
		// ICE回调接口，给对方发送candidate sdpMLineIndex sdpMid三个类型数据
		@Override
		public void onIceCandidate(final IceCandidate candidate) {
			runOnUiThread(new Runnable() {

				public void run() {

					JSONObject json = new JSONObject();

					jsonPut(json, "candidate", candidate.sdp);
					jsonPut(json, "sdpMLineIndex", candidate.sdpMLineIndex);
					jsonPut(json, "sdpMid", candidate.sdpMid);
					
					Log.e("activity", "PCObserver onIceCandidate:" );
					sendMessage(json, remoteName);
				}
			});
		}

		@Override
		public void onError() {
			runOnUiThread(new Runnable() {
				public void run() {
					throw new RuntimeException("PeerConnection error!");
				}
			});
		}

		@Override
		public void onSignalingChange(PeerConnection.SignalingState newState) {
			Log.e("hj", "PCObserver onSignalingChange" + newState.toString());
		}

		@Override
		public void onIceConnectionChange(
				PeerConnection.IceConnectionState newState) {
			Log.e("hj",
					"PCObserver onIceConnectionChange" + newState.toString());
		}

		@Override
		public void onIceGatheringChange(
				PeerConnection.IceGatheringState newState) {
			Log.e("hj", "PCObserver onIceGatheringChange" + newState.toString());
		}

		//添加流的回调，用于渲染和画数据的操作
		@Override
		public void onAddStream(final MediaStream stream) {
			runOnUiThread(new Runnable() {
				public void run() {
					Log.e("demoActivity", "PCObserver onAddStream");
					// abortUnless(stream.audioTracks.size() == 1 &&
					// stream.videoTracks.size() == 1,
					// "Weird-looking stream: " + stream);
					//Log.e("demoActivity", "PCObserver onAddStream addRenderer");
					stream.videoTracks.get(0).addRenderer(
							new VideoRenderer(new VideoCallbacks(vsv,
									VideoStreamsView.Endpoint.REMOTE)));

				}
			});
		}

		@Override
		public void onRemoveStream(final MediaStream stream) {
			runOnUiThread(new Runnable() {
				public void run() {
					stream.videoTracks.get(0).dispose();
				}
			});
		}

		@Override
		public void onDataChannel(final DataChannel dc) {
			runOnUiThread(new Runnable() {
				public void run() {
					Log.e("hj", "onDataChannel");
					throw new RuntimeException(
							"AppRTC doesn't use data channels, but got: "
									+ dc.label() + " anyway!");
				}
			});
		}
	}

	// Implementation detail: handle offer creation/signaling and answer
	// setting,
	// as well as adding remote ICE candidates once the answer SDP is set.
	private class SDPObserver implements SdpObserver {
	//sdp监听者回调，获取sdp信息(音视频相关编码选项等参数信息，ip地址信息等)发送给对方，
		@Override
		public void onCreateSuccess(final SessionDescription sdp) {
			runOnUiThread(new Runnable() {
				public void run() {
					Log.e("demoActivity", "SDPObserver local sdp onCreateSuccess:"
							+ sdp.type);
					// Log.e("demoActivity","SDPObserver onCreateSuccess:"+sdp.description);

//					logAndToast("Sending " + sdp.type);
					JSONObject json = new JSONObject();
					jsonPut(json, "type", sdp.type.canonicalForm());
					jsonPut(json, "sdp", sdp.description);
					sendMessage(json, remoteName);
					pc.setLocalDescription(sdpObserver, sdp);
					Log.e("demoActivity", "SDPObserver setLocalDescription:");
				}
			});
		}

		@Override
		public void onSetSuccess() {
			runOnUiThread(new Runnable() {
				public void run() {
					Log.e("demoActivity", "SDPObserver onSetSuccess");
				//	if (appRtcClient.isInitiator()) {
					if(!mIsCalled){
						if (pc.getRemoteDescription() != null) {
							// We've set our local offer and received & set the
							// remote
							// answer, so drain candidates.
							//Log.e("demoActivity", "SDPObserver drain candidate");
							drainRemoteCandidates();
						}
					} else {
						if (pc.getLocalDescription() == null) {
							// We just set the remote offer, time to create our
							// answer.
//							logAndToast("Creating answer");
							Log.e("demoActivity", "SDPObserver create answer");
						} else {
							// Sent our answer and set it as local description;
							// drain
							// candidates.
							drainRemoteCandidates();
						}
					}
				}
			});
		}

		@Override
		public void onCreateFailure(final String error) {
			runOnUiThread(new Runnable() {
				public void run() {
					throw new RuntimeException("createSDP error: " + error);
				}
			});
		}

		@Override
		public void onSetFailure(final String error) {
			runOnUiThread(new Runnable() {
				public void run() {
					throw new RuntimeException("setSDP error: " + error);
				}
			});
		}

		private void drainRemoteCandidates() {
			Log.e("demoActivity", " drainRemoteCandidates");
			if (queuedRemoteCandidates == null)
				return;
			// for (IceCandidate candidate : queuedRemoteCandidates) {
			// pc.addIceCandidate(candidate);
			// }
			for (int i = 0; i < queuedRemoteCandidates.size(); i++)
				pc.addIceCandidate(queuedRemoteCandidates.get(i));
			queuedRemoteCandidates = null;
		}
	}

	// Implementation detail: handler for receiving GAE messages and dispatching
	// them appropriately.
	private class GAEHandler implements GAEChannelClient.MessageHandler {

	// 创建一个远程的offer操作
		public void onOpen() {
			if (!appRtcClient.isInitiator()) {
				return;
			}
//			logAndToast("Creating offer...");
			// Create a new offer.
			// The CreateSessionDescriptionObserver callback will be called when
			// done.
			pc.createOffer(sdpObserver, sdpMediaConstraints);

		}
		//接收到对方的信息后进行处理，不管是offer还是answer的SDP等信息
		public int onMessage(String msg,String fromuser) {
			try {
				 Log.e("activity", "GAEHandler onMessage:"+msg);

				JSONObject json = new JSONObject(msg);
				String type = null;
				try {
					type = (String) json.get("type");
				} catch (JSONException e) {
					// throw new RuntimeException(e);
					type = null;
				}
				if (type == null) {
				//	Log.e("hj", "candidate");
					IceCandidate candidate = new IceCandidate(
							(String) json.get("sdpMid"),
							json.getInt("sdpMLineIndex"),
							(String) json.get("candidate"));

					if (queuedRemoteCandidates != null) {
						queuedRemoteCandidates.add(candidate);
					} else {
						pc.addIceCandidate(candidate);

					}
				} else if (type.equals("answer") || type.equals("offer")) {
					if (type.equals("offer")) {
						mIsCalled=true;
						remoteName=fromuser;
						}
					SessionDescription sdp = new SessionDescription(
							SessionDescription.Type.fromCanonicalForm(type),
							(String) json.get("sdp"));
					Log.e("hj", "get answer or offer setRemoteDescription");
					if(pc == null){
						logAndToast("pc ====null");
					}
					pc.setRemoteDescription(sdpObserver, sdp);
					//创建一个远程的answser操作
					if (type.equals("offer")) {

						 pc.createAnswer(sdpObserver, sdpMediaConstraints);
					}
				} else if (type.equals("bye")) {
					logAndToast("Remote end hung up; dropping PeerConnection");
					disconnectAndExit();
				} else {
					return 1;
					// throw new RuntimeException("Unexpected message: " +
					// data);
				}
				return 0;
			} catch (JSONException e) {
				throw new RuntimeException(e);
				// // return 0;
			}

		}

		//@JavascriptInterface
		public void onClose() {
			disconnectAndExit();
		}

		//@JavascriptInterface
		public void onError(int code, String description) {
			disconnectAndExit();
		}
	}

	// Disconnect from remote resources, dispose of local resources, and exit.
	private void disconnectAndExit() {
		synchronized (quit[0]) {
			if (quit[0]) {
				return;
			}
			quit[0] = true;
			wakeLock.release();

			if (pc != null) {
				pc.dispose();
				pc = null;
			}

			if (appRtcClient != null) {
				appRtcClient.disconnect();
				appRtcClient = null;
			}
            capturer.dispose();
			global.g_xmppEngine.setSMSListener(null);

		}
	}

	// Implementation detail: bridge the VideoRenderer.Callbacks interface to
	// the
	// VideoStreamsView implementation.
	//视频渲染的回调接口
	private class VideoCallbacks implements VideoRenderer.Callbacks {
		private final VideoStreamsView view;
		private final VideoStreamsView.Endpoint stream;

		public VideoCallbacks(VideoStreamsView view,
				VideoStreamsView.Endpoint stream) {
			this.view = view;
			this.stream = stream;
		}

		@Override
		public void setSize(final int width, final int height) {
			view.queueEvent(new Runnable() {
				public void run() {
					view.setSize(stream, width, height);
				}
			});
		}

		@Override
		public void renderFrame(I420Frame frame) {
			view.queueFrame(stream, frame);
		}
	}
	//消息接收，基本用于断开，和offer answer创建时对方SDP信息接收，通过gaeHandler.onMessage进行分类操作
	private IXmppSMSListener mXmppSMSListener = new IXmppSMSListener(){
	public boolean OnNewMessage(xmppMessage msg) {
		// TODO Auto-generated method stub
//		String ms = (msg.getAccount() + "(" + msg.getiType() + "): " + msg
//				.getData());
		if(msg.getData().indexOf("stop_call")>= 0){
			disconnectAndExit();
			finish();
		}
		if(msg.getiType()!=xmppMessage.SMS_TYPE_VIDEOCALL)
			return false;
		
		String str=msg.getData();
		if(str.indexOf("{")>=0&&str.indexOf("}")>0)
			gaeHandler.onMessage(str.substring(str.indexOf("{"), str.indexOf("}")+1),msg.getAccount());
		
		return false;
	}
	};
}
