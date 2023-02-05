package com.reco1l.data.adapters;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.reco1l.data.BaseAdapter;
import com.reco1l.data.BaseViewHolder;
import com.reco1l.global.Game;
import com.reco1l.tables.NotificationTable;
import com.reco1l.ui.custom.LoaderFragment;
import com.reco1l.utils.Animation;
import com.reco1l.utils.TouchHandler;
import com.reco1l.utils.execution.AsyncTask;
import com.reco1l.view.RoundLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osuplus.R;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class SkinListAdapter extends BaseAdapter<SkinListAdapter.ViewHolder, String> {

    private final SharedPreferences mPreferences;

    //--------------------------------------------------------------------------------------------//

    public SkinListAdapter() {
        super(new ArrayList<>());

        Set<String> keys = Config.getSkins().keySet();

        getData().add("Default");
        getData().addAll(keys);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(context());
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getItemLayout() {
        return R.layout.item_skin;
    }

    @Override
    protected ViewHolder getViewHolder(View pRootView) {
        return new ViewHolder(pRootView);
    }

    @Override
    protected void onHolderAssignment(ViewHolder holder, int i) {
        if (i > 0) {
            holder.mSkinPath = Config.getSkins().get(getData().get(i));
        } else {
            holder.mSkinPath = Config.getSkinTopPath();
        }
    }

    //--------------------------------------------------------------------------------------------//

    protected class ViewHolder extends BaseViewHolder<String> {

        private final RoundLayout mBody;

        private final TextView
                mName,
                mPath;

        private final ImageView
                mNumber,
                mHitCircle,
                mHitCircleOverlay;

        private final ColorDrawable mBackground;

        private AsyncTask mLoadTask;
        private String mSkinPath;

        //----------------------------------------------------------------------------------------//

        public ViewHolder(@NonNull View root) {
            super(root);

            mBody = root.findViewById(R.id.sl_body);
            mName = root.findViewById(R.id.sl_name);
            mPath = root.findViewById(R.id.sl_path);

            mNumber = root.findViewById(R.id.sl_number);
            mHitCircle = root.findViewById(R.id.sl_hitCircle);
            mHitCircleOverlay = root.findViewById(R.id.sl_hitCircleOverlay);

            new TouchHandler(() -> {
                if (select()) {
                    if (Game.globalManager.getSkinNow().equals(mSkinPath)) {
                        return;
                    }

                    mPreferences.edit()
                            .putString("skinPath", mSkinPath)
                            .commit();

                    Game.globalManager.setSkinNow(Config.getSkinPath());

                    new AsyncTask() {

                        private LoaderFragment mFragment;

                        public void run() {
                            mFragment = new LoaderFragment();
                            mFragment.show(true);

                            Game.skinManager.clearSkin();
                            Game.resourcesManager.loadSkin(mSkinPath);
                            Game.engine.getTextureManager().reloadTextures();

                            mFragment.close();
                        }

                        public void onComplete() {
                        }
                    }.execute();
                }
            }).apply(mBody);

            mBackground = new ColorDrawable(0xFF242424);
        }

        @Override
        protected void onBind(String item, int position) {
            mName.setText(item);
            mPath.setText(mSkinPath);
            mPath.setVisibility(View.VISIBLE);

            if (position == 0) {
                mPath.setVisibility(View.GONE);

                mNumber.setImageBitmap(Game.bitmapManager.get("default-1"));
                mHitCircle.setImageBitmap(Game.bitmapManager.get("hitcircle"));
                mHitCircleOverlay.setImageBitmap(Game.bitmapManager.get("hitcircleoverlay"));

                mHitCircle.setColorFilter(0xFF5E5E89, Mode.MULTIPLY);
                return;
            }

            if (mLoadTask != null && !mLoadTask.isShutdown()) {
                mLoadTask.cancel(true);
            }

            mLoadTask = new AsyncTask() {
                public void run() {
                    loadTextures();
                    loadJsonParameters();
                }
            };
            mLoadTask.execute();
        }

        private void loadTextures() {
            Drawable number = Drawable.createFromPath(mSkinPath + "/default-1.png");
            Drawable circle = Drawable.createFromPath(mSkinPath + "/hitcircle.png");
            Drawable overlay = Drawable.createFromPath(mSkinPath + "/hitcircleoverlay.png");

            Game.activity.runOnUiThread(() -> {
                mNumber.setImageDrawable(number);
                mHitCircle.setImageDrawable(circle);
                mHitCircleOverlay.setImageDrawable(overlay);
            });
        }

        private void loadJsonParameters() {
            File file = new File(mSkinPath, "skin.json");
            if (!file.exists()) {
                return;
            }

            JSONObject data;
            try {
                data = new JSONObject(OsuSkin.readFull(file));
            } catch (Exception e) {
                NotificationTable.exception(e);
                return;
            }

            JSONObject colorData = data.optJSONObject("ComboColor");
            if (colorData != null) {
                JSONArray colors = colorData.optJSONArray("colors");
                int tint = Color.WHITE;

                if (colors != null && colors.length() > 0) {
                    String hex = colors.optString(0, null);

                    if (hex != null) {
                        tint = RGBColor.hex2Rgb(hex).toInt();
                    }
                }

                int t = tint;
                Game.activity.runOnUiThread(() -> mHitCircle.setColorFilter(t, Mode.MULTIPLY));
            }

            JSONObject utilData = data.optJSONObject("Utils");
            if (utilData != null) {
                float scale = (float) utilData.optDouble("comboTextScale", 1f);

                Game.activity.runOnUiThread(() -> {
                    mNumber.setScaleX(scale);
                    mNumber.setScaleY(scale);
                });
            }
        }

        @Override
        public void onSelect() {
            int color = mBackground.getColor();

            Animation.ofColor(color, 0xFF222F3D)
                    .runOnUpdate(value -> {
                        mBackground.setColor((int) value);
                        mBody.invalidate();
                    })
                    .play(100);
        }

        @Override
        public void onDeselect() {
            int color = mBackground.getColor();

            Animation.ofColor(color, 0xFF242424)
                    .runOnUpdate(value -> {
                        mBackground.setColor((int) value);
                        mBody.invalidate();
                    })
                    .play(100);
        }
    }

}
