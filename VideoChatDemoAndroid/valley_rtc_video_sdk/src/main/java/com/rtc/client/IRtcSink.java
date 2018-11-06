package com.rtc.client;


public interface IRtcSink {
	public abstract void Respond(int type, int ec, Object ob, long userdata);
	public abstract void Notify(int type, Object ob, long userdata);
}

 