package com.edlplan.ui.fragment;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import androidx.annotation.StringRes;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.edlplan.framework.easing.Easing;
import com.edlplan.framework.support.util.Updater;
import com.edlplan.ui.BaseAnimationListener;
import com.edlplan.ui.EasingHelper;

import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.menu.IFilterMenu;
import ru.nsu.ccfit.zuev.osu.menu.SongMenu;
import ru.nsu.ccfit.zuev.osuplus.R;

public class FilterMenuFragment extends BaseFragment implements IFilterMenu {

    private static Context configContext = null;
    private static String savedFolder;
    private static boolean savedFavOnly = false;
    private static String savedFilter = null;
    private Scene scene = null;
    private EditText filter;
    private SongMenu menu;
    private CheckBox favoritesOnly;
    private TextView favoriteFolder;
    private Button orderButton;
    //private TextView openMapInfo;

    private Updater updater;

    public FilterMenuFragment() {
        setDismissOnBackgroundClick(true);
    }


    @Override
    protected int getLayoutID() {
        return R.layout.fragment_filtermenu;
    }

    @Override
    protected void onLoadView() {
        reloadViewData();
        playOnLoadAnim();
    }

    private void playOnLoadAnim() {
        View body = findViewById(R.id.frg_body);
        body.setAlpha(0);
        body.setTranslationX(400);
        body.animate().cancel();
        body.animate()
                .translationX(0)
                .alpha(1)
                .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
                .setDuration(150)
                .start();
        playBackgroundHideInAnim(150);
    }

    private void playEndAnim(Runnable action) {
        View body = findViewById(R.id.frg_body);
        body.animate().cancel();
        body.animate()
                .translationXBy(400)
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

    private void updateFavChecked() {
        favoritesOnly.setText(favoritesOnly.isChecked() ?
                R.string.menu_search_favsenabled : R.string.menu_search_favsdisabled);
    }

    private void updateOrderButton() {
        SongMenu.SortOrder order = getOrder();
        @StringRes int s;
        switch (order) {
            case Title:
                s = R.string.menu_search_sort_title;
                break;
            case Artist:
                s = R.string.menu_search_sort_artist;
                break;
            case Date:
                s = R.string.menu_search_sort_date;
                break;
            case Bpm:
                s = R.string.menu_search_sort_bpm;
                break;
            case Stars:
                s = R.string.menu_search_sort_stars;
                break;
            case Length:
                s = R.string.menu_search_sort_length;
                break;
            default:
                s = R.string.menu_search_sort_creator;
                break;
        }
        orderButton.setText(s);
    }

    private void updateFavFolderText() {
        if (savedFolder != null) {
            favoriteFolder.setText(savedFolder);
        }
        if (favoriteFolder.getText().length() == 0) {
            favoriteFolder.setText(StringTable.get(R.string.favorite_default));
        }
    }

    @Override
    public void dismiss() {
        playEndAnim(super::dismiss);
    }

    @Override
    public String getFilter() {
        return filter == null ? "" : (filter.getText() == null ? "" : filter.getText().toString());
    }

    @Override
    public SongMenu.SortOrder getOrder() {
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(configContext);
        final int sortOrder = prefs.getInt("sortorder", 0);
        return SongMenu.SortOrder.values()[sortOrder % SongMenu.SortOrder.values().length];
    }

    @SuppressLint("ApplySharedPref")
    private void saveOrder(SongMenu.SortOrder order) {
        PreferenceManager
                .getDefaultSharedPreferences(configContext)
                .edit()
                .putInt("sortorder", order.ordinal())
                .commit();
    }

    private void nextOrder() {
        SongMenu.SortOrder order = getOrder();
        order = SongMenu.SortOrder.values()[(order.ordinal() + 1) % SongMenu.SortOrder.values().length];
        saveOrder(order);
    }

    @Override
    public boolean isFavoritesOnly() {
        return favoritesOnly.isChecked();
    }

    @Override
    public String getFavoriteFolder() {
        return favoriteFolder == null ?
                null : StringTable.get(R.string.favorite_default).equals(favoriteFolder.getText().toString()) ?
                null : favoriteFolder.getText().toString();
    }

    @Override
    public void loadConfig(Context context) {
        configContext = context;
        reloadViewData();
    }

    @Override
    public Scene getScene() {
        return scene;
    }

    @Override
    public void hideMenu() {
        updateUpdater();
        scene = null;
        dismiss();
    }

    @Override
    public void showMenu(SongMenu parent) {
        this.menu = parent;
        scene = new Scene();
        scene.setBackgroundEnabled(false);
        if (parent != null) {
            //parent.scene.setChildScene(
            //        scene, false,
            //        true, true);
        }
        updater = new Updater() {
            @Override
            public Runnable createEventRunnable() {
                return () -> parent.loadFilter(FilterMenuFragment.this);
            }

            @Override
            public void postEvent(Runnable r) {
                parent.getScene().postRunnable(r);
            }
        };
        show();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (menu != null) {
            final SongMenu menu = this.menu;
            menu.getScene().postRunnable(() -> {
                //menu.getScene().clearChildScene();
                menu.loadFilter(this);
            });
            this.menu = null;
        }
        scene = null;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        reloadViewData();
    }

    private void updateUpdater() {
        if (updater != null) {
            updater.update();
        }
    }

    //private void openMapinfoDialog() {
    //    MapInfoFragment dialog = new MapInfoFragment();
    //    TrackInfo selectedTrack = GlobalManager.getInstance().getSongMenu().getSelectedTrack();
    //    DifficultyReCalculator diffReCalculator = new DifficultyReCalculator();
    //    if (selectedTrack != null)
    //        dialog.showWithMap(selectedTrack, ModMenu.getInstance().getSpeed(), diffReCalculator.getCS(selectedTrack));
    //    diffReCalculator = null;
    //}

    public void reloadViewData() {
        if (isCreated()) {
            filter = findViewById(R.id.searchEditText);
            favoritesOnly = findViewById(R.id.showFav);
            orderButton = findViewById(R.id.sortButton);
            favoriteFolder = findViewById(R.id.favFolder);
            //openMapInfo = findViewById(R.id.openMapInfo);

            favoritesOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateFavChecked();
                updateUpdater();
                savedFavOnly = isChecked;
            });
            orderButton.setOnClickListener(v -> {
                nextOrder();
                updateOrderButton();
                updateUpdater();
            });
            findViewById(R.id.favFolderLayout).setOnClickListener(v -> {
                FavoriteManagerFragment favoriteManagerFragment = new FavoriteManagerFragment();
                favoriteManagerFragment.showToSelectFloder(t -> {
                    savedFolder = t;
                    favoriteFolder.setText(t == null ? StringTable.get(R.string.favorite_default) : t);
                    updateUpdater();
                });
            });
            filter.setOnEditorActionListener((v, actionId, event) -> {
                if (event == null) {
                    return false;
                }

                if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    hideMenu();
                    return true;
                }
                return false;
            });
            filter.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    savedFilter = s.toString();
                    updateUpdater();
                }
            });

            //openMapInfo.setOnClickListener(v -> {
            //    openMapinfoDialog();
            //});
            //openMapInfo.setText("MapInfo");
            
            favoritesOnly.setChecked(savedFavOnly);
            if (savedFilter != null && savedFilter.length() > 0) {
                filter.setText(savedFilter);
            }
            updateOrderButton();
            updateFavChecked();
            updateFavFolderText();
        }
    }
}
