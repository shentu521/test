package com.rtc.client;

import java.util.Vector;

public class object_user_sheet {
	Vector<object_user> usrvec = new Vector<object_user>();
	protected void AddUser(String userid, boolean disabled, boolean blocked, String info)
	{ 
		object_user user = new object_user();
		user.userid = userid;
		user.bDisable = disabled;
		user.bBlocked = blocked;
		user.userinfo = info;
		usrvec.add(user);
	}
	
	public int size()
	{
		return usrvec.size();
	}
	public object_user item(int i)
	{
		int nSize = usrvec.size();
		if(nSize>0 && i>=0 && i<nSize)
			return usrvec.get(i);
		return null;		
	}
}
