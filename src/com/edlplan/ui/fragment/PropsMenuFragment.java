package com.edlplan.ui.fragment;

import android.animation.Animator;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.BaseAnimationListener;
import com.edlplan.ui.EasingHelper;

import ru.nsu.ccfit.zuev.osu.BeatmapProperties;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.PropertiesLibrary;
import ru.nsu.ccfit.zuev.osu.menu.IPropsMenu;
import ru.nsu.ccfit.zuev.osu.menu.MenuItem;
import ru.nsu.ccfit.zuev.osu.menu.SongMenu;
import ru.nsu.ccfit.zuev.osu.scoring.ScoreLibrary;
import ru.nsu.ccfit.zuev.osuplus.R;

public class PropsMenuFragment extends BaseFragment implements IPropsMenu {

    SongMenu menu;
    MenuItem item;
    BeatmapProperties props;

    private EditText offset;
    private CheckBox isFav;

    public PropsMenuFragment() {
        setDismissOnBackgroundClick(true);
    }

    @Override
    protected int getLayoutID() {
        return R.layout.fragment_props_menu;
    }

    @Override
    protected void onLoadView() {
        offset = findViewById(R.id.offsetBox);
        isFav = findViewById(R.id.addToFav);

        offset.setText(props.getOffset() + "");
        isFav.setChecked(props.isFavorite());

        isFav.setOnCheckedChangeListener((buttonView, isChecked) -> {
            props.setFavorite(isChecked);
            saveProp();
        });

        offset.addTextChangedListener(new TextWatcher() {

            boolean needRest = false;

            int o;

            int pos;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                pos = start;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                try {
                    o = Integer.parseInt(s.toString());
                    needRest = false;
                    if (Math.abs(o) > 250) {
                        o = 250 * (o > 0 ? 1 : -1);
                        needRest = true;
                    }
                    if (needRest) {
                        offset.removeTextChangedListener(this);
                        offset.setText(o + "");
                        offset.setSelection(pos);
                        offset.addTextChangedListener(this);
                    }
                    props.setOffset(o);
                    saveProp();
                } catch (NumberFormatException e) {
                    if (s.length() == 0) {
                        props.setOffset(0);
                        saveProp();
                    }
                    return;
                }
            }
        });

        findViewById(R.id.manageFavButton).setOnClickListener(v -> {
            FavoriteManagerFragment dialog = new FavoriteManagerFragment();
            //TODO : 铺面引用还是全局耦合的，需要分离
            dialog.showToAddToFloder(ScoreLibrary.getTrackDir(GlobalManager.getInstance().getSelectedTrack().getFilename()));
        });

        findViewById(R.id.deleteBeatmap).setOnClickListener(v -> {
            ConfirmDialogFragment confirm = new ConfirmDialogFragment();
            confirm.showForResult(isAccepted -> {
                if (isAccepted) {
                    if (menu != null) {
                        menu.scene.postRunnable(item::delete);
                    }
                    dismiss();
                }
            });
        });

        playOnLoadAnim();
    }

    private void playOnLoadAnim() {
        View body = findViewById(R.id.fullLayout);
        body.setAlpha(0);
        body.setTranslationY(200);
        body.animate().cancel();
        body.animate()
                .translationY(0)
                .alpha(1)
                .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
                .setDuration(150)
                .start();
        playBackgroundHideInAnim(150);
    }

    private void playEndAnim(Runnable action) {
        View body = findViewById(R.id.fullLayout);
        body.animate().cancel();
        body.animate()
                .translationY(200)
                .alpha(0)
                .setDuration(200)
                .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
                .setListener(new BaseAnimationListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (action != null) {
                            action.run();
                        }
                    }
                })
                .start();
        playBackgroundHideOutAnim(200);
    }


    @Override
    public void dismiss() {
        menu.setRank();
        playEndAnim(super::dismiss);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void show(SongMenu menu, MenuItem item) {
        this.menu = menu;
        this.item = item;
        props = PropertiesLibrary.getInstance().getProperties(item.getBeatmap().getPath());
        if (props == null) {
            props = new BeatmapProperties();
        }
        show();
    }

    public void saveProp() {
        PropertiesLibrary.getInstance().setProperties(
                item.getBeatmap().getPath(), props);
        item.setFavorite(props.favorite);
        PropertiesLibrary.getInstance().save();
    }
}
