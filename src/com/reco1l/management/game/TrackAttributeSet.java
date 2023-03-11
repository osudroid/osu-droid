package com.reco1l.management.game;

import com.reco1l.Game;
import com.reco1l.management.modding.ModAcronyms;
import com.reco1l.framework.execution.Async;

import java.util.EnumSet;
import java.util.function.Consumer;

import main.osu.TrackInfo;
import main.osu.game.GameHelper;
import main.osu.game.mods.GameMod;
import main.osu.helper.DifficultyReCalculator;

public class TrackAttributeSet implements ModAcronyms {

    private final TrackInfo mTrack;
    private final TrackAttribute<Number>[] mAttributes;

    public TrackAttributeSet(TrackInfo pTrack) {
        mTrack = pTrack;

        //noinspection unchecked
        mAttributes = new TrackAttribute[] {
                new TrackAttribute<>(pTrack::getBpmMin),
                new TrackAttribute<>(pTrack::getBpmMax),
                new TrackAttribute<>(pTrack::getMusicLength),
                new TrackAttribute<>(pTrack::getMaxCombo),
                new TrackAttribute<>(pTrack::getHitCircleCount),
                new TrackAttribute<>(pTrack::getSliderCount),
                new TrackAttribute<>(pTrack::getSpinnerCount),
                new TrackAttribute<>(pTrack::getApproachRate, 13f),
                new TrackAttribute<>(pTrack::getOverallDifficulty, 10f),
                new TrackAttribute<>(pTrack::getCircleSize, 15f),
                new TrackAttribute<>(pTrack::getHpDrain, 10f),
                new TrackAttribute<>(pTrack::getDifficulty),
        };
    }

    public TrackAttribute<Number> get(int pIndex) {
        if (pIndex > mAttributes.length - 1 || pIndex < 0) {
            return null;
        }
        return mAttributes[pIndex];
    }

    @SuppressWarnings("unchecked")
    public <T extends Number> T getValue(int pAttrIndex) {
        if (get(pAttrIndex) != null) {
            return (T) get(pAttrIndex).getValue();
        }
        return null;
    }

    public void forEach(Consumer<TrackAttribute<Number>> consumer) {
        for (TrackAttribute<Number> attr : mAttributes) {
            consumer.accept(attr);
        }
    }

    // TODO [TrackAttributeSet] Rework mod system to avoid this shit
    public void handleValues() {
        forEach(TrackAttribute::reset);

        EnumSet<GameMod> mods = Game.modManager.getMods();

        if (mods.contains(EZ)) {
            get(TrackAttribute.AR).opt(v -> ((float) v) * 0.5f);
            get(TrackAttribute.OD).opt(v -> ((float) v) * 0.5f);
            get(TrackAttribute.HP).opt(v -> ((float) v) * 0.5f);
            get(TrackAttribute.CS).opt(v -> ((float) v) - 1f);
        }

        if (mods.contains(HR)) {
            get(TrackAttribute.AR).opt(v ->
                    Math.min(((float) v) * 1.4f, 10)
            );
            get(TrackAttribute.OD).opt(v -> (float) v * 1.4f);
            get(TrackAttribute.HP).opt(v -> (float) v * 1.4f);
            get(TrackAttribute.CS).opt(v -> (float) v + 1f);
        }

        if (mods.contains(REZ)) {
            if (mods.contains(EZ)) {
                get(TrackAttribute.AR).opt(v -> ((float) v) * 2f - 0.5f);
            }

            get(TrackAttribute.AR).opt(v -> ((float) v) - 0.5f);

            if (Game.modManager.isCustomSpeed()) {
                get(TrackAttribute.AR).opt(v ->
                        (float) v - Game.modManager.getSpeed() - 1.0f
                );
            }
            else if (mods.contains(DT) || mods.contains(NC)) {
                get(TrackAttribute.AR).opt(v -> (float) v - 0.5f);
            }

            get(TrackAttribute.OD).opt(v -> (float) v * 0.5f);
            get(TrackAttribute.HP).opt(v -> (float) v * 0.5f);
            get(TrackAttribute.CS).opt(v -> (float) v - 1f);
        }

        if (mods.contains(SC)) {
            get(TrackAttribute.CS).opt(v -> (float) v + 4f);
        }

        if (Game.modManager.isCustomSpeed()) {
            float speed = Game.modManager.getSpeed();

            get(TrackAttribute.BPM_MAX).opt(v -> (float) v * speed);
            get(TrackAttribute.LENGTH).opt(v -> (long) ((long) v / speed));

            get(TrackAttribute.AR).opt(v ->
                    GameHelper.ms2ar(GameHelper.ar2ms((float) v) / speed)
            );
            get(TrackAttribute.OD).opt(v ->
                    GameHelper.ms2od(GameHelper.od2ms((float) v) / speed)
            );

        } else if (mods.contains(DT) || mods.contains(NC)) {

            get(TrackAttribute.BPM_MAX).opt(v -> (float) v * 1.5f);
            get(TrackAttribute.LENGTH).opt(v -> (long) ((long) v / 1.5f));

            get(TrackAttribute.AR).opt(v ->
                    GameHelper.ms2ar(GameHelper.ar2ms((float) v) * 2 / 3)
            );
            get(TrackAttribute.OD).opt(v ->
                    GameHelper.ms2od(GameHelper.od2ms((float) v) * 2 / 3)
            );

        } else if (mods.contains(HT)) {

            get(TrackAttribute.BPM_MAX).opt(v -> (float) v * 0.7f);
            get(TrackAttribute.LENGTH).opt(v -> (long) ((long) v / 0.7f));

            get(TrackAttribute.AR).opt(v ->
                    GameHelper.ms2ar(GameHelper.ar2ms((float) v) * 4 / 3)
            );
            get(TrackAttribute.OD).opt(v ->
                    GameHelper.ms2od(GameHelper.od2ms((float) v) * 4 / 3)
            );
        }

        if (Game.modManager.isCustomAR()) {
            get(TrackAttribute.AR).opt(v -> Game.modManager.getCustomAR());
        }

        Async.run(() -> {
            DifficultyReCalculator drc = new DifficultyReCalculator();

            get(TrackAttribute.STARS).opt(v ->
                    drc.recalculateStar(mTrack, getValue(TrackAttribute.CS), Game.modManager.getSpeed())
            );
        });
    }
}
