package com.example.livetvapp;

import android.os.AsyncTask;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ApiHelper {



    private static final String BASE_URL = "http://10.0.2.2:8000/api/";

    public interface ApiListener {
        void onBasarili(JSONObject sonuc);
        void onHata(String hata);
    }


    private void istekGonder(String endpoint, JSONObject veri, ApiListener listener) {
        new AsyncTask<Void, Void, String>() {
            String hataMesaji = null;

            @Override
            protected String doInBackground(Void... voids) {
                try {
                    URL url = new URL(BASE_URL + endpoint);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);

                    OutputStream os = conn.getOutputStream();
                    os.write(veri.toString().getBytes("UTF-8"));
                    os.close();

                    int kod = conn.getResponseCode();
                    BufferedReader br;
                    if (kod >= 200 && kod < 300) {
                        br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    } else {
                        br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
                    }

                    StringBuilder sb = new StringBuilder();
                    String satir;
                    while ((satir = br.readLine()) != null) {
                        sb.append(satir);
                    }
                    br.close();
                    return sb.toString();

                } catch (Exception e) {
                    hataMesaji = "Bağlantı hatası: " + e.getMessage();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String sonucMetni) {
                if (hataMesaji != null) {
                    listener.onHata(hataMesaji);
                    return;
                }
                try {
                    JSONObject json = new JSONObject(sonucMetni);
                    if (json.has("hata")) {
                        listener.onHata(json.getString("hata"));
                    } else {
                        listener.onBasarili(json);
                    }
                } catch (Exception e) {
                    listener.onHata("Sunucu yanıtı okunamadı.");
                }
            }
        }.execute();
    }

    public void kayitOl(String email, String sifre, ApiListener listener) {
        try {
            JSONObject veri = new JSONObject();
            veri.put("email", email);
            veri.put("sifre", sifre);
            istekGonder("kayit/", veri, listener);
        } catch (Exception e) {
            listener.onHata("Veri hazırlama hatası.");
        }
    }

    public void dogrula(String email, String kod, ApiListener listener) {
        try {
            JSONObject veri = new JSONObject();
            veri.put("email", email);
            veri.put("kod", kod);
            istekGonder("dogrula/", veri, listener);
        } catch (Exception e) {
            listener.onHata("Veri hazırlama hatası.");
        }
    }

    public void girisYap(String email, String sifre, String cihazId, ApiListener listener) {
        try {
            JSONObject veri = new JSONObject();
            veri.put("email", email);
            veri.put("sifre", sifre);
            veri.put("cihaz_id", cihazId);
            istekGonder("giris/", veri, listener);
        } catch (Exception e) {
            listener.onHata("Veri hazırlama hatası.");
        }
    }

    public void erisimKontrol(String email, String cihazId, ApiListener listener) {
        try {
            JSONObject veri = new JSONObject();
            veri.put("email", email);
            veri.put("cihaz_id", cihazId);
            veri.put("marka", android.os.Build.MANUFACTURER);
            veri.put("model", android.os.Build.MODEL);
            istekGonder("erisim/", veri, listener);
        } catch (Exception e) {
            listener.onHata("Veri hazırlama hatası.");
        }
    }
}