package com.jarves.android;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.media.AudioManager;
import android.os.*;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import java.text.SimpleDateFormat;
import java.util.*;

public class JarvesService extends Service {
    private TextToSpeech tts; private VoskEngine vosk; private boolean waitingCommand=false; private String pendingTaskerCommand="";
    @Override public void onCreate(){
        super.onCreate(); NotificationManager nm=getSystemService(NotificationManager.class);
        if(Build.VERSION.SDK_INT>=26) nm.createNotificationChannel(new NotificationChannel("jarves","Jarves",NotificationManager.IMPORTANCE_LOW));
        Notification.Builder nb=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"jarves"):new Notification.Builder(this);
        Notification n=nb.setContentTitle("Jarves ativo").setContentText("Reconhecimento offline • diga: Jarves").setSmallIcon(android.R.drawable.ic_btn_speak_now).build();
        if(Build.VERSION.SDK_INT>=29) startForeground(10,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE); else startForeground(10,n);
        tts=new TextToSpeech(this,s->{if(s==TextToSpeech.SUCCESS)tts.setLanguage(new Locale("pt","BR"));});
        vosk=new VoskEngine(this,new VoskEngine.Listener(){public void onText(String t){new Handler(Looper.getMainLooper()).post(()->handleSpeech(t));}public void onError(Exception e){new Handler(Looper.getMainLooper()).post(()->speak("Não consegui iniciar o reconhecimento offline."));}});
        vosk.start();
    }
    @Override public int onStartCommand(Intent intent,int flags,int id){
        if(intent!=null&&"TASKER_COMMAND".equals(intent.getAction())) pendingTaskerCommand=intent.getStringExtra("command");
        if(pendingTaskerCommand!=null&&!pendingTaskerCommand.trim().isEmpty()){String c=pendingTaskerCommand.trim();pendingTaskerCommand="";executeCommand(c.toLowerCase(Locale.ROOT));}
        return START_STICKY;
    }
    private void handleSpeech(String raw){String text=raw.toLowerCase(Locale.ROOT).trim();if(text.contains("jarves")){waitingCommand=true;String c=text.substring(text.indexOf("jarves")+6).trim();if(c.isEmpty())speak("Estou ouvindo. Pode falar.");else executeCommand(c);}else if(waitingCommand){waitingCommand=false;executeCommand(text);}}
    private void executeCommand(String c){
        if(c.contains("hora")||c.contains("horas")){String h=new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date());speak("Agora são "+h);}
        else if(c.contains("whatsapp"))openPackage("com.whatsapp","WhatsApp");
        else if(c.contains("youtube"))openPackage("com.google.android.youtube","YouTube");
        else if(c.contains("instagram"))openPackage("com.instagram.android","Instagram");
        else if(c.contains("facebook"))openPackage("com.facebook.katana","Facebook");
        else if(c.contains("configurações")||c.contains("configuracoes")||c.contains("configuração")){startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));speak("Abrindo as configurações.");}
        else if(c.contains("aumenta o volume")||c.contains("aumentar o volume")){((AudioManager)getSystemService(AUDIO_SERVICE)).adjustVolume(AudioManager.ADJUST_RAISE,AudioManager.FLAG_SHOW_UI);speak("Volume aumentado.");}
        else if(c.contains("diminui o volume")||c.contains("diminuir o volume")){((AudioManager)getSystemService(AUDIO_SERVICE)).adjustVolume(AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI);speak("Volume diminuído.");}
        else if(c.contains("silêncio")||c.contains("silencio")){((AudioManager)getSystemService(AUDIO_SERVICE)).adjustVolume(AudioManager.ADJUST_MUTE,AudioManager.FLAG_SHOW_UI);speak("Som silenciado.");}
        else if(c.matches(".*(ligar|ligue|chamar|chame).*\\d{8,}.*")){String n=c.replaceAll("\\D","");if(n.length()>=8)call(n);else speak("Não consegui identificar o número.");}
        else speak("Ainda não tenho esse comando. Posso receber novos comandos pelo Tasker.");
    }
    private void openPackage(String pkg,String name){Intent i=getPackageManager().getLaunchIntentForPackage(pkg);if(i!=null){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);speak("Abrindo "+name+".");}else speak(name+" não está instalado.");}
    private void call(String n){if(checkSelfPermission("android.permission.CALL_PHONE")!=android.content.pm.PackageManager.PERMISSION_GRANTED){speak("A permissão para chamadas não foi concedida.");return;}Intent i=new Intent(Intent.ACTION_CALL,android.net.Uri.parse("tel:"+n));i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);speak("Iniciando a chamada.");}
    private void speak(String s){if(tts!=null)tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"jarves");}
    @Override public void onDestroy(){if(vosk!=null)vosk.stop();if(tts!=null)tts.shutdown();super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}