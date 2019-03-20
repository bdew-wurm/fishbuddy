package net.bdew.wurm.fishbuddy;

import com.wurmonline.client.renderer.FishingSystem;
import com.wurmonline.client.renderer.gui.FishBuddyWindow;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.math.WMath;
import com.wurmonline.shared.constants.FishingEnums;
import com.wurmonline.shared.constants.PlayerAction;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import java.lang.reflect.InvocationTargetException;

public class Hooks {
    public static HeadsUpDisplay hud;
    public static FishBuddyWindow window;

    private static long fishTool = -10L;
    private static long fishTarget = -10L;
    private static long lastSentAction = Long.MIN_VALUE;

    public static void startFishing(FishingSystem.Mode type, float minRadius, float maxRadius, byte rodType, byte rodMaterial, byte reelType, byte reelMaterial, byte floatType, byte baitType, boolean auto) {
        if (!window.isActive()) return;

        window.setText(1, "Active: " + type.name());
        window.setText(2, "Status: Casting rod");

        float rot = hud.getWorld().getPlayerRotX();
        float x = (float) (hud.getWorld().getPlayerPosX() + Math.sin(rot * WMath.DEG_TO_RAD) * maxRadius * 0.95f);
        float y = (float) (hud.getWorld().getPlayerPosY() - Math.cos(rot * WMath.DEG_TO_RAD) * maxRadius * 0.95f);

        FishingSystem fishing = Hooks.hud.getWorld().getWorldRenderer().getFishing();

        try {
            ReflectionUtil.setPrivateField(fishing, ReflectionUtil.getField(FishingSystem.class, "targetX"), x);
            ReflectionUtil.setPrivateField(fishing, ReflectionUtil.getField(FishingSystem.class, "targetY"), y);
            ReflectionUtil.callPrivateMethod(fishing, ReflectionUtil.getMethod(FishingSystem.class, "castRod"));
        } catch (IllegalAccessException | NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            error(e);
        }
    }

    public static void setFishBiting(long playerId, boolean hooked, byte fishType, long fishId) {
        if (!window.isActive()) return;
        if (playerId == -1) {
            if (hooked) {
                window.setText(2, "Status: Fish biting!");
                window.setText(3, "Fish: " + FishingEnums.FishType.fromInt(fishType).name().toLowerCase());
                FishingSystem fishing = Hooks.hud.getWorld().getWorldRenderer().getFishing();
                try {
                    ReflectionUtil.callPrivateMethod(fishing, ReflectionUtil.getMethod(FishingSystem.class,
                            "strikeRod", new Class[]{Boolean.TYPE}), true);
                } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    error(e);
                }
            } else {
                window.setText(2, "Status: Fish escaped, waiting...");
                window.setText(3, "");
            }
        }
    }

    public static void setFishCasted(long playerId, byte floatType, float posX, float posY) {

    }

    public static void setFishHooked(long playerId) {
        if (window.isActive() && playerId == -1) window.setText(2, "Status: Fish hooked, pray to RNGesus!");
    }

    public static void cancelFishing(long playerId) {
        if (window.isActive() && playerId == -1) restartFishing();
    }

    public static void showFishStrike() {

    }

    public static void showSpearStrike(long playerId, float posX, float posY) {

    }

    public static void textMessage(String title, float r, float g, float b, String message, byte onScreenType) {
        if (!window.isActive()) return;
        if (title.equals(":Event")) {
            if (message.equals("You cast the line and start fishing.")) {
                window.setText(2, "Waiting for fish...");
            } else if (message.startsWith("Fishing rod needs a ") || message.startsWith("Fishing pole needs a ")) {
                pause("Cant fish - missing gear");
            } else if (message.startsWith("The water is too shallow")) {
                pause("Cant fish - too shallow");
            }
        }
    }

    public static void sendAction(long source, long[] targets, PlayerAction action) {
        if (action.getId() == PlayerAction.CMD_FISH && targets.length == 1 && !window.isActive()) {
            window.show();
            window.setActive(true);
            window.clearText();
            window.setText(1, "Starting");
            fishTool = source;
            fishTarget = targets[0];
        }
    }

    public static void checkSendAction() {
        if (window.isActive() && lastSentAction + 500 < System.currentTimeMillis()) restartFishing();
    }

    private static void restartFishing() {
        window.clearText();
        window.setText(1, "Restarting");
        hud.getWorld().getServerConnection().sendAction(fishTool, new long[]{fishTarget}, PlayerAction.FISH);
        lastSentAction = System.currentTimeMillis();
    }

    private static void error(Throwable e) {
        window.setActive(false);
        window.setText(1, "Error! (Check logs)");
        FishBuddy.logException("Error in fishing", e);
    }

    private static void pause(String reason) {
        window.clearText();
        window.setText(1, reason);
        window.setActive(false);
    }
}
