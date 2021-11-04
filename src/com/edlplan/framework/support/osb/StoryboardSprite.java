package com.edlplan.framework.support.osb;

import android.graphics.Color;

import com.edlplan.andengine.TextureHelper;
import com.edlplan.edlosbsupport.OsuStoryboard;
import com.edlplan.edlosbsupport.OsuStoryboardLayer;
import com.edlplan.edlosbsupport.elements.IStoryboardElement;
import com.edlplan.edlosbsupport.elements.StoryboardAnimationSprite;
import com.edlplan.edlosbsupport.parser.OsbFileParser;
import com.edlplan.edlosbsupport.player.OsbPlayer;
import com.edlplan.framework.math.Anchor;
import com.edlplan.framework.math.Vec2;
import com.edlplan.framework.support.ProxySprite;
import com.edlplan.framework.support.SupportSprite;
import com.edlplan.framework.support.batch.BatchEngine;
import com.edlplan.framework.support.batch.object.TextureQuad;
import com.edlplan.framework.support.batch.object.TextureQuadBatch;
import com.edlplan.framework.support.graphics.BaseCanvas;
import com.edlplan.framework.support.graphics.texture.TexturePool;
import com.edlplan.framework.support.util.Tracker;
import com.edlplan.framework.utils.functionality.SmartIterator;

import org.anddev.andengine.opengl.texture.region.TextureRegion;

import java.io.File;
import java.util.HashMap;

import ru.nsu.ccfit.zuev.osu.helper.FileUtils;

public class StoryboardSprite extends SupportSprite {

    OsbContext context = new OsbContext();
    OsuStoryboard storyboard;
    OsbPlayer osbPlayer;
    TextureQuad backgroundQuad;
    TextureQuad forgroundQuad;
    boolean replaceBackground;
    String loadedOsu;
    private double time;
    private boolean needUpdate = false;

    public StoryboardSprite(float width, float height) {
        super(width, height);
    }

    private static HashMap<String, Integer> countTextureUsedTimes(OsuStoryboard storyboard) {
        HashMap<String, Integer> textures = new HashMap<>();
        Integer tmp;
        String tmps;
        for (OsuStoryboardLayer layer : storyboard.layers) {
            if (layer != null) {
                for (IStoryboardElement element : layer.elements) {
                    if (element instanceof StoryboardAnimationSprite) {
                        StoryboardAnimationSprite as = (StoryboardAnimationSprite) element;
                        for (int i = 0; i < as.frameCount; i++) {
                            if ((tmp = textures.get(tmps = as.buildPath(i))) == null) {
                                textures.put(tmps, 1);
                                continue;
                            }
                            textures.put(tmps, tmp + 1);
                        }
                    } else if (element instanceof com.edlplan.edlosbsupport.elements.StoryboardSprite) {
                        if ((tmp = textures.get(tmps = ((com.edlplan.edlosbsupport.elements.StoryboardSprite) element).spriteFilename)) == null) {
                            textures.put(tmps, 1);
                            continue;
                        }
                        textures.put(tmps, tmp + 1);
                    }
                }
            }
        }
        return textures;
    }

    public TexturePool getLoadedPool() {
        return context.texturePool;
    }

    public void setBrightness(float brightness) {
        TextureRegion region = TextureHelper.create1xRegion(Color.argb(255, 0, 0, 0));
        backgroundQuad = new TextureQuad();
        backgroundQuad.anchor = Anchor.TopLeft;
        backgroundQuad.setTextureAndSize(region);
        forgroundQuad = new TextureQuad();
        forgroundQuad.anchor = Anchor.TopLeft;
        forgroundQuad.setTextureAndSize(region);
        forgroundQuad.alpha.value = 1 - brightness;
    }

    public void updateTime(double time) {
        if (Math.abs(this.time - time) > 10) {
            this.time = time;
            if (osbPlayer != null) {
                osbPlayer.update(time);
            }
        }
    }

    public boolean isStoryboardAvailable() {
        return storyboard != null;
    }

    public void setOverlayDrawProxy(ProxySprite proxy) {
        proxy.setDrawProxy(this::drawOverlay);
    }

    public void drawOverlay(BaseCanvas canvas) {
        if (storyboard == null) {
            return;
        }

        canvas.getBlendSetting().save();
        canvas.save();
        float scale = Math.max(640 / canvas.getWidth(), 480 / canvas.getHeight());
        Vec2 startOffset = new Vec2(canvas.getWidth() / 2, canvas.getHeight() / 2)
                .minus(640 * 0.5f / scale, 480 * 0.5f / scale);

        canvas.translate(startOffset.x, startOffset.y).expendAxis(scale);

        if (context.engines != null) {
            for (LayerRenderEngine engine : context.engines) {
                if (engine != null && engine.getLayer() == com.edlplan.edlosbsupport.elements.StoryboardSprite.Layer.Overlay) {
                    engine.draw(canvas);
                }
            }
        }

        canvas.restore();
        canvas.getBlendSetting().restore();
    }

    @Override
    protected void onSupportDraw(BaseCanvas canvas) {
        super.onSupportDraw(canvas);

        if (storyboard == null) {
            return;
        }

        if (replaceBackground) {
            if (backgroundQuad != null) {
                backgroundQuad.size.set(canvas.getWidth(), canvas.getHeight());
                TextureQuadBatch.getDefaultBatch().add(backgroundQuad);
                BatchEngine.flush();
            }
        } else {
            if (backgroundQuad == null) {
                backgroundQuad = new TextureQuad();
            }
            backgroundQuad.anchor = Anchor.Center;
            backgroundQuad.setTextureAndSize(context.texturePool.get(storyboard.backgroundFile));
            backgroundQuad.position.set(canvas.getWidth() / 2, canvas.getHeight() / 2);
            backgroundQuad.enableScale().scale.set(
                    Math.min(
                            canvas.getWidth() / backgroundQuad.size.x,
                            canvas.getHeight() / backgroundQuad.size.y));
            TextureQuadBatch.getDefaultBatch().add(backgroundQuad);
        }

        canvas.getBlendSetting().save();
        canvas.save();
        float scale = Math.max(640 / canvas.getWidth(), 480 / canvas.getHeight());
        Vec2 startOffset = new Vec2(canvas.getWidth() / 2, canvas.getHeight() / 2)
                .minus(640 * 0.5f / scale, 480 * 0.5f / scale);

        canvas.translate(startOffset.x, startOffset.y).expendAxis(scale);

        if (context.engines != null) {
            for (LayerRenderEngine engine : context.engines) {
                if (engine != null && engine.getLayer() != com.edlplan.edlosbsupport.elements.StoryboardSprite.Layer.Overlay) {
                    engine.draw(canvas);
                }
            }
        }

        canvas.restore();
        canvas.getBlendSetting().restore();

        if (forgroundQuad != null) {
            forgroundQuad.size.set(canvas.getWidth(), canvas.getHeight());
            TextureQuadBatch.getDefaultBatch().add(forgroundQuad);
            BatchEngine.flush();
        }
    }

    private File findOsb(String osuFile) {
        File dir = new File(osuFile);
        dir = dir.getParentFile();
        File[] fs = FileUtils.listFiles(dir, ".osb");
        if (fs.length > 0) {
            return fs[0];
        } else {
            return null;
        }
    }

    private void loadOsb(String osuFile) {
        File file = findOsb(osuFile);
        if (file == null) {
            return;
        }

        OsbFileParser parser = new OsbFileParser(
                file,
                null);

        Tracker.createTmpNode("ParseOsb").wrap(() -> {
            try {
                parser.parse();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).then(System.out::println);

        storyboard = parser.getBaseParser().getStoryboard();
    }

    private void loadOsu(String osuFile) {
        OsbFileParser parser = new OsbFileParser(new File(osuFile), null);
        Tracker.createTmpNode("ParseOsu").wrap(() -> {
            try {
                parser.parse();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).then(System.out::println);

        OsuStoryboard osustoryboard = parser.getBaseParser().getStoryboard();

        if (storyboard == null) {
            boolean empty = true;
            for (OsuStoryboardLayer layer : osustoryboard.layers) {
                if (layer != null) {
                    empty = false;
                    break;
                }
            }
            if (empty) {
                return;
            }
            storyboard = osustoryboard;
        } else {
            storyboard.appendStoryboard(osustoryboard);
        }
    }

    private void loadFromCache() {

        context.engines = new LayerRenderEngine[com.edlplan.edlosbsupport.elements.StoryboardSprite.Layer.values().length];
        for (int i = 0; i < context.engines.length; i++) {
            context.engines[i] = new LayerRenderEngine(com.edlplan.edlosbsupport.elements.StoryboardSprite.Layer.values()[i]);
        }

        if (storyboard == null) {
            return;
        }

        osbPlayer = new OsbPlayer(s -> {
            if (s.getClass() == com.edlplan.edlosbsupport.elements.StoryboardSprite.class) {
                return new EGFStoryboardSprite(context);
            } else {
                return new EGFStoryboardAnimationSprite(context);
            }
        });

        Tracker.createTmpNode("LoadPlayer").wrap(() -> {
            osbPlayer.loadStoryboard(storyboard);
        }).then(System.out::println);
    }

    public void loadStoryboard(String osuFile) {
        System.out.println(this + " load storyboard from " + osuFile);
        if (osuFile.equals(loadedOsu)) {
            System.out.println("load storyboard from cache");
            loadFromCache();
            return;
        }
        loadedOsu = osuFile;

        releaseStoryboard();

        loadedOsu = osuFile;

        File osu = new File(osuFile);
        File dir = osu.getParentFile();
        TexturePool pool = new TexturePool(dir);

        context.texturePool = pool;
        context.engines = new LayerRenderEngine[com.edlplan.edlosbsupport.elements.StoryboardSprite.Layer.values().length];
        for (int i = 0; i < context.engines.length; i++) {
            context.engines[i] = new LayerRenderEngine(com.edlplan.edlosbsupport.elements.StoryboardSprite.Layer.values()[i]);
        }

        loadOsb(osuFile);
        loadOsu(osuFile);

        if (storyboard == null) {
            return;
        }

        replaceBackground = storyboard.needReplaceBackground();
        Tracker.createTmpNode("PackTextures").wrap(() -> {
            //Set<String> all = new HashSet<>();// = storyboard.getAllNeededTextures();
            HashMap<String, Integer> counted = countTextureUsedTimes(storyboard);
            if ((!replaceBackground) && storyboard.backgroundFile != null) {
                counted.put(
                        storyboard.backgroundFile,
                        counted.get(storyboard.backgroundFile) == null ?
                                1 : (counted.get(storyboard.backgroundFile) + 1));
            }

            SmartIterator<String> allToPack = SmartIterator.wrap(counted.keySet().iterator())
                    .applyFilter(s -> counted.get(s) >= 15);
            pool.packAll(allToPack, null);

            allToPack = SmartIterator.wrap(counted.keySet().iterator())
                    .applyFilter(s -> counted.get(s) < 15);
            while (allToPack.hasNext()) {
                pool.add(allToPack.next());
            }
        }).then(System.out::println);


        osbPlayer = new OsbPlayer(s -> {
            if (s.getClass() == com.edlplan.edlosbsupport.elements.StoryboardSprite.class) {
                return new EGFStoryboardSprite(context);
            } else {
                return new EGFStoryboardAnimationSprite(context);
            }
        });

        Tracker.createTmpNode("LoadPlayer").wrap(() -> {
            osbPlayer.loadStoryboard(storyboard);
        }).then(System.out::println);

    }

    public void releaseStoryboard() {
        if (context.texturePool != null) {
            context.texturePool.clear();
            context.texturePool = null;
        }
        if (storyboard != null) {
            storyboard.clear();
        }
        if (osbPlayer != null) {
            osbPlayer = null;
        }
        loadedOsu = null;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        releaseStoryboard();
    }
}
