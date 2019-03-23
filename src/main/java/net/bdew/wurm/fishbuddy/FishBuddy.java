package net.bdew.wurm.fishbuddy;

import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import javassist.ClassPool;
import javassist.CtClass;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmClientMod;

import java.util.logging.Level;
import java.util.logging.Logger;

public class FishBuddy implements WurmClientMod, Initable, PreInitable {
    private static final Logger logger = Logger.getLogger("FishBuddy");

    public static void logException(String msg, Throwable e) {
        if (logger != null)
            logger.log(Level.SEVERE, msg, e);
    }

    public static void logInfo(String msg) {
        if (logger != null)
            logger.log(Level.INFO, msg);
    }

    @Override
    public void preInit() {
        try {
            ClassPool classPool = HookManager.getInstance().getClassPool();
            CtClass ctListener = classPool.get("com.wurmonline.client.comm.ServerConnectionListenerClass");
            CtClass ctConnection = classPool.get("com.wurmonline.client.comm.SimpleServerConnectionClass");

            ctListener.getMethod("setFishHooked", "(J)V")
                    .insertAfter("net.bdew.wurm.fishbuddy.Hooks.setFishHooked($1);");

            ctListener.getMethod("showSpearStrike", "(JFF)V")
                    .insertAfter("net.bdew.wurm.fishbuddy.Hooks.showSpearStrike($1, $2, $3);");

            ctListener.getMethod("cancelFishing", "(J)V")
                    .insertAfter("net.bdew.wurm.fishbuddy.Hooks.cancelFishing($1);");

            ctListener.getMethod("setFishBiting", "(JZBJ)V")
                    .insertAfter("net.bdew.wurm.fishbuddy.Hooks.setFishBiting($1, $2, $3, $4);");

            ctListener.getMethod("startFishing", "(Lcom/wurmonline/client/renderer/FishingSystem$Mode;FFBBBBBBZ)V")
                    .insertAfter("net.bdew.wurm.fishbuddy.Hooks.startFishing($1, $2, $3, $4, $5, $6, $7, $8, $9, $10);");

            ctListener.getMethod("setFishCasted", "(JBFF)V")
                    .insertAfter("net.bdew.wurm.fishbuddy.Hooks.setFishCasted($1, $2, $3, $4);");

            ctListener.getMethod("showFishStrike", "()V")
                    .insertAfter("net.bdew.wurm.fishbuddy.Hooks.showFishStrike();");

            ctListener.getMethod("textMessage", "(Ljava/lang/String;FFFLjava/lang/String;B)V")
                    .insertAfter("net.bdew.wurm.fishbuddy.Hooks.textMessage($1, $2, $3, $4, $5, $6);");

            ctListener.getMethod("updateInventoryItem", "(JJJLjava/lang/String;Ljava/lang/String;FFFFFFISBBBS)V")
                    .insertAfter("net.bdew.wurm.fishbuddy.Hooks.updateInventoryItem($2, $7);");

            ctConnection.getMethod("sendAction", "(J[JLcom/wurmonline/shared/constants/PlayerAction;)V")
                    .insertAfter("net.bdew.wurm.fishbuddy.Hooks.sendAction($1, $2, $3);");

            // Disable secure strings bullshit
            CtClass ctSecureStrings = classPool.getCtClass("com.wurmonline.client.util.SecureStrings");
            ctSecureStrings.getConstructor("(Ljava/lang/String;)V").setBody("this.chars = $1.toCharArray();");
            ctSecureStrings.getMethod("toString", "()Ljava/lang/String;").setBody("return new String(this.chars);");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void init() {
        try {
            // Hook HUD init to setup our stuff
            HookManager.getInstance().registerHook("com.wurmonline.client.renderer.gui.HeadsUpDisplay", "init", "(II)V", () -> (proxy, method, args) -> {
                method.invoke(proxy, args);
                Hooks.init((HeadsUpDisplay) proxy);
                return null;
            });
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
