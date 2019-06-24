package com.wurmonline.client.renderer.gui;

import net.bdew.wurm.fishbuddy.Hooks;

public class FishBuddyWindow extends WWindow implements ButtonListener {
    private final WButton btStop, btPause;
    private WurmLabel[] text;

    private boolean active = false, visible = false;

    public FishBuddyWindow() {
        super("FishBuddy");
        setTitle("FishBuddy");
        resizable = false;
        closeable = false;
        text = new WurmLabel[3];
        btStop = new WButton("Stop", this);
        btPause = new WButton("Pause", this);
        btStop.setSize(150, btStop.height);
        btPause.setSize(150, btPause.height);
        WurmArrayPanel<FlexComponent> buttons = new WurmArrayPanel<>(WurmArrayPanel.DIR_HORIZONTAL, 300, btStop.height);
        buttons.addComponents(btStop, btPause);
        WurmArrayPanel<FlexComponent> panel = new WurmArrayPanel<>(WurmArrayPanel.DIR_VERTICAL, 300, 100);
        for (int i = 0; i < text.length; i++)
            panel.addComponent(text[i] = new WurmLabel(""));
        panel.addComponents(buttons);
        setComponent(panel);
        this.setSize(300, panel.calcHeight() + 25);
    }

    @Override
    public void buttonPressed(WButton button) {
    }

    @Override
    public void buttonClicked(WButton button) {
        if (button == btStop) {
            visible = active = false;
            hud.hideComponent(this);
        } else if (button == btPause) {
            setActive(!active);
        }
    }

    @Override
    public void gameTick() {
        if (active) Hooks.pollFishing();
    }

    public void setActive(boolean value) {
        if (value != active) {
            active = value;
            btPause.setLabel(active ? "Pause" : "Resume", false);
        }
    }

    public boolean isActive() {
        return active;
    }

    public boolean isVisible() {
        return visible;
    }


    public void show() {
        visible = true;
        hud.showComponent(this);
        setPosition((hud.getWidth() - width) / 2, (hud.getHeight() - height) / 2);
    }

    public void setText(int row, String s) {
        text[row - 1].setLabel(s);
    }

    public void clearText() {
        for (WurmLabel row : text)
            row.setLabel("");
    }
}
