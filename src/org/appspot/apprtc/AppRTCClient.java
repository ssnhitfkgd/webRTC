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

import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;

import android.app.Activity;

/**
 * Negotiates signaling for chatting with apprtc.appspot.com "rooms". Uses the
 * client<->server specifics of the apprtc AppEngine webapp.
 * 
 * To use: create an instance of this object (registering a message handler) and
 * call connectToRoom(). Once that's done call sendMessage() and wait for the
 * registered handler to be called with received messages.
 */
public class AppRTCClient {
	private static final String TAG = "AppRTCClient";
	private GAEChannelClient channelClient;

	public AppRTCSignalingParameters appRTCSignalingParameters;

	/**
	 * Callback fired once the room's signaling parameters specify the set of
	 * ICE servers to use.
	 */

	public static interface IceServersObserver {
		public void onIceServers(List<PeerConnection.IceServer> iceServers);
	}

	public AppRTCClient(Activity activity,
			GAEChannelClient.MessageHandler gaeHandler,
			IceServersObserver iceServersObserver) {
	}

	//初始化ice server 和 AppRTCSignalingParameters 用于createPeerConnection参数
	public List<PeerConnection.IceServer>  initwebrtc(String url,String user,String credential) {
		// set server parameter

//		Log.e("hj", "ConnectLogin socket close,and start calll");
		PeerConnection.IceServer turn = new PeerConnection.IceServer(
				url,user,credential);
		LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<PeerConnection.IceServer>();
		iceServers.add(turn);
		MediaConstraints constraints = new MediaConstraints();
		MediaConstraints vconstraints = new MediaConstraints();
		constraints.optional.add(new MediaConstraints.KeyValuePair(
				"DtlsSrtpKeyAgreement", "true"));

		constraints.mandatory.add(new
		 MediaConstraints.KeyValuePair("VoiceActivityDetection", "false"));
		 
		vconstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", "240"));
		vconstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", "320"));
		

		appRTCSignalingParameters = new AppRTCSignalingParameters(iceServers,
				null, null, null, true, constraints, vconstraints);

		appRTCSignalingParameters.iceServers.add(turn);

		return appRTCSignalingParameters.iceServers;
	}

	/**
	 * Disconnect from the GAE Channel.
	 */
	public void disconnect() {
		if (channelClient != null) {
			channelClient = null;
		}
	}

	/**
	 * Queue a message for sending to the room's channel and send it if already
	 * connected (other wise queued messages are drained when the channel is
	 * eventually established).
	 */

	public boolean isInitiator() {
		return appRTCSignalingParameters.initiator;
	}

	public MediaConstraints pcConstraints() {
		return appRTCSignalingParameters.pcConstraints;
	}

	public MediaConstraints videoConstraints() {
		return appRTCSignalingParameters.videoConstraints;
	}

	// Struct holding the signaling parameters of an AppRTC room.
	class AppRTCSignalingParameters {
		public final List<PeerConnection.IceServer> iceServers;
		public final String gaeBaseHref;
		public final String channelToken;
		public final String postMessageUrl;
		public final boolean initiator;
		public final MediaConstraints pcConstraints;
		public final MediaConstraints videoConstraints;

		public AppRTCSignalingParameters(
				List<PeerConnection.IceServer> iceServers, String gaeBaseHref,
				String channelToken, String postMessageUrl, boolean initiator,
				MediaConstraints pcConstraints,
				MediaConstraints videoConstraints) {
			this.iceServers = iceServers;
			this.gaeBaseHref = gaeBaseHref;
			this.channelToken = channelToken;
			this.postMessageUrl = postMessageUrl;
			this.initiator = initiator;
			this.pcConstraints = pcConstraints;
			this.videoConstraints = videoConstraints;
		}
	}
}
