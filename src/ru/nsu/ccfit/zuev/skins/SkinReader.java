package ru.nsu.ccfit.zuev.skins;

import org.json.JSONObject;

public abstract class SkinReader {

    public void loadSkin() {
        OsuSkin.get().reset();
        loadSkinBase();
    }

    protected abstract void loadSkinBase();
    protected abstract void loadComboColorSetting();
    protected abstract void loadSlider();
    protected abstract void loadUtils();
    protected abstract void loadLayout();
    protected abstract void loadColor();
    protected abstract void loadCursor();
    protected void putLayout(String name, SkinLayout layout) {
        OsuSkin.get().layoutData.put(name, layout);
    }
}
