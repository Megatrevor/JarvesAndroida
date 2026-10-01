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
    private static final int MIC = 10;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 48, 32, 32);
        TextView title = new TextView(this);
        title.setText("JARVES\nAssistente Android v0.2");
        title.setTextSize(28);
        root.addView(title);
        TextView info = new TextView(this);
        info.setText("Comandos: Jarves, que horas são? | abrir WhatsApp | abrir YouTube | abrir configurações");
        info.setTextSize(16);
        root.addView(info);
        Button start = new Button(this); start.setText("ATIVAR JARVES"); root.addView(start);
        Button stop = new Button(this); stop.setText("DESATIVAR"); root.addView(stop);
        Button mic = new Button(this); mic.setText("PERMITIR MICROFONE"); root.addView(mic);
        Button overlay = new Button(this); overlay.setText("PERMITIR SOBREPOSIÇÃO"); root.addView(overlay);
        Button access = new Button(this); access.setText("CONFIGURAR ACESSIBILIDADE"); root.addView(access);
        setContentView(root);
        start.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC);
                return;
            }
            startService(new Intent(this, JarvesService.class));
        });
        stop.setOnClickListener(v -> stopService(new Intent(this, JarvesService.class)));
        mic.setOnClickListener(v -> requestMic());
        overlay.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))));
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
    }

    private void requestMic() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC);
    }
}