from django.db import models
from django.contrib.auth.hashers import make_password, check_password


class Kullanici(models.Model):
    email = models.EmailField(unique=True)
    sifre = models.CharField(max_length=255)

    mail_dogrulandi = models.BooleanField(default=False)
    dogrulama_kodu = models.CharField(max_length=64, blank=True, null=True)

    trial_baslangic = models.DateTimeField(auto_now_add=True)
    trial_bitis = models.DateTimeField(null=True, blank=True)

    premium_mi = models.BooleanField(default=False)

   

    olusturma_tarihi = models.DateTimeField(auto_now_add=True)

    def sifre_belirle(self, ham_sifre):
        self.sifre = make_password(ham_sifre)

    def sifre_dogru_mu(self, ham_sifre):
        return check_password(ham_sifre, self.sifre)

    def __str__(self):
        return self.email


class Ayarlar(models.Model):
    bakim_modu_acik = models.BooleanField(default=False)
    bakim_mesaji = models.CharField(
        max_length=255,
        default="Uygulama bakımda, birazdan döneceğiz."
    )

    def __str__(self):
        return "Genel Ayarlar"


class Cihaz(models.Model):
    kullanici = models.ForeignKey(Kullanici, on_delete=models.CASCADE, related_name="cihazlar")
    cihaz_id = models.CharField(max_length=255)
    marka = models.CharField(max_length=100, blank=True, default="")
    model = models.CharField(max_length=100, blank=True, default="")
    eklenme_tarihi = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"{self.marka} {self.model} ({self.kullanici.email})"