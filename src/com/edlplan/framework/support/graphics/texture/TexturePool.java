package com.edlplan.framework.support.graphics.texture;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Build;

import com.edlplan.andengine.TextureHelper;
import com.edlplan.framework.math.Vec2Int;
import com.edlplan.framework.support.graphics.BitmapUtil;
import com.edlplan.framework.utils.interfaces.Consumer;

import org.anddev.andengine.BuildConfig;
import org.anddev.andengine.opengl.texture.ITexture;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.util.GLHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.helper.QualityFileBitmapSource;

public class TexturePool {

    int glMaxWidth;
    BitmapFactory.Options options = new BitmapFactory.Options() {{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            inPremultiplied = true;
        }
    }};
    private File dir;
    private Set<ITexture> createdTextures = new HashSet<>();
    private HashMap<String, TextureRegion> textures = new HashMap<>();
    private int currentPack = 0;
    private int currentX;
    private int currentY;
    private int lineMaxY;
    private int marginX = 2, marginY = 2;
    private int maxW, maxH;

    public TexturePool(File dir) {
        this.dir = dir;
        glMaxWidth = GLHelper.GlMaxTextureWidth;
        if (BuildConfig.DEBUG) System.out.println("GL_MAX_TEXTURE_SIZE = " + glMaxWidth);
        if (glMaxWidth == 0) {
            throw new RuntimeException("glMaxWidth not found");
        }
        glMaxWidth = Math.min(glMaxWidth, 4096);
        maxW = Math.min(400, glMaxWidth / 2);
        maxH = Math.min(400, glMaxWidth / 2);
    }

    public void clear() {
        textures.clear();
        for (ITexture texture : createdTextures) {
            GlobalManager.getInstance().getEngine().getTextureManager().unloadTexture(texture);
        }
        createdTextures.clear();
        currentPack = 0;
        currentX = currentY = lineMaxY = 0;
    }

    public void add(String name) {
        TextureInfo info = loadInfo(name);
        Bitmap bmp = loadBitmap(info);
        info.texture = TextureHelper.createRegion(bmp);
        createdTextures.add(info.texture.getTexture());
        directPut(info.name, info.texture);
        bmp.recycle();
    }

    public void packAll(Iterator<String> collection, Consumer<Bitmap> onPackDrawDone) {
        clear();

        List<TextureInfo> infos = new ArrayList<>();
        for (String n : (Iterable<String>) () -> collection) {
            infos.add(loadInfo(n));
        }
        Collections.sort(infos, (p1, p2) -> {
            if (p1.size.y == p2.size.y) {
                return Float.compare(p1.size.x, p2.size.x);
            } else {
                return Float.compare(p1.size.y, p2.size.y);
            }
        });

        for (TextureInfo t : infos) {
            testAddRaw(t);
        }

        Collections.sort(infos, (p1, p2) -> Integer.compare(p1.pageIndex, p2.pageIndex));

        ListIterator<TextureInfo> iterator = infos.listIterator();

        while (iterator.hasNext()) {
            TextureInfo info = iterator.next();
            if (info.pageIndex != -1) {
                iterator.previous();
                break;
            }
            Bitmap bmp = loadBitmap(info);
            info.texture = TextureHelper.createRegion(bmp);
            createdTextures.add(info.texture.getTexture());
            directPut(info.name, info.texture);
            bmp.recycle();
        }

        Bitmap pack = null;

        if (iterator.hasNext()) {
            int width = glMaxWidth;
            int height = currentPack == 0 ? lineMaxY + 10 : glMaxWidth;
            pack = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
        if (pack == null) {
            return;
        }
        Canvas canvas = new Canvas(pack);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        List<TextureInfo> toLoad = new ArrayList<>();
        Bitmap tmp;
        while (iterator.hasNext()) {
            toLoad.clear();
            pack.eraseColor(Color.argb(0, 0, 0, 0));
            int currentPack = iterator.next().pageIndex;
            iterator.previous();
            while (iterator.hasNext()) {
                TextureInfo info = iterator.next();
                if (info.pageIndex != currentPack) {
                    break;
                }
                toLoad.add(info);
                canvas.drawBitmap(tmp = loadBitmap(info), info.pos.x, info.pos.y, paint);
                tmp.recycle();
            }
            if (onPackDrawDone != null) {
                onPackDrawDone.consume(pack);
            }
            final QualityFileBitmapSource source = new QualityFileBitmapSource(
                    TextureHelper.createFactoryFromBitmap(pack));
            final BitmapTextureAtlas tex = new BitmapTextureAtlas(glMaxWidth, glMaxWidth, TextureOptions.BILINEAR);
            tex.addTextureAtlasSource(source, 0, 0);
            GlobalManager.getInstance().getEngine().getTextureManager().loadTexture(tex);
            createdTextures.add(tex);
            for (TextureInfo info : toLoad) {
                info.texture = new TextureRegion(tex, info.pos.x, info.pos.y, info.size.x, info.size.y);
                info.texture.setTextureRegionBufferManaged(false);
            }
        }
        pack.recycle();

        for (TextureInfo info : infos) {
            directPut(info.name, info.texture);
        }

    }

    private void testAddRaw(TextureInfo raw) {
        if (raw.size.x > maxW || raw.size.y > maxH) {
            raw.single = true;
            raw.pageIndex = -1;
        } else {
            tryAddToPack(raw);
        }
    }

    private void tryAddToPack(TextureInfo raw) {
        if (currentX + raw.size.x + marginX < glMaxWidth) {
            tryAddInLine(raw);
        } else {
            toNextLine();
            tryAddToPack(raw);
        }
    }

    private void tryAddInLine(TextureInfo raw) {
        if (currentY + raw.size.y + marginY < glMaxWidth) {
            raw.single = false;
            raw.pageIndex = currentPack;
            raw.pos = new Vec2Int(currentX, currentY);
            currentX += raw.size.x + marginX;
            lineMaxY = Math.round(Math.max(lineMaxY, currentY + raw.size.y + marginY));
        } else {
            toNewPack();
            tryAddToPack(raw);
        }
    }

    public void toNewPack() {
        currentPack++;
        currentX = 0;
        currentY = 0;
        lineMaxY = 0;
    }

    private void toNextLine() {
        currentX = 0;
        currentY = lineMaxY + marginY;
    }

    private Bitmap loadBitmap(TextureInfo info) {
        Bitmap bmp;
        if (info.err) {
            bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            bmp.setPixel(0, 0, Color.argb(255, 255, 0, 0));
        } else {
            try {
                bmp = BitmapFactory.decodeFile(info.file, options);
            } catch (Exception e) {
                bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                bmp.setPixel(0, 0, Color.argb(255, 255, 0, 0));
            }
        }
        return bmp;
    }

    protected void directPut(String name, TextureRegion region) {
        textures.put(name, region);
    }

    private TextureInfo loadInfo(String name) {
        TextureInfo info = new TextureInfo();
        try {
            info.name = name;
            info.file = new File(dir, name).getAbsolutePath();
            Vec2Int size = BitmapUtil.parseBitmapSize(new File(info.file));
            info.pos = new Vec2Int(0, 0);
            info.size = size;
        } catch (Exception e) {
            e.printStackTrace();
            info.err = true;
            info.pos = new Vec2Int(0, 0);
            info.size = new Vec2Int(1, 1);
        }
        return info;
    }

    public TextureRegion get(String name) {
        TextureRegion region;
        if ((region = textures.get(name)) == null) {
            add(name);
            region = get(name);
        }
        return region;
    }

    private static class TextureInfo {
        public TextureRegion texture;
        public String name;
        public String file;
        public Vec2Int size;
        public Vec2Int pos;
        public boolean err = false;
        public boolean single = true;
        public int pageIndex = -1;
    }

}
