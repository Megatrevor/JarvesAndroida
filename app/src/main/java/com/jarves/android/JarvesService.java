package com.jarves.android;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.media.*;
import android.os.*;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.vosk.Model;
import org.vosk.Recognizer;

public class JarvesService extends Service {
    private static final int NOTIFICATION_ID = 10;
    private AudioRecord recorder;
    private Model model;
    private Recognizer recognizer;
    private TextToSpeech tts;
    private ExecutorService worker;
    private volatile boolean running;
    private boolean waitingCommand;

    @Override public void onCreate() {
        super.onCreate();
        createNotification("Inicializando Jarves...");
        startForegroundCompat();
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("pt", "BR"));
        });
        worker = Executors.newSingleThreadExecutor();
        worker.execute(() -> {
            try {
                if (!ModelManager.isInstalled(this)) {
                    updateNotification("Baixando modelo de voz em português...");
                    ModelManager.download(this);
                }
                model = new Model(ModelManager.getModelDir(this).getAbsolutePath());
                updateNotification("Jarves ativo — diga Jarves");
                startOfflineRecognition();
            } catch (Exception e) {
                updateNotification("Falha ao carregar reconhecimento de voz");
                speak("Não consegui carregar o reconhecimento offline.");
            }
        });
    }

    private void startForegroundCompat() {
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, buildNotification("Inicializando Jarves..."),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            startForeground(NOTIFICATION_ID, buildNotification("Inicializando Jarves..."));
        }
    }

    private void createNotification(String text) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(
                "jarves", "Jarves", NotificationManager.IMPORTANCE_LOW));
    }

    private Notification buildNotification(String text) {
        return new Notification.Builder(this, "jarves")
                .setContentTitle("Jarves")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.notify(NOTIFICATION_ID, buildNotification(text));
    }

    private void startOfflineRecognition() {
        int sampleRate = 16000;
        int min = AudioRecord.getMinBufferSize(
                sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int bufferSize = Math.max(min, 8192);

        recorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        recognizer = new Recognizer(model, sampleRate);
        running = true;
        recorder.startRecording();
        byte[] buffer = new byte[bufferSize];

        while (running) {
            int read = recorder.read(buffer, 0, buffer.length);
            if (read > 0 && recognizer.acceptWaveForm(buffer, read)) {
                String json = recognizer.getResult();
                String text = extractText(json);
                if (!text.isEmpty()) handleSpeech(text);
            }
        }
    }

    private String extractText(String json) {
        int p = json.indexOf("\"text\"");
        if (p < 0) return "";
        int colon = json.indexOf(':', p);
        if (colon < 0) return "";
        int first = json.indexOf('"', colon + 1);
        int second = first < 0 ? -1 : json.indexOf('"', first + 1);
        if (first < 0 || second < 0) return "";
        return json.substring(first + 1, second).trim();
    }

    private void handleSpeech(String raw) {
        String text = raw.toLowerCase(Locale.ROOT).trim();
        if (text.contains("jarves")) {
            waitingCommand = true;
            String command = text.substring(text.indexOf("jarves") + 6).trim();
            if (command.isEmpty()) speak("Estou ouvindo. Pode falar.");
            else {
                waitingCommand = false;
                executeCommand(command);
            }
        } else if (waitingCommand) {
            waitingCommand = false;
            executeCommand(text);
        }
    }

    public void executeCommand(String c) {
        c = c.toLowerCase(Locale.ROOT).trim();
        if (c.isEmpty()) return;

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
            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            am.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
            speak("Volume aumentado.");
        } else if (c.contains("diminui o volume") || c.contains("diminuir o volume")) {
            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            am.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
            speak("Volume diminuído.");
        } else if (c.contains("silêncio") || c.contains("silencio")) {
            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            am.adjustVolume(AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI);
            speak("Som silenciado.");
        } else if (c.startsWith("ligar para ") || c.startsWith("ligue para ")) {
            String number = c.replaceAll("[^0-9+]", "");
            if (number.length() >= 8) {
                Intent dial = new Intent(Intent.ACTION_DIAL,
                        android.net.Uri.parse("tel:" + number));
                dial.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(dial);
                speak("Abrindo o telefone para ligar.");
            } else speak("Não consegui identificar o número.");
        } else {
            speak("Ainda não tenho esse comando. Posso aprender novos comandos.");
        }
    }

    private void openPackage(String pkg, String name) {
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i != null) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            speak("Abrindo " + name + ".");
        } else speak(name + " não está instalado.");
    }

    private void speak(String text) {
        if (tts != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarves");
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "TASKER_COMMAND".equals(intent.getAction())) {
            String command = intent.getStringExtra("command");
            if (command != null && !command.trim().isEmpty()) {
                new Handler(Looper.getMainLooper()).post(() -> executeCommand(command));
            }
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        running = false;
        try { if (recorder != null) recorder.stop(); } catch (Exception ignored) {}
        if (recorder != null) recorder.release();
        if (recognizer != null) recognizer.close();
        if (model != null) model.close();
        if (worker != null) worker.shutdownNow();
        if (tts != null) tts.shutdown();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
