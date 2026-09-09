package com.example.livetvapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etSifre;
    private Button btnGirisYap, btnKayitOl, btnGoogle;
    private TextView tvHata, tvTrialBilgisi, tvTrialDolduBanner, tvPremiumBilgisi;
    private ProgressBar progressBar;

    private View layoutDogrulama;
    private TextView tvDogrulamaEmail;
    private Button btnTekrarGonder;
    private Button btnGirisEkraninaGeri;

    private FirebaseHelper firebaseHelper;

    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseHelper = new FirebaseHelper(this);

        // Google Sign-In launcher
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        yuklemeGoster(true);
                        firebaseHelper.googleIleGirisYap(account, new FirebaseHelper.SonucListener() {
                            @Override
                            public void onBasarili() {
                                yuklemeGoster(false);
                                erisimKontrolEt();
                            }
                            @Override
                            public void onHata(String hata) {
                                yuklemeGoster(false);
                                hataGoster(hataMetniCevir(hata));
                            }
                        });
                    } catch (ApiException e) {
                        hataGoster("Google girişi başarısız: " + e.getMessage());
                    }
                });

        boolean trialDoldu = getIntent().getBooleanExtra("trial_doldu", false);
        baglantilariYap();
        olaylariAyarla();

        if (trialDoldu) {
            tvTrialDolduBanner.setVisibility(View.VISIBLE);
        }

        if (firebaseHelper.girisYapilmisMi()) {
            erisimKontrolEt();
        }

        String hataMesaji = getIntent().getStringExtra("hata_mesaji");  // ✅ if dışında
        if (hataMesaji != null) {
            hataGoster(hataMesaji);
        }
    }

    private void baglantilariYap() {
        etEmail             = findViewById(R.id.etEmail);
        etSifre             = findViewById(R.id.etSifre);
        btnGirisYap         = findViewById(R.id.btnGirisYap);
        btnKayitOl          = findViewById(R.id.btnKayitOl);
        btnGoogle           = findViewById(R.id.btnGoogle);
        tvHata              = findViewById(R.id.tvHata);
        tvTrialBilgisi      = findViewById(R.id.tvTrialBilgisi);
        tvTrialDolduBanner  = findViewById(R.id.tvTrialDolduBanner);
        tvPremiumBilgisi    = findViewById(R.id.tvPremiumBilgisi);   // ── YENİ
        progressBar         = findViewById(R.id.progressBar);

        layoutDogrulama      = findViewById(R.id.layoutDogrulama);
        tvDogrulamaEmail     = findViewById(R.id.tvDogrulamaEmail);
        btnTekrarGonder      = findViewById(R.id.btnTekrarGonder);
        btnGirisEkraninaGeri = findViewById(R.id.btnGirisEkraninaGeri);
    }

    private void olaylariAyarla() {
        btnGirisYap.setOnClickListener(v -> islemYap(false));
        btnKayitOl.setOnClickListener(v -> islemYap(true));
        btnGoogle.setOnClickListener(v -> {
            Intent signInIntent = firebaseHelper.getGoogleSignInClient().getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

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

        btnTekrarGonder.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String sifre = etSifre.getText().toString().trim();

            if (email.isEmpty() || sifre.isEmpty()) {
                hataGoster("Tekrar göndermek için e-posta ve şifrenizi girin.");
                return;
            }

            yuklemeGoster(true);
            firebaseHelper.dogrulamaMailiGonder(email, sifre, new FirebaseHelper.SonucListener() {
                @Override
                public void onBasarili() {
                    yuklemeGoster(false);
                    hataGoster("Doğrulama maili tekrar gönderildi. Lütfen gelen kutunuzu kontrol edin.");
                }
                @Override
                public void onHata(String hata) {
                    yuklemeGoster(false);
                    hataGoster(hataMetniCevir(hata));
                }
            });
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
            firebaseHelper.kayitOl(email, sifre, new FirebaseHelper.SonucListener() {
                @Override
                public void onBasarili() {
                    yuklemeGoster(false);
                    dogrulamaPaneliniGoster(email);
                }
                @Override
                public void onHata(String hata) {
                    yuklemeGoster(false);
                    hataGoster(hataMetniCevir(hata));
                }
            });
        } else {
            firebaseHelper.girisYap(email, sifre, new FirebaseHelper.SonucListener() {
                @Override
                public void onBasarili() {
                    yuklemeGoster(false);
                    erisimKontrolEt();
                }
                @Override
                public void onHata(String hata) {
                    yuklemeGoster(false);
                    if ("__mail_dogrulanmamis__".equals(hata)) {
                        dogrulamaPaneliniGoster(email);
                    } else {
                        hataGoster(hataMetniCevir(hata));
                    }
                }
            });
        }
    }

    private void dogrulamaPaneliniGoster(String email) {
        runOnUiThread(() -> {
            hataGizle();

            // Form elemanlarını gizle
            etEmail.setVisibility(View.GONE);
            etSifre.setVisibility(View.GONE);
            btnGirisYap.setVisibility(View.GONE);
            btnKayitOl.setVisibility(View.GONE);
            btnGoogle.setVisibility(View.GONE);

            // Bilgi alanlarını gizle — bunlar panelin altında kalıp karmaşa yaratıyordu
            if (tvTrialBilgisi != null)   tvTrialBilgisi.setVisibility(View.GONE);
            if (tvPremiumBilgisi != null) tvPremiumBilgisi.setVisibility(View.GONE);

            tvDogrulamaEmail.setText(
                    email + " adresine bir doğrulama maili gönderdik.\n\n" +
                            "Lütfen gelen kutunuzu (ve spam klasörünü) kontrol edin, " +
                            "linke tıkladıktan sonra buraya dönüp giriş yapabilirsiniz."
            );
            layoutDogrulama.setVisibility(View.VISIBLE);
        });
    }

    private void dogrulamaPaneliniGizle() {
        runOnUiThread(() -> {
            layoutDogrulama.setVisibility(View.GONE);

            // Form elemanlarını geri getir
            etEmail.setVisibility(View.VISIBLE);
            etSifre.setVisibility(View.VISIBLE);
            btnGirisYap.setVisibility(View.VISIBLE);
            btnKayitOl.setVisibility(View.VISIBLE);
            btnGoogle.setVisibility(View.VISIBLE);

            // Bilgi alanlarını geri getir
            if (tvTrialBilgisi != null)   tvTrialBilgisi.setVisibility(View.VISIBLE);
            if (tvPremiumBilgisi != null) tvPremiumBilgisi.setVisibility(View.VISIBLE);

            hataGizle();
        });
    }

    private void erisimKontrolEt() {
        yuklemeGoster(true);
        firebaseHelper.erisimKontrolEt(new FirebaseHelper.ErisimListener() {
            @Override
            public void onErisimVar() {
                yuklemeGoster(false);
                uygulamayiAc();
            }
            @Override
            public void onTrialBitti() {
                yuklemeGoster(false);
                firebaseHelper.cikisYap();
                tvTrialDolduBanner.setVisibility(View.VISIBLE);
                hataGoster("Deneme süreniz doldu. Lütfen iletişime geçin.");
            }
            @Override
            public void onGirisYok() {
                yuklemeGoster(false);
            }
            @Override
            public void onHata(String hata) {
                yuklemeGoster(false);
                hataGoster("Bağlantı hatası: " + hata);
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

    private void yuklemeGoster(boolean goster) {
        runOnUiThread(() -> {
            progressBar.setVisibility(goster ? View.VISIBLE : View.GONE);
            btnGirisYap.setEnabled(!goster);
            btnKayitOl.setEnabled(!goster);
            btnGoogle.setEnabled(!goster);
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

    private String hataMetniCevir(String hata) {
        if (hata == null) return "Bilinmeyen hata.";
        if (hata.contains("email address is already in use"))
            return "Bu e-posta adresi zaten kayıtlı.";
        if (hata.contains("no user record") || hata.contains("user-not-found"))
            return "Bu e-posta adresiyle kayıtlı hesap bulunamadı.";
        if (hata.contains("password is invalid") || hata.contains("wrong-password"))
            return "Şifre hatalı.";
        if (hata.contains("network"))
            return "İnternet bağlantısı hatası.";
        if (hata.contains("credential is incorrect") || hata.contains("malformed or has expired"))
            return "Lütfen önce kayıt olun veya bilgilerinizi kontrol edin.";
        if (hata.contains("CONFIGURATION_NOT_FOUND") || hata.contains("configuration"))
            return "Bağlantı hatası. Lütfen internete bağlanın.";
        if (hata.contains("NETWORK_ERROR") || hata.contains("Unable to resolve host"))
            return "İnternet bağlantısı yok. Lütfen bağlantınızı kontrol edin.";
        if (hata.contains("2 cihazda aktif"))
            return hata;
        if (hata.contains("zaten doğrulanmış"))
            return hata;
        return "Hata: " + hata;
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