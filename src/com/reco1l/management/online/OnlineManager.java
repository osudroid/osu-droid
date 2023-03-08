package com.reco1l.management.online;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.reco1l.management.Settings;
import com.reco1l.framework.Logging;

import org.grove.Grove;
import org.grove.cmt.ExistOrFoundInfo;
import org.grove.cmt.Work;
import org.grove.lib.result.Result;
import org.grove.prop.Api2UploadReplayFileProp;
import org.grove.prop.CreateApi2TokenProp;
import org.grove.prop.LeaderBoardUserProp;
import org.grove.prop.PushPlayProp;
import org.grove.prop.PushPlayStartProp;
import org.grove.prop.SimpleTokenProp;
import org.grove.result.CreateApi2TokenResult;
import org.grove.result.LeaderBoardUser;
import org.grove.result.PushPlayStartResult200;
import org.grove.result.PushReplayResult200;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.UUID;

import main.osu.TrackInfo;
import main.osu.helper.FileUtils;
import main.osu.scoring.StatisticV2;

// Don't operate in main thread!
public final class OnlineManager {

    public static final OnlineManager instance = new OnlineManager();

    private final ArrayList<IOnlineObserver> mObservers;

    private UserInfo mCurrentUser;
    private UUID mToken;

    // TODO [OnlineManager] shouldn't be here!
    private long mCurrentPlayID;

    //--------------------------------------------------------------------------------------------//

    private OnlineManager() {
        mObservers = new ArrayList<>();
    }

    //--------------------------------------------------------------------------------------------//

    private void clear(boolean notify) {
        mCurrentUser = null;
        mToken = null;

        Settings.edit()
                .putString("username", null)
                .putString("password", null)
                .commit();

        if (notify) {
            mObservers.forEach(IOnlineObserver::onClear);
        }
    }

    // Login
    //--------------------------------------------------------------------------------------------//

    public void tryLogin() {
        if (isOfflineMode()) {
            return;
        }

        String user = Settings.get("username");
        String pass = Settings.get("password");

        try {
            login(user, pass);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void login(String user, String pw) throws Exception {
        clear(false);

        Settings.edit()
                .putString("username", user)
                .putString("password", pw)
                .commit();

        mCurrentUser = createSessionOf(user, pw);

        loadStatisticsOf(mCurrentUser);

        try {
            loadAvatarOf(mCurrentUser, 200);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mObservers.forEach(o -> o.onLogin(mCurrentUser));
    }

    private UserInfo createSessionOf(String name, String pass) throws Exception {
        Logging.i(this, "Called createSessionOf(" + name + "," + pass + ")");

        UserInfo user = new UserInfo(name);

        // Login
        CreateApi2TokenProp loginProp = new CreateApi2TokenProp();
        loginProp.setUsername(name);
        loginProp.setPasswd(pass);

        Result<CreateApi2TokenResult, Exception> loginRes = Grove.V2.login.tokenCreate(loginProp);

        if (loginRes.isErr()) {
            clear(true);
            throw loginRes.getErr();
        }

        Logging.i(this,
                "LoginToken is " + loginRes.getOk().getToken() + "\n" +
                "LoginPasswordInvalid is " + loginRes.getOk().getPasswdFalse() + "\n" +
                "LoginUsernameInvalid is " + loginRes.getOk().getUsernameFalse());

        mToken = loginRes.getOk().getToken();

        // Getting user ID
        SimpleTokenProp tokenProp = new SimpleTokenProp();
        tokenProp.setToken(mToken);

        Result<ExistOrFoundInfo<Long>, Exception> tokenRes = Grove.V2.login.tokenUserId(tokenProp);

        if (tokenRes.isErr()) {
            clear(true);
            throw tokenRes.getErr();
        }

        ExistOrFoundInfo<Long> ok = tokenRes.getOk();

        if (ok.valueIsSet()) {
            user.setID(ok.getValue());
        } else {
            clear(true);
            throw new Exception("Unable to get user ID!");
        }

        return user;
    }

    public void logOut() {
        clear(true);

        if (mToken == null) {
            return;
        }

        SimpleTokenProp tokenProp = new SimpleTokenProp();
        tokenProp.setToken(mToken);

        Result<Work, Exception> res = Grove.V2.login.tokenRemove(tokenProp);

        try {
            Work work = res.getOk();

            if (work.isHasWork()) {
                Logging.i(this, "Log out and token remove success");
            } else {
                Logging.i(this, "Log out and token remove fail");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Avatar
    //--------------------------------------------------------------------------------------------//

    public Bitmap loadAvatarOf(long id, int size) throws Exception {

        Result<byte[], Exception> res = Grove.V2.avatar.getAvatar(size, id);

        if (res.isErr()) {
            throw res.getErr();
        }
        byte[] bytes = res.getOk();

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    public void loadAvatarOf(UserInfo user, int size) throws Exception {
        Bitmap bm = loadAvatarOf(user.getID(), size);
        user.setAvatar(bm);
    }


    // Statistics
    //--------------------------------------------------------------------------------------------//

    public void loadStatisticsOf(UserInfo user) {

        LeaderBoardUserProp propUser = new LeaderBoardUserProp();
        propUser.setUserId(user.getID());

        Result<ExistOrFoundInfo<LeaderBoardUser>, Exception> res = Grove.V2.leaderboard.getUser(propUser);

        if (res.isErr()) {
            return;
        }

        LeaderBoardUser s = res.getOkUnsafe().getValue();

        user.setRank(s.getRank());
        user.setRegion(s.getRegion());
        user.setScore(s.getOverallScore());
        user.setPlayCount(s.getOverallPlaycount());
        user.setAccuracy(s.getOverallAccuracy());
        user.setSSCount(s.getOverallSs());
        user.setSCount(s.getOverallS());
        user.setACount(s.getOverallA());
    }

    // Replay
    //--------------------------------------------------------------------------------------------//

    public long requestPlayID(TrackInfo track) throws Exception {
        Logging.i(this, "Called requestPlayID(" + track.getFilename() + ")");

        File file = new File(track.getFilename());
        String hash = FileUtils.getMD5Checksum(file);

        if (hash == null || hash.length() == 0) {
            Logging.i(this, "Invalid hash: " + hash);
            throw new Exception("Invalid file hash!");
        }

        PushPlayStartProp prop = new PushPlayStartProp();
        prop.setUid(mCurrentUser.getID());
        prop.setFilename(file.getName());
        prop.setFileHash(hash);

        Result<PushPlayStartResult200, Exception> push = Grove.V2.submit.pushPlayStart(prop, mToken);

        if (push.isErr()) {
            mCurrentPlayID = -1;
            throw push.getErr();
        }
        Logging.i(this, "Play ID: " + push.getOk().getPlayId());

        return mCurrentPlayID = push.getOk().getPlayId();
    }

    public void submitReplay(long id, StatisticV2 stats, File odr) throws Exception {
        id = mCurrentPlayID;

        Logging.i(this, "Called submitReplay(" + id + "," + odr.getPath() + ")");

        if (mCurrentPlayID == -1) {
            Logging.i(this, "Invalid play ID");
            throw new Exception("Invalid play id!");
        }

        PushPlayProp prop = new PushPlayProp();

        prop.setId(id);
        prop.setUid(mCurrentUser.getID());
        prop.setMark(stats.getMark());
        prop.setCombo(stats.getMaxCombo());
        prop.setScore(stats.getAutoTotalScore());
        prop.setAccuracy(stats.getAccuracyL());
        prop.setPerfect(stats.getHit300());
        prop.setMode(stats.getModString());
        prop.setGood(stats.getHit100());
        prop.setBad(stats.getHit50());
        prop.setMiss(stats.getMisses());
        prop.setGeki(stats.getHit300k());
        prop.setKatu(stats.getHit100k());

        Result<PushReplayResult200, Exception> push = Grove.V2.submit.pushReplay(prop, mToken);

        if (push.isErr()) {
            throw push.getErr();
        }
        //pushReplayFile(id, odr);
    }

    private void pushReplayFile(long id, File odr) throws Exception {
        byte[] bytes = Files.readAllBytes(odr.toPath());
        String hash = FileUtils.getSHA256Checksum(odr);

        Api2UploadReplayFileProp prop = new Api2UploadReplayFileProp();
        prop.setReplayId(id);
        prop.setMapHash(hash);

        Result<Work, Exception> res = Grove.V2.submit.uploadReplayFile(prop, mToken, bytes);

        if (res.isErr()) {
            throw res.getErr();
        }
    }

    //--------------------------------------------------------------------------------------------//

    public UserInfo getCurrentUser() {
        return mCurrentUser;
    }

    public boolean isOfflineMode() {
        return Settings.<Boolean>get("offlineMode", false);
    }

    public boolean isLogged() {
        return !isOfflineMode() && mCurrentUser != null;
    }

    //--------------------------------------------------------------------------------------------//

    public void bindOnlineObserver(IOnlineObserver observer) {
        mObservers.add(observer);
    }

    public void unbindOnlineObserver(IOnlineObserver observer) {
        mObservers.remove(observer);
    }
}
