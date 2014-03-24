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


/**
 * Java-land version of Google AppEngine's JavaScript Channel API:
 * https://developers.google.com/appengine/docs/python/channel/javascript
 *
 * Requires a hosted HTML page that opens the desired channel and dispatches JS
 * on{Open,Message,Close,Error}() events to a global object named
 * "androidMessageHandler".
 */
//这个接口本来是操作web页面或者js的，现在只是用了interface MessageHandler接口用来操作webrtc通信过程作用
public class GAEChannelClient {
  private static final String TAG = "GAEChannelClient";

  /**
   * Callback interface for messages delivered on the Google AppEngine channel.
   *
   * Methods are guaranteed to be invoked on the UI thread of |activity| passed
   * to GAEChannelClient's constructor.
   */
  public interface MessageHandler {
    public void onOpen();
    public int onMessage(String data,String fromuser);
    public void onClose();
    public void onError(int code, String description);
  }

}
