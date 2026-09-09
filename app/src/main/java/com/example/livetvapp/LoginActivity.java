package com.example.livetvapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etSifre, etDogrulamaKodu;
    private Button btnGirisYap, btnKayitOl, btnGoogle;
    private TextView tvHata, tvTrialBilgisi, tvTrialDolduBanner, tvPremiumBilgisi;
    private ProgressBar progressBar;

    private View layoutDogrulama;
    private TextView tvDogrulamaEmail;
    private Button btnKoduOnayla;
    private Button btnTekrarGonder;
    private Button btnGirisEkraninaGeri;

    private ApiHelper apiHelper;
    private String beklenenDogrulamaEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        apiHelper = new ApiHelper();

        boolean trialDoldu = getIntent().getBooleanExtra("trial_doldu", false);
        baglantilariYap();
        olaylariAyarla();

        // Google ile giriş şimdilik desteklenmiyor, gizle
        btnGoogle.setVisibility(View.GONE);

        if (trialDoldu) {
            tvTrialDolduBanner.setVisibility(View.VISIBLE);
        }

        String kayitliEmail = kayitliEmailGetir();
        if (kayitliEmail != null) {
            erisimKontrolEt(kayitliEmail);
        }

        String hataMesaji = getIntent().getStringExtra("hata_mesaji");
        if (hataMesaji != null) {
            hataGoster(hataMesaji);
        }
    }

    private void baglantilariYap() {
        etEmail             = findViewById(R.id.etEmail);
        etSifre             = findViewById(R.id.etSifre);
        etDogrulamaKodu     = findViewById(R.id.etDogrulamaKodu);
        btnGirisYap         = findViewById(R.id.btnGirisYap);
        btnKayitOl          = findViewById(R.id.btnKayitOl);
        btnGoogle           = findViewById(R.id.btnGoogle);
        tvHata              = findViewById(R.id.tvHata);
        tvTrialBilgisi      = findViewById(R.id.tvTrialBilgisi);
        tvTrialDolduBanner  = findViewById(R.id.tvTrialDolduBanner);
        tvPremiumBilgisi    = findViewById(R.id.tvPremiumBilgisi);
        progressBar         = findViewById(R.id.progressBar);

        layoutDogrulama      = findViewById(R.id.layoutDogrulama);
        tvDogrulamaEmail     = findViewById(R.id.tvDogrulamaEmail);
        btnKoduOnayla        = findViewById(R.id.btnKoduOnayla);
        btnTekrarGonder      = findViewById(R.id.btnTekrarGonder);
        btnGirisEkraninaGeri = findViewById(R.id.btnGirisEkraninaGeri);
    }

    private void olaylariAyarla() {
        btnGirisYap.setOnClickListener(v -> islemYap(false));
        btnKayitOl.setOnClickListener(v -> islemYap(true));

        etEmail.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                etSifre.requestFocus();
                return true;
            }
            return false;
        });
        etSifre.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                islemYap(false);
                return true;
            }
            return false;
        });

        btnKoduOnayla.setOnClickListener(v -> {
            String kod = etDogrulamaKodu.getText().toString().trim();
            if (kod.isEmpty()) {
                hataGoster("Lütfen doğrulama kodunu girin.");
                return;
            }
            klavyeGizle();
            yuklemeGoster(true);
            hataGizle();
            apiHelper.dogrula(beklenenDogrulamaEmail, kod, new ApiHelper.ApiListener() {
                @Override
                public void onBasarili(JSONObject sonuc) {
                    yuklemeGoster(false);
                    dogrulamaPaneliniGizle();
                    hataGoster("Hesabınız doğrulandı! Şimdi giriş yapabilirsiniz.");
                }
                @Override
                public void onHata(String hata) {
                    yuklemeGoster(false);
                    hataGoster(hata);
                }
            });
        });

        btnTekrarGonder.setOnClickListener(v -> {
            hataGoster("Yeni kod almak için lütfen tekrar kayıt olun.");
        });

        btnGirisEkraninaGeri.setOnClickListener(v -> dogrulamaPaneliniGizle());
    }

    private void islemYap(boolean kayitModu) {
        String email = etEmail.getText().toString().trim();
        String sifre = etSifre.getText().toString().trim();

        if (email.isEmpty()) {
            hataGoster("E-posta adresi boş olamaz.");
            etEmail.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            hataGoster("Geçerli bir e-posta adresi girin.");
            etEmail.requestFocus();
            return;
        }
        if (sifre.isEmpty()) {
            hataGoster("Şifre boş olamaz.");
            etSifre.requestFocus();
            return;
        }
        if (sifre.length() < 6) {
            hataGoster("Şifre en az 6 karakter olmalıdır.");
            etSifre.requestFocus();
            return;
        }

        klavyeGizle();
        yuklemeGoster(true);
        hataGizle();

        if (kayitModu) {
            apiHelper.kayitOl(email, sifre, new ApiHelper.ApiListener() {
                @Override
                public void onBasarili(JSONObject sonuc) {
                    yuklemeGoster(false);
                    dogrulamaPaneliniGoster(email);
                }
                @Override
                public void onHata(String hata) {
                    yuklemeGoster(false);
                    hataGoster(hata);
                }
            });
        } else {
            String cihazId = cihazIdGetir();
            apiHelper.girisYap(email, sifre, cihazId, new ApiHelper.ApiListener() {
                @Override
                public void onBasarili(JSONObject sonuc) {
                    yuklemeGoster(false);
                    kayitliEmailKaydet(email);
                    erisimKontrolEt(email);
                }
                @Override
                public void onHata(String hata) {
                    yuklemeGoster(false);
                    if ("Hesabınız henüz doğrulanmamış.".equals(hata)) {
                        dogrulamaPaneliniGoster(email);
                    } else {
                        hataGoster(hata);
                    }
                }
            });
        }
    }

    private void dogrulamaPaneliniGoster(String email) {
        beklenenDogrulamaEmail = email;
        runOnUiThread(() -> {
            hataGizle();

            etEmail.setVisibility(View.GONE);
            etSifre.setVisibility(View.GONE);
            btnGirisYap.setVisibility(View.GONE);
            btnKayitOl.setVisibility(View.GONE);
            btnGoogle.setVisibility(View.GONE);

            if (tvTrialBilgisi != null)   tvTrialBilgisi.setVisibility(View.GONE);
            if (tvPremiumBilgisi != null) tvPremiumBilgisi.setVisibility(View.GONE);

            tvDogrulamaEmail.setText(
                    email + " adresine bir doğrulama kodu gönderdik.\n\n" +
                            "Kodu aşağıya girip onaylayın, ardından giriş yapabilirsiniz."
            );
            layoutDogrulama.setVisibility(View.VISIBLE);
        });
    }

    private void dogrulamaPaneliniGizle() {
        runOnUiThread(() -> {
            layoutDogrulama.setVisibility(View.GONE);

            etEmail.setVisibility(View.VISIBLE);
            etSifre.setVisibility(View.VISIBLE);
            btnGirisYap.setVisibility(View.VISIBLE);
            btnKayitOl.setVisibility(View.VISIBLE);

            if (tvTrialBilgisi != null)   tvTrialBilgisi.setVisibility(View.VISIBLE);
            if (tvPremiumBilgisi != null) tvPremiumBilgisi.setVisibility(View.VISIBLE);

            hataGizle();
        });
    }

    private void erisimKontrolEt(String email) {
        yuklemeGoster(true);
        apiHelper.erisimKontrol(email, new ApiHelper.ApiListener() {
            @Override
            public void onBasarili(JSONObject sonuc) {
                yuklemeGoster(false);
                try {
                    boolean bakimModu = sonuc.optBoolean("bakim_modu", false);
                    if (bakimModu) {
                        hataGoster(sonuc.optString("mesaj", "Uygulama bakımda."));
                        return;
                    }
                    String durum = sonuc.optString("durum", "");
                    boolean erisim = sonuc.optBoolean("erisim", false);

                    if (erisim) {
                        uygulamayiAc();
                    } else if ("mail_dogrulanmadi".equals(durum)) {
                        dogrulamaPaneliniGoster(email);
                    } else {
                        tvTrialDolduBanner.setVisibility(View.VISIBLE);
                        hataGoster("Deneme süreniz doldu. Lütfen iletişime geçin.");
                    }
                } catch (Exception e) {
                    hataGoster("Beklenmeyen bir hata oluştu.");
                }
            }
            @Override
            public void onHata(String hata) {
                yuklemeGoster(false);
                hataGoster(hata);
            }
        });
    }

    private void uygulamayiAc() {
        Intent intent;
        if (com.example.livetvapp.remotecontrol.DeviceDetector.isPhone(this)) {
            intent = new Intent(LoginActivity.this, LauncherActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, MainActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String cihazIdGetir() {
        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private SharedPreferences tercihler() {
        return getSharedPreferences("azsh_giris", Context.MODE_PRIVATE);
    }

    private void kayitliEmailKaydet(String email) {
        tercihler().edit().putString("email", email).apply();
    }

    private String kayitliEmailGetir() {
        return tercihler().getString("email", null);
    }

    private void yuklemeGoster(boolean goster) {
        runOnUiThread(() -> {
            progressBar.setVisibility(goster ? View.VISIBLE : View.GONE);
            btnGirisYap.setEnabled(!goster);
            btnKayitOl.setEnabled(!goster);
        });
    }

    private void hataGoster(String mesaj) {
        runOnUiThread(() -> {
            tvHata.setText(mesaj);
            tvHata.setVisibility(View.VISIBLE);
        });
    }

    private void hataGizle() {
        runOnUiThread(() -> tvHata.setVisibility(View.GONE));
    }

    private void klavyeGizle() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            View focused = getCurrentFocus();
            if (focused instanceof Button) {
                focused.performClick();
                return true;
            } else if (focused == etEmail) {
                etSifre.requestFocus();
                return true;
            } else if (focused == etSifre) {
                islemYap(false);
                return true;
            }
        }
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (layoutDogrulama != null && layoutDogrulama.getVisibility() == View.VISIBLE) {
                dogrulamaPaneliniGizle();
                return true;
            }
            klavyeGizle();
            finishAffinity();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (layoutDogrulama != null && layoutDogrulama.getVisibility() == View.VISIBLE) {
            dogrulamaPaneliniGizle();
            return;
        }
        klavyeGizle();
        super.onBackPressed();
        finishAffinity();
    }
}