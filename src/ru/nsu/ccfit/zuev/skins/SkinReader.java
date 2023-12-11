package ru.nsu.ccfit.zuev.skins;

public abstract class SkinReader {

    public void loadSkin() {
        OsuSkin.get().reset();
        loadSkinBase();
    }

    protected abstract void loadSkinBase();

    protected void putLayout(String name, SkinLayout layout) {
        OsuSkin.get().layoutData.put(name, layout);
    }

}
