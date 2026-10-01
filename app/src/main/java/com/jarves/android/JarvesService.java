package com.jarves.android;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.media.AudioManager;
import android.os.*;
import android.provider.Settings;
import android.speech.*;
import android.speech.tts.TextToSpeech;
import java.text.SimpleDateFormat;
import java.util.*;

public class JarvesService extends Service {
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private boolean waitingCommand = false;

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("jarves", "Jarves", NotificationManager.IMPORTANCE_LOW));
        Notification n = new Notification.Builder(this, "jarves")
                .setContentTitle("Jarves ativo")
                .setContentText("Diga: Jarves")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now).build();
        if (Build.VERSION.SDK_INT >= 29) startForeground(10, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        else startForeground(10, n);
        tts = new TextToSpeech(this, s -> { if (s == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("pt", "BR")); });
        listen();
    }

    private void listen() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return;
        if (recognizer != null) recognizer.destroy();
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onResults(Bundle b) {
                ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                handleSpeech((r == null || r.isEmpty()) ? "" : r.get(0));
            }
            @Override public void onError(int e) { new Handler(Looper.getMainLooper()).postDelayed(() -> listen(), 700); }
            @Override public void onReadyForSpeech(Bundle b) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float v) {}
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle b) {}
            @Override public void onEvent(int a, Bundle b) {}
        });
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR");
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        recognizer.startListening(i);
    }

    private void handleSpeech(String raw) {
        String text = raw.toLowerCase(Locale.ROOT).trim();
        if (text.contains("jarves")) {
            waitingCommand = true;
            String command = text.substring(text.indexOf("jarves") + 6).trim();
            if (command.isEmpty()) speak("Estou ouvindo. Pode falar.");
            else executeCommand(command);
        } else if (waitingCommand) {
            waitingCommand = false;
            executeCommand(text);
        }
        listen();
    }

    private void executeCommand(String c) {
        if (c.contains("hora") || c.contains("horas")) {
            String h = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            speak("Agora são " + h);
        } else if (c.contains("whatsapp")) openPackage("com.whatsapp", "WhatsApp");
        else if (c.contains("youtube")) openPackage("com.google.android.youtube", "YouTube");
        else if (c.contains("instagram")) openPackage("com.instagram.android", "Instagram");
        else if (c.contains("facebook")) openPackage("com.facebook.katana", "Facebook");
        else if (c.contains("configurações") || c.contains("configuracoes") || c.contains("configuração")) {
            startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            speak("Abrindo as configurações.");
        } else if (c.contains("aumenta o volume") || c.contains("aumentar o volume")) {
            AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE);
            am.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
            speak("Volume aumentado.");
        } else if (c.contains("diminui o volume") || c.contains("diminuir o volume")) {
            AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE);
            am.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
            speak("Volume diminuído.");
        } else if (c.contains("silêncio") || c.contains("silencio")) {
            AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE);
            am.adjustVolume(AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI);
            speak("Som silenciado.");
        } else speak("Ainda não tenho esse comando. Posso aprender novos comandos.");
    }

    private void openPackage(String pkg, String name) {
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i != null) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            speak("Abrindo " + name + ".");
        } else speak(name + " não está instalado.");
    }

    private void speak(String s) { if (tts != null) tts.speak(s, TextToSpeech.QUEUE_FLUSH, null, "jarves"); }
    @Override public int onStartCommand(Intent i, int f, int id) { return START_STICKY; }
    @Override public void onDestroy() { if (recognizer != null) recognizer.destroy(); if (tts != null) tts.shutdown(); super.onDestroy(); }
    @Override public IBinder onBind(Intent i) { return null; }
}