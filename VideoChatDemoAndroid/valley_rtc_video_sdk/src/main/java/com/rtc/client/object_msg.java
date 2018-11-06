package com.rtc.client;
 

public class object_msg
{
	
	public final static int typeText = 1;   // 文本消息 服务器会存储
	public final static int typeAudio = 2;  // 语音消息 服务器会存储
	public final static int typeBinary = 3; // 二进制数据 服务器不存储
	public final static int typeCmd = 10;   // 命令 服务器不存储
  
	int        msgid;
	int        msgtype;
	String     fromuser;
	String     touser;
	String     token;
	long       msgtime;
	String     data; 

	public int      getMsgID() {return msgid;}
	public int      getMsgType() {return msgtype;}
	public String   getFromUserID() {return fromuser;}
	public String   getToUserID(){return touser;}
	public String   getToken() {return token;}
	public long     getTime() {return msgtime;}
	public String   getData() {return data;}
	public int      getLength() {return data.length();}
}
 