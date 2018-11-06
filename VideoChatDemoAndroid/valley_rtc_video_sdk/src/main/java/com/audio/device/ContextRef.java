package com.audio.device;

import android.content.Context;

public class ContextRef {
private Context mctx = null;
public ContextRef(Context ctx)
{
	mctx = ctx;
}
public Context getContext()
{
	return mctx; 
}
}
