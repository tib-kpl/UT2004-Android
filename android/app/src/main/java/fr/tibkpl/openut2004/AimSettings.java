package fr.tibkpl.openut2004;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Aides a la visee a la manette. Les reglages de forme du stick (sensibilite, zone morte, courbe,
 * acceleration) et le gyroscope agissent cote appli et marchent aussi en ligne ; l'aide du moteur
 * (PlayerController.bAimingHelp) elargit le cone de visee et anticipe les cibles, hors ligne seulement.
 */
final class AimSettings {
    private AimSettings() {}

    static final int DEFAULT_SENS = 100, DEFAULT_DEADZONE = 15, DEFAULT_CURVE = 1, DEFAULT_ACCEL = 0,
            DEFAULT_GYRO_SENS = 100;
    static final String[] CURVES_FR = {"Linéaire", "Progressive", "Précise", "Dynamique"};
    static final String[] CURVES_EN = {"Linear", "Progressive", "Precise", "Dynamic"};
    /** Force de l'acceleration de rotation (surcroit de vitesse au bout du stick, en plus de 1). */
    static final float[] ACCEL_BOOST = {0f, .35f, .7f, 1.2f};

    /** Niveaux d'aide : {sensX, sensY, zone morte, courbe, acceleration, aide du moteur (0/1)}. 4 = personnalise. */
    static final int[][] PRESETS = {
            {100, 100, 15, 1, 0, 0},   // desactivee
            {90, 90, 12, 1, 1, 0},     // legere
            {80, 80, 10, 3, 2, 1},     // moyenne
            {70, 75, 8, 3, 3, 1},      // forte
    };

    /** Niveau correspondant aux reglages donnes, ou 4 (personnalise). */
    static int presetOf(int sensX, int sensY, int deadzone, int curve, int accel, boolean engine) {
        for (int i = 0; i < PRESETS.length; i++) {
            int[] p = PRESETS[i];
            if (p[0] == sensX && p[1] == sensY && p[2] == deadzone && p[3] == curve && p[4] == accel
                    && (p[5] == 1) == engine) return i;
        }
        return 4;
    }

    /** Valeurs lues une fois, puis lues sans verrou par la boucle de visee. */
    static final class Snapshot {
        float sensX = 1f, sensY = 1f, deadzone = .15f;
        int curve = DEFAULT_CURVE;
        float accel = 0f;
        boolean invertY, gyro;
        float gyroSens = 1f;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("aim", Context.MODE_PRIVATE);
    }

    static int sensX(Context c) { return prefs(c).getInt("sensX", DEFAULT_SENS); }
    static int sensY(Context c) { return prefs(c).getInt("sensY", DEFAULT_SENS); }
    static int deadzone(Context c) { return prefs(c).getInt("deadzone", DEFAULT_DEADZONE); }
    static int curve(Context c) { return prefs(c).getInt("curve", DEFAULT_CURVE); }
    static int accel(Context c) { return prefs(c).getInt("accel", DEFAULT_ACCEL); }
    static boolean invertY(Context c) { return prefs(c).getBoolean("invertY", false); }
    static boolean gyro(Context c) { return prefs(c).getBoolean("gyro", false); }
    static int gyroSens(Context c) { return prefs(c).getInt("gyroSens", DEFAULT_GYRO_SENS); }
    static boolean engineAimHelp(Context c) { return prefs(c).getBoolean("engineAimHelp", false); }

    static void save(Context c, int sensX, int sensY, int deadzone, int curve, int accel, boolean invertY,
            boolean gyro, int gyroSens, boolean engineAimHelp) {
        prefs(c).edit().putInt("sensX", sensX).putInt("sensY", sensY).putInt("deadzone", deadzone)
                .putInt("curve", curve).putInt("accel", accel).putBoolean("invertY", invertY)
                .putBoolean("gyro", gyro).putInt("gyroSens", gyroSens)
                .putBoolean("engineAimHelp", engineAimHelp).apply();
    }

    static Snapshot snapshot(Context c) {
        Snapshot s = new Snapshot();
        s.sensX = sensX(c) / 100f;
        s.sensY = sensY(c) / 100f;
        s.deadzone = deadzone(c) / 100f;
        s.curve = curve(c);
        s.accel = ACCEL_BOOST[Math.max(0, Math.min(ACCEL_BOOST.length - 1, accel(c)))];
        s.invertY = invertY(c);
        s.gyro = gyro(c);
        s.gyroSens = gyroSens(c) / 100f;
        return s;
    }

    /** Reponse du stick : s dans [0, 1] apres zone morte -> [0, 1]. */
    static float curve(int curve, float s) {
        switch (curve) {
            case 0: return s;
            case 2: return s * s * s;
            case 3: return s < .6f ? s * s * .6f : .216f + (s - .6f) * 1.96f; // precis puis rapide
            default: return s * s;
        }
    }
}
