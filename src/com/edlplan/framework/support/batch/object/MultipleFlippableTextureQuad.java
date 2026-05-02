package com.edlplan.framework.support.batch.object;

import com.edlplan.framework.math.Vec2;

import org.andengine.opengl.texture.region.TextureRegion;

import java.util.List;

public class MultipleFlippableTextureQuad extends FlippableTextureQuad {

    public TextureEntry[] textureEntries;

    public TextureEntry currentEntry;

    public void switchTexture(int id) {
        if (textureEntries.length == 0) {
            throw new RuntimeException();
        }
        id = id % textureEntries.length;
        currentEntry = textureEntries[id];
        texture = currentEntry.texture;
        size.set(currentEntry.size);
        u1 = currentEntry.u1;
        v1 = currentEntry.v1;
        u2 = currentEntry.u2;
        v2 = currentEntry.v2;
    }

    public void initialWithTextureList(List<TextureRegion> textures) {

        textureEntries = new TextureEntry[textures.size()];
        if (textureEntries.length == 0) {
            return;
        }

        for (int i = 0; i < textureEntries.length; i++) {
            TextureRegion texture = textures.get(i);
            TextureEntry entry = new TextureEntry();
            entry.texture = texture;
            entry.size = new Vec2(texture.getWidth(), texture.getHeight());
            entry.u1 = texture.getU();
            entry.u2 = texture.getU2();
            entry.v1 = texture.getV();
            entry.v2 = texture.getV2();
            textureEntries[i] = entry;
        }

        switchTexture(0);
    }

    public void initialWithTextureListWithScale(List<TextureRegion> textures, float globalScale) {

        textureEntries = new TextureEntry[textures.size()];
        if (textureEntries.length == 0) {
            return;
        }

        for (int i = 0; i < textureEntries.length; i++) {
            TextureRegion texture = textures.get(i);
            TextureEntry entry = new TextureEntry();
            entry.texture = texture;
            entry.size = new Vec2(texture.getWidth() * globalScale, texture.getHeight() * globalScale);
            entry.u1 = texture.getU();
            entry.u2 = texture.getU2();
            entry.v1 = texture.getV();
            entry.v2 = texture.getV2();
            textureEntries[i] = entry;
        }

        switchTexture(0);
    }

    public static class TextureEntry {

        public Vec2 size = new Vec2();

        public TextureRegion texture;

        public float u1, v1, u2, v2;

    }

}

