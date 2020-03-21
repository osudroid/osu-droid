package com.edlplan.osu.support.object;

import android.graphics.PointF;

import com.edlplan.framework.math.Vec2;
import com.edlplan.framework.math.line.LinePath;
import com.edlplan.framework.utils.advance.StringSplitter;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;

public class SupportSliderPath {

    protected static StdPath parseStdPath(Vec2 startPoint, String s) {
        StdPath p = new StdPath();
        p.addControlPoint(startPoint);
        StringSplitter spl = new StringSplitter(s, "\\|");
        p.setType(StdPath.Type.forName(spl.next()));
        while (spl.hasNext()) {
            p.addControlPoint(parseVec2FF(spl.next()));
        }
        return p;
    }

    protected static Vec2 parseVec2FF(String s) {
        String[] sp = s.split(":");
        return new Vec2(Float.parseFloat(sp[0]), Float.parseFloat(sp[1]));
    }

    public static LinePath parseToLinePath(Vec2 s, String p) {
        StdPath path = parseStdPath(s, p);
        return (new StdSliderPathMaker(path)).calculatePath();
    }

    public static GameHelper.SliderPath parseDroidLinePath(PointF s, String p, float l) {
        LinePath path = parseToLinePath(new Vec2(s.x, s.y), p);
        path.measure();
        path.bufferLength(l);
        path = path.cutPath(0, path.getMeasurer().maxLength()).fitToLinePath();
        path.measure();
        ArrayList<PointF> points = new ArrayList<>(path.size());
        for (int i = 0; i < path.size(); i++) {
            Vec2 v = path.get(i);
            points.add(Utils.realToTrackCoords(new PointF(v.x, v.y)));
        }
        GameHelper.SliderPath pp = new GameHelper.SliderPath();
        pp.points = points;
        pp.length = new ArrayList<>(points.size());
        float len = 0;
        for (int i = 1; i < points.size(); i++) {
            len += Vec2.length(
                    points.get(i - 1).x - points.get(i).x,
                    points.get(i - 1).y - points.get(i).y
            );
            pp.length.add(len);
        }
        return pp;
    }

}
