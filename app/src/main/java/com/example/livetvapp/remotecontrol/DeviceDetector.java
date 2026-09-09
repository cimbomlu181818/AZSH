package com.example.livetvapp.remotecontrol;

import android.app.UiModeManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;

public class DeviceDetector {

    /**
     * Telefon mu?
     */
    public static boolean isPhone(Context context) {
        return !isTablet(context) && !isTvBox(context);
    }

    /**
     * Tablet mi? (Ekran boyutu 600dp ve üzeri, TV değilse)
     */
    public static boolean isTablet(Context context) {
        if (isTvBox(context)) return false;
        Configuration config = context.getResources().getConfiguration();
        int smallestWidth = config.smallestScreenWidthDp;
        return smallestWidth >= 600;
    }

    /**
     * TV veya TV-Box mı?
     */
    public static boolean isTvBox(Context context) {
        // 1. UiModeManager ile TV modu kontrolü
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        if (uiModeManager != null &&
                uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            return true;
        }

        // 2. Leanback özelliği var mı?
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
            return true;
        }

        // 3. Bilinen TV Box markaları (opsiyonel)
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        String model = Build.MODEL.toLowerCase();
        String[] tvKeywords = {
                "mibox", "xiaomi", "nvidia", "shield",
                "firetv", "amlogic", "rockchip", "h96",
                "x96", "tx9", "tx3", "mecool", "minix"
        };
        for (String keyword : tvKeywords) {
            if (manufacturer.contains(keyword) || model.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Cihaz tipini string olarak döndürür (log için)
     */
    public static String getDeviceTypeString(Context context) {
        if (isTvBox(context)) return "TV/TV-BOX";
        if (isTablet(context)) return "TABLET";
        return "TELEFON";
    }
}