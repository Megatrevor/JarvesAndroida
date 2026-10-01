package com.jarves.android;

import android.app.*; import android.content.*; import android.content.pm.ServiceInfo; import android.os.*; import android.speech.*; import android.speech.RecognizerIntent; import android.speech.SpeechRecognizer; import android.speech.tts.TextToSpeech; import java.util.*;

public class JarvesService extends Service {
    private SpeechRecognizer recognizer; private TextToSpeech tts;
    @Override public void onCreate(){ super.onCreate();
        String channel="jarves"; NotificationManager nm=getSystemService(NotificationManager.class); nm.createNotificationChannel(new NotificationChannel(channel,"Jarves",NotificationManager.IMPORTANCE_LOW));
        Notification n=new Notification.Builder(this,channel).setContentTitle("Jarves ativo").setContentText("Diga: Jarves").setSmallIcon(android.R.drawable.ic_btn_speak_now).build();
        if(Build.VERSION.SDK_INT>=29) startForeground(10,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE); else startForeground(10,n);
        tts=new TextToSpeech(this,s -> { if(s==TextToSpeech.SUCCESS) tts.setLanguage(new Locale("pt","BR")); });
        listen();
    }
    private void listen(){ if(!SpeechRecognizer.isRecognitionAvailable(this)) return; if(recognizer!=null) recognizer.destroy(); recognizer=SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener(){ public void onResults(Bundle b){ ArrayList<String> r=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION); if(r!=null) for(String x:r) if(x.toLowerCase(Locale.ROOT).contains("jarves")){ respond("Estou ouvindo. O que você precisa?"); break; } listen(); }
            public void onError(int e){ new Handler(Looper.getMainLooper()).postDelayed(()->listen(),700); } public void onReadyForSpeech(Bundle b){} public void onBeginningOfSpeech(){} public void onRmsChanged(float v){} public void onBufferReceived(byte[] b){} public void onEndOfSpeech(){} public void onPartialResults(Bundle b){} public void onEvent(int a,Bundle b){} });
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"pt-BR"); i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false); recognizer.startListening(i);
    }
    private void respond(String s){ if(tts!=null) tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"jarves"); }
    @Override public int onStartCommand(Intent i,int f,int id){ return START_STICKY; }
    @Override public void onDestroy(){ if(recognizer!=null) recognizer.destroy(); if(tts!=null) tts.shutdown(); super.onDestroy(); }
    @Override public android.os.IBinder onBind(Intent i){ return null; }
}
