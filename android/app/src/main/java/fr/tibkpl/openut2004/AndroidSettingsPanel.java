package fr.tibkpl.openut2004;

import android.app.Activity;
import android.content.ContextWrapper;
import java.io.File;
import java.util.List;

/**
 * Menu des reglages en jeu, theme Unreal Tournament : meme principe que ceux d'Unreal et d'UT99
 * (rouage pour l'ecran tactile seulement, Select + Start avec une manette). Reprend tous les
 * reglages de l'ecran du lanceur : affichage, manette, langue, fichiers du jeu.
 */
final class AndroidSettingsPanel extends ContextWrapper {
    /** Resultats que l'activite du jeu traite a la fermeture du menu. */
    static final int RESTART = 3521, DATA = 3522, KEYBOARD = 3523, INPUT_MODE = 3524;
    private static final String[] RENDERERS = {"auto", "turnip", "software"};

    private final boolean french;
    private final GameSettingsMenu menu;
    private final File root;
    private final List<String> languages;
    private int result;
    private int renderer, resolution, fpsCap;
    private boolean fps;
    private String language;

    static GameSettingsMenu open(Activity activity, File root, List<String> languages, GameSettingsMenu.Result completed) {
        return new AndroidSettingsPanel(activity, root, languages, completed).menu;
    }

    private String text(String fr, String en) { return french ? fr : en; }
    private void finish() { menu.dismiss(); }

    private AndroidSettingsPanel(Activity activity, File root, List<String> languages, GameSettingsMenu.Result completed) {
        super(activity);
        this.root = root;
        this.languages = languages == null ? new java.util.ArrayList<>() : languages;
        // Le menu suit la langue choisie pour le jeu ("frt" = francais) ; a defaut, celle du systeme.
        String chosen = this.languages.isEmpty() ? null : GameData.getLanguage(this, this.languages);
        french = chosen != null ? "frt".equals(chosen)
                : "fr".equalsIgnoreCase(getResources().getConfiguration().locale.getLanguage());
        for (int i = 0; i < RENDERERS.length; i++) if (RENDERERS[i].equals(GameData.getRenderer(this))) renderer = i;
        for (int i = 0; i < GameData.RENDER_HEIGHTS.length; i++)
            if (GameData.RENDER_HEIGHTS[i] == GameData.getRenderHeight(this)) resolution = i;
        for (int i = 0; i < GameData.FPS_CAPS.length; i++)
            if (GameData.FPS_CAPS[i] == GameData.getFpsCap(this)) fpsCap = i;
        fps = GameData.showFps(this);
        language = this.languages.isEmpty() ? null : GameData.getLanguage(this, this.languages);
        menu = new GameSettingsMenu(activity, true, R.drawable.menu_logo_ut2004, french);
        menu.setOnDismissListener(dialog -> {
            saveGraphics();
            if (!activity.isFinishing()) completed.complete(result);
        });
        menu.page(text("Affichage", "Display"), this::display);
        menu.page(text("Commandes", "Controls"), this::controls);
        menu.page(text("Manette", "Controller"), this::controller);
        menu.page(text("Jeu", "Game"), this::game);
        menu.page(text("Fichiers", "Game data"), this::data);
        menu.show("UNREAL TOURNAMENT 2004", text("RÉGLAGES ANDROID  /  UT2004", "ANDROID SETTINGS  /  UT2004"), -1);
    }

    private void saveGraphics() {
        GameData.saveGraphics(this, GameData.RENDER_HEIGHTS[resolution], RENDERERS[renderer], fps);
        GameData.saveFpsCap(this, GameData.FPS_CAPS[fpsCap]);
    }

    private void display() {
        menu.section(text("Rendu", "Rendering"));
        menu.choice(text("Pilote graphique", "Graphics driver"), getResources().getStringArray(R.array.graphics_drivers),
                renderer, 0, index -> renderer = index);
        menu.choice(text("Résolution de rendu", "Render resolution"), getResources().getStringArray(R.array.graphics_resolutions),
                resolution, GameData.RENDER_HEIGHTS.length > 1 ? 1 : 0, index -> resolution = index);
        String[] caps = new String[GameData.FPS_CAPS.length];
        for (int i = 0; i < caps.length; i++)
            caps[i] = GameData.FPS_CAPS[i] == 0 ? text("Sans limite", "Unlimited") : GameData.FPS_CAPS[i] + " FPS";
        menu.choice(text("Limite de FPS", "FPS limit"), caps, fpsCap, 2, index -> fpsCap = index);
        menu.choice(text("Afficher les FPS du moteur", "Show engine FPS"),
                new String[]{text("Non", "No"), text("Oui", "Yes")}, fps ? 1 : 0, 0, index -> fps = index == 1);
        menu.note(text("Pour plus de fluidité : Turnip, 720p ou 540p. Une limite de FPS régulière (60 ou 120 sur un écran 120 Hz) évite les à-coups. Le rendu logiciel sert au dépannage. Pilote, résolution et limite prennent effet après relance.",
                "For smoother play: Turnip, 720p or 540p. A steady FPS limit (60, or 120 on a 120 Hz screen) avoids stutter. Software rendering is for troubleshooting. Driver, resolution and limit apply after a restart."));
        menu.button(text("Appliquer et relancer le jeu…", "Apply and restart the game…"), () ->
                confirm(text("Quitter la partie et relancer le jeu avec ces réglages ?", "Leave the match and restart the game with these settings?"), RESTART));
    }

    private void controls() {
        menu.section(text("Souris et clavier", "Mouse and keyboard"));
        menu.button(text("Basculer curseur / visée", "Toggle cursor / aiming"), () -> { result = INPUT_MODE; finish(); });
        menu.button(text("Saisir du texte…", "Type text…"), () -> { result = KEYBOARD; finish(); });
        menu.note(text("Select + Start ouvre ce menu à la manette. Le rouage n’apparaît qu’en mode tactile sans manette.",
                "Select + Start opens these settings with a controller. The gear icon appears in touch mode only, with no controller connected."));
    }

    private void controller() {
        menu.section(text("Manettes", "Controllers"));
        menu.button(text("Configurer les boutons et les sticks…", "Configure buttons and sticks…"), () ->
                ControllerSettings.show(menu.getOwnerActivity()));
        menu.note(text("Le réglage est mémorisé par manette. Choisissez un bouton puis appuyez sur la touche voulue.",
                "Settings are saved per controller. Pick a button, then press the key you want."));
    }

    private void game() {
        menu.section(text("Langue du jeu", "Game language"));
        if (languages.isEmpty()) {
            menu.note(text("Aucune langue détectée dans les fichiers du jeu.", "No language detected in the game data."));
            return;
        }
        String[] names = new String[languages.size()];
        for (int i = 0; i < names.length; i++) names[i] = GameData.languageName(languages.get(i));
        menu.choice(text("Langue", "Language"), names, Math.max(0, languages.indexOf(language)), 0,
                index -> language = languages.get(index));
        menu.note(text("La traduction doit être présente dans les fichiers du jeu. La langue prend effet après relance.",
                "The translation must be present in the game data. Language changes require a restart."));
        menu.button(text("Appliquer la langue et relancer…", "Apply language and restart…"), () ->
                confirm(text("Quitter la partie et relancer le jeu avec cette langue ?", "Leave the match and restart with this language?"), RESTART));
    }

    private void data() {
        menu.section(text("Emplacement du jeu", "Game location"));
        menu.path(root.getAbsolutePath());
        menu.button(text("Changer de fichiers ou télécharger le jeu…", "Change game data or download the game…"), () ->
                confirm(text("Quitter la partie et ouvrir la gestion des fichiers du jeu ? Les fichiers actuels seront conservés.",
                        "Leave the match and open the game data manager? Current files will be preserved."), DATA));
    }

    private void confirm(String message, int action) {
        menu.confirm(message, text("Continuer", "Continue"), () -> {
            if (action == RESTART && language != null) GameData.saveLanguage(this, language);
            result = action;
            finish();
        });
    }
}
