package com.jarves.android;
import android.content.*;
public class TaskerReceiver extends BroadcastReceiver { @Override public void onReceive(Context c, Intent i){ String cmd=i.getStringExtra("command"); if(cmd==null) cmd=""; Intent s=new Intent(c,JarvesService.class); c.startService(s); } }

// Jarves v0.3
