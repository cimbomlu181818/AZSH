package com.example.livetvapp;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.google.firebase.installations.FirebaseInstallations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseHelper {

    // ─── GELİŞTİRME MODU ────────────────────────────────────────────────────────
    // true → Firebase'e hiç bağlanmadan tüm kontroller "başarılı" sayılır.
    // Yayına almadan önce mutlaka false yapın!
    private static final boolean DEV_MODE = true;
    // ────────────────────────────────────────────────────────────────────────────

    private static final long TRIAL_SURE_MS = 24 * 60 * 60 * 1000; // 1 gün
    private static final int MAX_CIHAZ = 2;

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final Context context;

    public interface SonucListener {
        void onBasarili();
        void onHata(String hata);
    }

    public interface ErisimListener {
        void onErisimVar();
        void onTrialBitti();
        void onGirisYok();
        void onHata(String hata);
    }

    // ─── Mail doğrulama durumu için arayüz ──────────────────────────────────
    public interface DogrulamaListener {
        void onDogrulanmamis();
        void onDogrulanmis();
        void onHata(String hata);
    }

    // ─── Cihaz ID için dahili async arayüz ──────────────────────────────────
    private interface CihazIdListener {
        void onAlindi(String cihazId);
        void onHata(String hata);
    }

    public FirebaseHelper(Context context) {
        this.context = context;
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    // ─── Mevcut kullanıcı ───────────────────────────────────────────────────
    public FirebaseUser mevcutKullanici() {
        return auth.getCurrentUser();
    }

    public boolean girisYapilmisMi() {
        if (DEV_MODE) return true; // Geliştirme: her zaman giriş yapılmış say
        return auth.getCurrentUser() != null;
    }

    // ─── Mail doğrulandı mı? ────────────────────────────────────────────────
    public boolean mailDogrulandiMi() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null && user.isEmailVerified();
    }

    // ─── Kayıt ol (doğrulama maili gönder) ──────────────────────────────────
    public void kayitOl(String email, String sifre, SonucListener listener) {
        auth.createUserWithEmailAndPassword(email, sifre)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        listener.onHata("Kullanıcı oluşturulamadı.");
                        return;
                    }
                    user.sendEmailVerification()
                            .addOnSuccessListener(v -> listener.onBasarili())
                            .addOnFailureListener(e -> listener.onHata(
                                    "Hesap oluşturuldu ancak doğrulama maili gönderilemedi: " + e.getMessage()));
                })
                .addOnFailureListener(e -> listener.onHata(e.getMessage()));
    }

    // ─── Giriş yap (mail doğrulama kontrolü ile) ────────────────────────────
    public void girisYap(String email, String sifre, SonucListener listener) {
        auth.signInWithEmailAndPassword(email, sifre)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        listener.onHata("Giriş başarısız.");
                        return;
                    }

                    if (!user.isEmailVerified()) {
                        auth.signOut();
                        listener.onHata("__mail_dogrulanmamis__");
                        return;
                    }

                    // ✅ Sunucudan zorla çek — cache kullanma
                    db.collection("users").document(user.getUid()).get(Source.SERVER)
                            .addOnSuccessListener(doc -> {
                                if (!doc.exists()) {
                                    kullaniciBelgesiOlustur(user.getUid(), user.getEmail(), listener);
                                } else {
                                    cihazKontrolVeEkle(user.getUid(), listener);
                                }
                            })
                            .addOnFailureListener(e -> listener.onHata(
                                    "İnternet bağlantısı yok. Lütfen bağlantınızı kontrol edin."));
                })
                .addOnFailureListener(e -> listener.onHata(e.getMessage()));
    }

    // ─── Doğrulama mailini tekrar gönder ────────────────────────────────────
    public void dogrulamaMailiGonder(String email, String sifre, SonucListener listener) {
        auth.signInWithEmailAndPassword(email, sifre)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        listener.onHata("Kullanıcı bulunamadı.");
                        return;
                    }
                    if (user.isEmailVerified()) {
                        auth.signOut();
                        listener.onHata("Bu hesap zaten doğrulanmış.");
                        return;
                    }
                    user.sendEmailVerification()
                            .addOnSuccessListener(v -> {
                                auth.signOut();
                                listener.onBasarili();
                            })
                            .addOnFailureListener(e -> {
                                auth.signOut();
                                listener.onHata(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> listener.onHata(e.getMessage()));
    }

    // ─── Google Sign-In client ───────────────────────────────────────────────
    public GoogleSignInClient getGoogleSignInClient() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        return GoogleSignIn.getClient(context, gso);
    }

    // ─── Google ile giriş yap ────────────────────────────────────────────────
    public void googleIleGirisYap(GoogleSignInAccount account, SonucListener listener) {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(account.getIdToken(), null))
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        listener.onHata("Giriş başarısız.");
                        return;
                    }
                    if (result.getAdditionalUserInfo() != null && result.getAdditionalUserInfo().isNewUser()) {
                        kullaniciBelgesiOlustur(user.getUid(), user.getEmail(), listener);
                    } else {
                        cihazKontrolVeEkle(user.getUid(), listener);
                    }
                })
                .addOnFailureListener(e -> listener.onHata(e.getMessage()));
    }

    public void cikisYap() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            // Zaten çıkış yapılmış, Google oturumunu da kapat
            getGoogleSignInClient().signOut();
            return;
        }

        String uid = user.getUid();

        // ✅ Önce cihazı Firestore'dan sil, SONRA auth.signOut() çağır
        // Çünkü Firestore kuralı request.auth != null gerektiriyor
        getCihazId(new CihazIdListener() {
            @Override
            public void onAlindi(String cihazId) {
                db.collection("users").document(uid).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                List<String> cihazlar = (List<String>) doc.get("devices");
                                if (cihazlar != null) {
                                    cihazlar.remove(cihazId);
                                    db.collection("users").document(uid)
                                            .update("devices", cihazlar)
                                            .addOnCompleteListener(task -> {
                                                // ✅ Firestore işlemi bitti, şimdi çıkış yap
                                                auth.signOut();
                                                getGoogleSignInClient().signOut();
                                            });
                                } else {
                                    auth.signOut();
                                    getGoogleSignInClient().signOut();
                                }
                            } else {
                                auth.signOut();
                                getGoogleSignInClient().signOut();
                            }
                        })
                        .addOnFailureListener(e -> {
                            // Firestore'a ulaşılamazsa bile çıkış yap
                            auth.signOut();
                            getGoogleSignInClient().signOut();
                        });
            }

            @Override
            public void onHata(String hata) {
                // Cihaz ID alınamazsa bile çıkış yap
                auth.signOut();
                getGoogleSignInClient().signOut();
            }
        });
    }

    // ─── Erişim kontrolü ─────────────────────────────────────────────────────
    // ✅ getIdToken(true) ile token sunucudan doğrulanır — internet yoksa hata verir
    // ✅ get(Source.SERVER) ile Firestore cache'i bypass edilir — internet yoksa hata verir
    public void erisimKontrolEt(ErisimListener listener) {
        // ─── GELİŞTİRME MODU: Firebase kontrolü atlanıyor ───────────────────────
        if (DEV_MODE) {
            listener.onErisimVar();
            return;
        }
        // ─────────────────────────────────────────────────────────────────────────
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onGirisYok();
            return;
        }

        // Önce token'ı sunucudan doğrula — internet yoksa burda hata verir
        user.getIdToken(true)
                .addOnSuccessListener(tokenResult -> {
                    // Token geçerli → Firestore'dan sunucu verisi çek
                    db.collection("users").document(user.getUid()).get(Source.SERVER)
                            .addOnSuccessListener(doc -> {
                                if (!doc.exists()) {
                                    listener.onHata("Kullanıcı verisi bulunamadı.");
                                    return;
                                }

                                Boolean isPremium = doc.getBoolean("isPremium");
                                if (isPremium != null && isPremium) {
                                    listener.onErisimVar();
                                    return;
                                }

                                Long trialEnd = doc.getLong("trialEnd");
                                if (trialEnd == null) {
                                    listener.onTrialBitti();
                                    return;
                                }

                                long simdi = System.currentTimeMillis();
                                if (simdi < trialEnd) {
                                    listener.onErisimVar();
                                } else {
                                    listener.onTrialBitti();
                                }
                            })
                            .addOnFailureListener(e -> listener.onHata(
                                    "İnternet bağlantısı yok. Lütfen bağlantınızı kontrol edin."));
                })
                .addOnFailureListener(e -> listener.onHata(
                        "İnternet bağlantısı yok. Lütfen bağlantınızı kontrol edin."));
    }

    // ─── Cihaz ID ─────────────────────────────────────────────────────────────
    private void getCihazId(CihazIdListener listener) {
        FirebaseInstallations.getInstance().getId()
                .addOnSuccessListener(id -> listener.onAlindi(id))
                .addOnFailureListener(e -> listener.onHata(e.getMessage()));
    }

    // ─── Yardımcı: kullanıcı belgesi oluştur ─────────────────────────────────
    private void kullaniciBelgesiOlustur(String uid, String email, SonucListener listener) {
        getCihazId(new CihazIdListener() {
            @Override
            public void onAlindi(String cihazId) {
                long simdi = com.google.firebase.Timestamp.now().toDate().getTime();

                List<String> cihazlar = new ArrayList<>();
                cihazlar.add(cihazId);

                Map<String, Object> veri = new HashMap<>();
                veri.put("email", email);
                veri.put("trialStart", simdi);
                veri.put("trialEnd", simdi + TRIAL_SURE_MS);
                veri.put("isPremium", false);
                veri.put("devices", cihazlar);

                db.collection("users").document(uid).set(veri)
                        .addOnSuccessListener(v -> listener.onBasarili())
                        .addOnFailureListener(e -> listener.onHata(e.getMessage()));
            }

            @Override
            public void onHata(String hata) {
                listener.onHata("Cihaz kimliği alınamadı: " + hata);
            }
        });
    }

    // ─── Yardımcı: cihaz kontrol ve ekle ─────────────────────────────────────
    private void cihazKontrolVeEkle(String uid, SonucListener listener) {
        getCihazId(new CihazIdListener() {
            @Override
            public void onAlindi(String cihazId) {
                // ✅ Cihaz kontrolünde de sunucudan çek
                db.collection("users").document(uid).get(Source.SERVER)
                        .addOnSuccessListener(doc -> {
                            if (!doc.exists()) {
                                listener.onHata("Kullanıcı verisi bulunamadı.");
                                return;
                            }

                            List<String> cihazlar = (List<String>) doc.get("devices");
                            if (cihazlar == null) cihazlar = new ArrayList<>();

                            if (cihazlar.contains(cihazId)) {
                                listener.onBasarili();
                                return;
                            }

                            if (cihazlar.size() >= MAX_CIHAZ) {
                                listener.onHata("Bu hesap zaten 2 cihazda aktif.\nÇıkış yapmak için diğer cihazı kullanın.");
                                auth.signOut();
                                return;
                            }

                            cihazlar.add(cihazId);
                            final List<String> finalCihazlar = cihazlar;
                            db.collection("users").document(uid)
                                    .update("devices", finalCihazlar)
                                    .addOnSuccessListener(v -> listener.onBasarili())
                                    .addOnFailureListener(e -> listener.onHata(e.getMessage()));
                        })
                        .addOnFailureListener(e -> listener.onHata(
                                "İnternet bağlantısı yok. Lütfen bağlantınızı kontrol edin."));
            }

            @Override
            public void onHata(String hata) {
                listener.onHata("Cihaz kimliği alınamadı: " + hata);
            }
        });
    }
    // ─── Remote Config: Bakım modu kontrolü ─────────────────────────────────────
    public interface BakimModuListener {
        void onAktif(String mesaj, String url);   // Bakım modu açık
        void onPasif();                            // Normal, devam et
        void onHata(String hata);
    }

    public void bakimModuKontrolEt(BakimModuListener listener) {
        if (DEV_MODE) { listener.onPasif(); return; } // Geliştirme: bakım modu yok
        com.google.firebase.remoteconfig.FirebaseRemoteConfig remoteConfig =
                com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance();

        com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings settings =
                new com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(0) // Anlık güncelleme
                        .build();
        remoteConfig.setConfigSettingsAsync(settings);

        // Varsayılan değerler (internet yoksa bunlar kullanılır)
        java.util.Map<String, Object> varsayilanlar = new java.util.HashMap<>();
        varsayilanlar.put("bakim_modu", false);
        varsayilanlar.put("bakim_mesaji", "Uygulama güncelleniyor, lütfen bekleyin.");
        varsayilanlar.put("bakim_url", "");
        remoteConfig.setDefaultsAsync(varsayilanlar);

        remoteConfig.fetchAndActivate()
                .addOnSuccessListener(updated -> {
                    boolean bakimModu = remoteConfig.getBoolean("bakim_modu");
                    if (bakimModu) {
                        String mesaj = remoteConfig.getString("bakim_mesaji");
                        String url   = remoteConfig.getString("bakim_url");
                        listener.onAktif(mesaj, url);
                    } else {
                        listener.onPasif();
                    }
                })
                .addOnFailureListener(e -> listener.onHata(e.getMessage()));
    }
}