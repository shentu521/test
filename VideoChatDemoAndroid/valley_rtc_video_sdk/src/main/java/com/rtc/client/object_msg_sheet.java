package com.rtc.client;
import java.util.Vector;

public class object_msg_sheet{
    Vector<object_msg> msgvec = new Vector<object_msg>();
	protected void AddMessage(int msgid, int msgtype, String fromuser, String touser, String token, long msgtime, String data)
	{ 
		object_msg msg = new object_msg();
		msg.msgid = msgid;
		msg.msgtype = msgtype;
		msg.fromuser = fromuser;
		msg.touser = touser;
		msg.token = token;
		msg.msgtime = msgtime;
		msg.data = data;  
		msgvec.add(msg);
	}
	 
	public int size()
	{
		return msgvec.size();
	}
	public object_msg item(int i)
	{
		int nSize = msgvec.size();
		if(nSize>0 && i>=0 && i<nSize)
			return msgvec.get(i);
		return null;		
	}
}

