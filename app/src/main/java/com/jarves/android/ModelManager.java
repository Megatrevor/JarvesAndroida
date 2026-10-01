package com.jarves.android;

import android.content.Context;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ModelManager {
    private static final String MODEL_URL =
            "https://alphacephei.com/vosk/models/vosk-model-small-pt-0.3.zip";
    private static final String MODEL_DIR = "vosk-model-small-pt-0.3";

    private ModelManager() {}

    public static File getModelDir(Context context) {
        return new File(context.getFilesDir(), MODEL_DIR);
    }

    public static boolean isInstalled(Context context) {
        File dir = getModelDir(context);
        return new File(dir, "am/final.mdl").exists()
                && new File(dir, "conf/model.conf").exists();
    }

    public static void download(Context context) throws IOException {
        File base = context.getFilesDir();
        File zip = new File(base, MODEL_DIR + ".zip");
        HttpURLConnection connection = (HttpURLConnection) new URL(MODEL_URL).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.setInstanceFollowRedirects(true);
        connection.connect();
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP " + connection.getResponseCode());
        }

        try (InputStream in = new BufferedInputStream(connection.getInputStream());
             OutputStream out = new BufferedOutputStream(new FileOutputStream(zip))) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
        } finally {
            connection.disconnect();
        }

        File target = getModelDir(context);
        if (target.exists()) deleteRecursively(target);
        if (!target.mkdirs() && !target.exists()) throw new IOException("Não foi possível criar o modelo");

        try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(zip)))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zin.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.contains("..")) throw new IOException("Entrada inválida no modelo");
                File out = new File(base, name);
                String canonicalBase = base.getCanonicalPath() + File.separator;
                if (!out.getCanonicalPath().startsWith(canonicalBase)) throw new IOException("Caminho inválido");
                if (entry.isDirectory()) {
                    if (!out.exists() && !out.mkdirs()) throw new IOException("Falha ao criar diretório");
                } else {
                    File parent = out.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IOException("Falha ao criar diretório");
                    try (OutputStream os = new BufferedOutputStream(new FileOutputStream(out))) {
                        int n;
                        while ((n = zin.read(buffer)) != -1) os.write(buffer, 0, n);
                    }
                }
                zin.closeEntry();
            }
        } finally {
            if (!zip.delete()) zip.deleteOnExit();
        }

        File extracted = new File(base, MODEL_DIR);
        if (!isInstalled(context)) throw new IOException("Modelo incompleto");
    }

    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteRecursively(child);
        }
        file.delete();
    }
}
