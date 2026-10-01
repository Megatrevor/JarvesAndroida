package com.jarves.android;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) { super.onCreate(b);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(32,48,32,32);
        TextView title=new TextView(this); title.setText("JARVES\nSeu assistente Android"); title.setTextSize(28); root.addView(title);
        Button start=new Button(this); start.setText("ATIVAR JARVES"); root.addView(start);
        Button stop=new Button(this); stop.setText("DESATIVAR"); root.addView(stop);
        Button mic=new Button(this); mic.setText("PERMITIR MICROFONE"); root.addView(mic);
        Button overlay=new Button(this); overlay.setText("PERMITIR SOBREPOSIÇÃO"); root.addView(overlay);
        Button access=new Button(this); access.setText("CONFIGURAR ACESSIBILIDADE"); root.addView(access);
        setContentView(root);
        start.setOnClickListener(v -> { requestMic(); if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED) startService(new Intent(this,JarvesService.class)); });
        stop.setOnClickListener(v -> stopService(new Intent(this,JarvesService.class)));
        mic.setOnClickListener(v -> requestMic());
        overlay.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName()))));
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
    }
    private void requestMic(){ if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},10); }
}
