package net.bdew.wurm.fishbuddy;

import com.wurmonline.client.game.PlayerPosition;
import com.wurmonline.client.renderer.FishingSystem;
import com.wurmonline.client.renderer.cell.CreatureCellRenderable;
import com.wurmonline.client.renderer.gui.FishBuddyWindow;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.math.WMath;
import com.wurmonline.shared.constants.FishingEnums;
import com.wurmonline.shared.constants.PlayerAction;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Optional;

public class Hooks {
    private static HeadsUpDisplay hud;
    private static FishBuddyWindow window;

    private static long fishTool = -10L;
    private static long fishTarget = -10L;
    private static long lastSentAction = Long.MIN_VALUE;
    private static long lastTriedStrike = Long.MIN_VALUE;

    private static FishingSystem.Mode mode = null;

    private static Field fishingTargetX, fishingTargetY;
    private static Method fishingCastRod, fishingStrikeRod;

    public static void init(HeadsUpDisplay theHud) throws NoSuchMethodException, NoSuchFieldException {
        hud = theHud;
        window = new FishBuddyWindow();

        fishingTargetX = ReflectionUtil.getField(FishingSystem.class, "targetX");
        fishingTargetY = ReflectionUtil.getField(FishingSystem.class, "targetY");
        fishingCastRod = ReflectionUtil.getMethod(FishingSystem.class, "castRod");
        fishingStrikeRod = ReflectionUtil.getMethod(FishingSystem.class, "strikeRod", new Class[]{Boolean.TYPE});
    }

    public static void startFishing(FishingSystem.Mode type, float minRadius, float maxRadius, byte rodType, byte rodMaterial, byte reelType, byte reelMaterial, byte floatType, byte baitType, boolean auto) {
        if (!window.isActive()) return;

        mode = type;

        if (mode == FishingSystem.Mode.FISHING) {
            window.setText(1, "Active: Rod fishing");
            window.setText(2, "Status: Casting rod");

            float rot = hud.getWorld().getPlayerRotX();
            float x = (float) (hud.getWorld().getPlayerPosX() + Math.sin(rot * WMath.DEG_TO_RAD) * maxRadius * 0.95f);
            float y = (float) (hud.getWorld().getPlayerPosY() - Math.cos(rot * WMath.DEG_TO_RAD) * maxRadius * 0.95f);

            FishingSystem fishing = Hooks.hud.getWorld().getWorldRenderer().getFishing();

            try {
                ReflectionUtil.setPrivateField(fishing, fishingTargetX, x);
                ReflectionUtil.setPrivateField(fishing, fishingTargetY, y);
                ReflectionUtil.callPrivateMethod(fishing, fishingCastRod);
            } catch (IllegalAccessException | InvocationTargetException e) {
                error(e);
            }
        } else if (mode == FishingSystem.Mode.SPEAR_FISHING) {
            window.setText(1, "Active: Spear fishing");
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
                    ReflectionUtil.callPrivateMethod(fishing, fishingStrikeRod, true);
                } catch (IllegalAccessException | InvocationTargetException e) {
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
                pause("Can't fish - missing gear");
            } else if (message.startsWith("The water is too shallow")) {
                pause("Can't fish - too shallow");
            } else if (message.startsWith("You wouldn't be able to carry")) {
                pause("Can't fish - inventory full");
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

    public static void pollFishing() {
        if (hud.getActionString() == null && lastSentAction + 500 < System.currentTimeMillis())
            restartFishing();
        else if (mode == FishingSystem.Mode.SPEAR_FISHING && lastTriedStrike + 500 < System.currentTimeMillis()) {
            lastTriedStrike = System.currentTimeMillis();

            PlayerPosition playerPos = hud.getWorld().getPlayer().getPos();
            float px = playerPos.getX();
            float py = playerPos.getY();

            Comparator<CreatureCellRenderable> comparator = Comparator.comparing(c -> {
                float xd = px - c.getXPos();
                float yd = py - c.getYPos();
                return xd * xd + yd * yd;
            });

            Optional<CreatureCellRenderable> fish = hud.getWorld().getServerConnection().getServerConnectionListener()
                    .getCreatures().values().stream()
                    .filter(c -> c.getModelName().toString().startsWith("model.creature.fish."))
                    .min(comparator);

            if (fish.isPresent()) {
                window.setText(2, "Status: Striking!");
                window.setText(3, String.format("Fish: %s", fish.get().getHoverName()));

                FishingSystem fishing = Hooks.hud.getWorld().getWorldRenderer().getFishing();

                float rot = fish.get().getRot();
                float x = (float) (fish.get().getXPos() + Math.sin(rot * WMath.DEG_TO_RAD) * 0.8f);
                float y = (float) (fish.get().getYPos() - Math.cos(rot * WMath.DEG_TO_RAD) * 0.8f);

                try {
                    ReflectionUtil.setPrivateField(fishing, fishingTargetX, x);
                    ReflectionUtil.setPrivateField(fishing, fishingTargetY, y);
                } catch (IllegalAccessException e) {
                    error(e);
                }

                fishing.strikeSpear(-1L, 0, 0, true);
            } else {
                window.setText(2, "Status: Looking for fish");
            }
        }
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
