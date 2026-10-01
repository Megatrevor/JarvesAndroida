package com.jarves.android;

import android.content.Context;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import org.vosk.Model;
import org.vosk.Recognizer;
import java.io.*;

public final class VoskEngine {
    public interface Listener { void onText(String text); void onError(Exception e); }
    private final Context context; private final Listener listener;
    private volatile boolean running; private Thread thread;
    private Model model; private Recognizer recognizer; private AudioRecord recorder;

    public VoskEngine(Context context, Listener listener) { this.context=context.getApplicationContext(); this.listener=listener; }

    public void start() {
        if (running) return; running=true;
        thread=new Thread(() -> {
            try {
                File modelDir=copyAssetTree("vosk-model-small-pt-0.3");
                model=new Model(modelDir.getAbsolutePath());
                int min=AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
                int buffer=Math.max(min,4096);
                recorder=new AudioRecord(MediaRecorder.AudioSource.MIC,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,buffer*2);
                recognizer=new Recognizer(model,16000.0f); recorder.startRecording();
                byte[] data=new byte[buffer];
                while(running){ int n=recorder.read(data,0,data.length); if(n>0 && recognizer.acceptWaveForm(data,n)){ String text=extractText(recognizer.getResult()); if(!text.isEmpty()) listener.onText(text); } }
            } catch(Exception e){ listener.onError(e); } finally { stopInternal(); }
        },"Jarves-Vosk");
        thread.start();
    }
    public void stop(){ running=false; if(recorder!=null) try{recorder.stop();}catch(Exception ignored){} }
    private void stopInternal(){ running=false; if(recorder!=null){try{recorder.stop();}catch(Exception ignored){} recorder.release();recorder=null;} if(recognizer!=null){recognizer.close();recognizer=null;} if(model!=null){model.close();model=null;} }
    private String extractText(String json){int p=json.indexOf(""text"");if(p<0)return "";int c=json.indexOf(':',p);int q1=json.indexOf('"',c+1);int q2=json.indexOf('"',q1+1);return q1>=0&&q2>q1?json.substring(q1+1,q2):"";}
    private File copyAssetTree(String root)throws IOException{File out=new File(context.getFilesDir(),root);if(out.exists())return out;copyDir(context.getAssets(),root,out);return out;}
    private void copyDir(AssetManager a,String path,File dest)throws IOException{String[] children=a.list(path);if(children==null||children.length==0){dest.getParentFile().mkdirs();try(InputStream in=a.open(path);OutputStream out=new FileOutputStream(dest)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}return;}dest.mkdirs();for(String child:children)copyDir(a,path+"/"+child,new File(dest,child));}
}