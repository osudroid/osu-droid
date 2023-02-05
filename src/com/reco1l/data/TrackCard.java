package com.reco1l.data;

import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.reco1l.utils.helpers.BeatmapHelper;
import com.reco1l.view.BadgeTextView;
import com.reco1l.view.RoundLayout;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osuplus.R;

public final class TrackCard {

    private final RoundLayout mRootView;

    private final TextView
            mTitle,
            mArtist,
            mDifficulty;

    private final BadgeTextView
            mBpmText,
            mLengthText,
            mComboText,
            mCirclesText,
            mSlidersText,
            mSpinnersText,
            mARText,
            mODText,
            mCSText,
            mHPText,
            mStarsText,
            mMapper;

    private final ImageView mBackground;

    private TrackAttributeSet mAttrs;

    //--------------------------------------------------------------------------------------------//

    public TrackCard(RoundLayout pRoot) {
        mRootView = pRoot;

        mBpmText = pRoot.findViewById(R.id.bp_bpm);
        mLengthText = pRoot.findViewById(R.id.bp_length);
        mComboText = pRoot.findViewById(R.id.bp_combo);
        mCirclesText = pRoot.findViewById(R.id.bp_circles);
        mSlidersText = pRoot.findViewById(R.id.bp_sliders);
        mSpinnersText = pRoot.findViewById(R.id.bp_spinners);
        mARText = pRoot.findViewById(R.id.bp_ar);
        mODText = pRoot.findViewById(R.id.bp_od);
        mCSText = pRoot.findViewById(R.id.bp_cs);
        mHPText = pRoot.findViewById(R.id.bp_hp);
        mStarsText = pRoot.findViewById(R.id.bp_stars);

        mTitle = pRoot.findViewById(R.id.bp_title);
        mArtist = pRoot.findViewById(R.id.bp_artist);
        mMapper = pRoot.findViewById(R.id.bp_mapper);
        mBackground = pRoot.findViewById(R.id.bp_songBackground);
        mDifficulty = pRoot.findViewById(R.id.bp_difficulty);
    }

    //--------------------------------------------------------------------------------------------//

    private void changeLabels(TrackInfo pTrack) {

        mTitle.setText(BeatmapHelper.getTitle(pTrack));
        mArtist.setText("by " + BeatmapHelper.getArtist(pTrack));
        mMapper.setText(pTrack.getCreator());
        mDifficulty.setText(pTrack.getMode());

        if (pTrack.getBackground() != null) {
            mBackground.setVisibility(View.VISIBLE);
            mBackground.setImageDrawable(BeatmapHelper.getBackground(pTrack));
        } else {
            mBackground.setVisibility(View.INVISIBLE);
        }
    }

    private void createAttributes(TrackInfo pTrack) {
        mAttrs = new TrackAttributeSet(pTrack);

        mAttrs.forEach(a -> {
            if (a.getValue() instanceof Float) {
                a.setFormatter(v -> "" + GameHelper.Round((Float) v, 2));
            }
            if (a.getValue() instanceof Integer) {
                a.setFormatter(v -> "" + NumberFormat.getNumberInstance(Locale.US).format(v));
            }
            if (a.getValue() instanceof Long) {
                a.setFormatter(v -> {
                    SimpleDateFormat sdf;

                    if ((long) a.getValue() > 3600 * 1000) {
                        sdf = new SimpleDateFormat("HH:mm:ss");
                    } else {
                        sdf = new SimpleDateFormat("mm:ss");
                    }
                    return sdf.format(a.getValue());
                });
            }
        });
    }

    private void handleColoring(float pStars) {
        int darkerColor = BeatmapHelper.Palette.getDarkerColor(pStars);
        int textColor = BeatmapHelper.Palette.getTextColor(pStars);
        int color = BeatmapHelper.Palette.getColor(pStars);

        mStarsText.setBackground(new ColorDrawable(color));
        mStarsText.setTextColor(textColor);
        mMapper.setBackground(new ColorDrawable(darkerColor));
        mRootView.setBackground(new ColorDrawable(darkerColor));
    }

    //--------------------------------------------------------------------------------------------//

    public void onMusicChange(@Nullable TrackInfo pNewTrack) {
        if (pNewTrack == null) {
            return;
        }
        changeLabels(pNewTrack);
        createAttributes(pNewTrack);

        mAttrs.get(TrackAttribute.BPM_MAX).setView(mBpmText.getTextView());
        mAttrs.get(TrackAttribute.LENGTH).setView(mLengthText.getTextView());

        mAttrs.get(TrackAttribute.COMBO).setView(mComboText.getTextView());
        mAttrs.get(TrackAttribute.CIRCLES).setView(mCirclesText.getTextView());
        mAttrs.get(TrackAttribute.SLIDERS).setView(mSlidersText.getTextView());
        mAttrs.get(TrackAttribute.SPINNERS).setView(mSpinnersText.getTextView());

        mAttrs.get(TrackAttribute.AR).setView(mARText.getTextView());
        mAttrs.get(TrackAttribute.OD).setView(mODText.getTextView());
        mAttrs.get(TrackAttribute.CS).setView(mCSText.getTextView());
        mAttrs.get(TrackAttribute.HP).setView(mHPText.getTextView());

        mAttrs.get(TrackAttribute.STARS)
                .setView(mStarsText.getTextView())
                .setOnChange(v -> handleColoring((Float) v));

        handleModifications();
    }

    public void handleModifications() {
        if (mAttrs != null) {
            mAttrs.handleValues();
        }
    }
}
