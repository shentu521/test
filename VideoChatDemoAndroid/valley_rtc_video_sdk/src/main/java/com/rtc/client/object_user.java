package com.rtc.client;

public class object_user {
	String   userid;
	String   userinfo;
	boolean bDisable;
	boolean bBlocked;
	 
	public String getUserID(){return userid;}
	public String getUserInfo(){return userinfo;}
	public boolean getDisableSpeak(){return bDisable;}
	public boolean getBlocked(){return bBlocked;}
	public String   getAttr(String name){return "";};
}

 