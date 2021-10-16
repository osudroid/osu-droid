package com.edlplan.ui;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.edlplan.framework.math.FMath;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import ru.nsu.ccfit.zuev.osu.Config;

public class TriangleDrawable extends Drawable {


    private final float m = (float) (Math.sqrt(3) / 2);
    int spawnClock = 0;
    int spawnCost = 120;
    private LinkedList<Triangle> triangles = new LinkedList<>();
    private boolean spawnNewTriangles = true;
    private Paint paint = new Paint();
    private Random random = new Random();
    private Path path = new Path();
    private int[] colors = new int[]{
            0xFFF7E67A,
            0xFFEE8100,
            0xFF74C684,
            0xFFF8558C,
            0xFF5245F7
    };
    private float width, height;
    private long time = -1;
    private boolean preSpawnTriangles;
    private PosXDistribution xDistribution = null;
    private float defaultXDistributionScale = 10;
    private float baseSpeed = 15;
    private float edgeClampRate = 0.2f;
    private boolean freeze = false;

    public TriangleDrawable() {
        this(true);
    }

    public TriangleDrawable(boolean preSpawnTriangles) {
        this.preSpawnTriangles = preSpawnTriangles;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        onDraw(canvas);
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

    public void setFreeze(boolean freeze) {
        this.freeze = freeze;
    }

    public void setXDistribution(PosXDistribution xDistribution) {
        this.xDistribution = xDistribution;
        this.time = -1;
        this.triangles.clear();
    }

    public void setDefaultXDistributionScale(float defaultXDistributionScale) {
        this.defaultXDistributionScale = defaultXDistributionScale;
    }

    public void setEdgeClampRate(float edgeClampRate) {
        this.edgeClampRate = edgeClampRate;
    }

    public float getEdgeClampRate() {
        return edgeClampRate;
    }

    protected void onDraw(Canvas canvas) {
        if(!Config.isTrianglesAnimation()) {
            return;
        }

        width = getBounds().width();
        height = getBounds().height();
        paint.setColor(0xFFFFFFFF);
        if (time == -1) {
            time = System.currentTimeMillis();
            if (this.preSpawnTriangles) {
                for (int i = 0; i < 200; i++) {
                    update(36);
                }
            }
            return;
        }
        int dt = (int) (System.currentTimeMillis() - time);
        time += dt;
        if (!freeze) {
            update(dt * 2);
        }

        for (Triangle triangle : triangles) {
            path.rewind();
            path.moveTo(triangle.center.x, triangle.center.y - triangle.size);
            path.rLineTo(triangle.size * m, triangle.size * 1.5f);
            path.rLineTo(-triangle.size * m * 2, 0);
            path.close();
            canvas.drawPath(path, triangle.paint);
        }
    }

    private float nextAlpha() {
        return (float) Math.max(1 - 1.5 * Math.abs(random.nextGaussian()), 0.1);
    }

    private float defaultX() {
        // return 2 * random.nextGaussian();
        return (float) (2f / (1 + Math.exp((Math.random() * 2 - 1) * defaultXDistributionScale)) - 1);
    }

    private PointF nextPos() {
        return new PointF(
                FMath.clamp(width / 2 * (1 +
                        (xDistribution != null ? xDistribution.generate() : defaultX())), 0, width),
                height
        );
    }

    private float nextSize() {
        //return random.nextInt(200);
        //return (float) (Math.min(width, height) * 0.8 * (Math.min(1, Math.abs(random.nextGaussian())) * 0.8 + 0.2));
        return (float) (Math.pow(Math.random(), 2) * 200);
    }

    private void spawnOneTriangle() {
        Triangle triangle = new Triangle();
        triangle.color = colors[random.nextInt(colors.length)];
        triangle.alpha = nextAlpha();
        triangle.center = nextPos();

        triangle.speed = (float) (15 * (Math.abs(random.nextGaussian()) * 0.4 + 0.6));
        triangle.size = nextSize();
        triangle.center.y += triangle.size;
        triangle.lifeTime = 8000;
        triangle.center.x = FMath.clamp(triangle.center.x, triangle.size * edgeClampRate, width - triangle.size * edgeClampRate);
        triangle.fixBound();
        if (triangle.size < 20 || triangle.lifeTime < 100) {
            return;
        }
        triangle.paint = new Paint();
        triangle.paint.setColor(triangle.color);
        triangles.add(triangle);
    }

    private void doSpawnNewTriangles(int dt) {
        spawnClock += dt;
        while (spawnClock > spawnCost) {
            /*int count = random.nextInt(2);
            for (int i = 0; i < count; i++) {
                spawnOneTriangle();
            }*/
            spawnOneTriangle();
            spawnClock -= spawnCost;
        }

    }

    private void update(int dt) {
        if (spawnNewTriangles) {
            doSpawnNewTriangles(dt);
        }

        Iterator<Triangle> iterator = triangles.iterator();
        while (iterator.hasNext()) {
            Triangle triangle = iterator.next();
            triangle.update(dt);
            if (triangle.updateAlpha < 0.005) {
                iterator.remove();
            }
        }
    }

    public interface PosXDistribution {
        float generate();
    }


    public class Triangle {

        public Paint paint;

        public PointF center;

        public float size;

        public float alpha;

        public float updateAlpha;

        public float speed;

        public int color;

        public int lifeTime;

        public int passTime;

        public void fixBound() {
            /*size = Math.min(size, Math.min(center.x, width - center.x));
            size = Math.min(center.y, size);
            size = Math.min(2 * (height - center.y), size);
            lifeTime = (int) Math.min(lifeTime, (center.y - size) / speed * 1000);*/
        }

        public void update(int dt) {
            passTime += dt;
            updateAlpha = center.y / height * alpha;
            center.y -= dt * speed / 1000;
            paint.setAlpha(Math.min(255, (int) (updateAlpha * 255)));
        }

    }
}
