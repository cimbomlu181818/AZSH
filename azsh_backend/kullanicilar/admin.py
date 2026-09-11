from django.contrib import admin
from .models import Kullanici, Ayarlar, Cihaz


class CihazInline(admin.TabularInline):
    model = Cihaz
    extra = 0
    fields = ('marka', 'model', 'cihaz_id', 'eklenme_tarihi')
    readonly_fields = ('cihaz_id', 'eklenme_tarihi')


@admin.register(Kullanici)
class KullaniciAdmin(admin.ModelAdmin):
    list_display = ('email', 'mail_dogrulandi', 'premium_mi', 'trial_bitis', 'cihaz_sayisi')
    search_fields = ('email',)
    list_filter = ('premium_mi', 'mail_dogrulandi')
    inlines = [CihazInline]

    def cihaz_sayisi(self, obj):
        return obj.cihazlar.count()
    cihaz_sayisi.short_description = "Cihaz Sayısı"


@admin.register(Ayarlar)
class AyarlarAdmin(admin.ModelAdmin):
    list_display = ('bakim_modu_acik', 'bakim_mesaji')

    def has_add_permission(self, request):
        if Ayarlar.objects.exists():
            return False
        return True