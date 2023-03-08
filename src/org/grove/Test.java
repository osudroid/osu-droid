package org.grove;

import org.grove.cmt.ExistOrFoundInfo;
import org.grove.lib.result.Result;
import org.grove.prop.*;
import org.grove.result.*;
import org.grove.result.entities.BblScoreWithUsername;

import java.util.List;
import java.util.UUID;

public final class Test {

    private static final String

            TEST_USERNAME = "",
            TEST_PASSWORD = "",
            TEST_FILE_NAME = "Crywolf_-_Eyes_Half_Closed_(Truzon)_[Remap].osu",
            TEST_FILE_HASH = "eedb1567ea1043270a9a92468c8bc81f",
            TEST_QUERY = "Acivev";

    private static final long
            TEST_USER_ID = 55374,
            TEST_PLAY_ID = 6459578;

    //----------------------------------------------------------------------------------------------------------------//

    public static void main(String[] args) {
        Grove.init();

        if (Settings.getDomain() == null || Settings.getDomain().length() == 0) {
            System.err.println("\nCannot start test, please set domain and data token!");
            return;
        }

        log("\nTEST START\n");

        try {
            log("\ntestLogin START\n");
            UUID token = testLogin();
            log("\ntestLogin END\n");

            // These test require login token so if testLogin() fails then all of them too.
            try {
                log("\ntestToken START\n");
                testToken(token);
                log("\ntestToken END\n");
            } catch (Exception e) {
                e.printStackTrace();
                log("\ntestToken FAIL\n");
            }

            try {
                log("\ntestMapLeaderboard START\n");
                testMapLeaderboard(token);
                log("\ntestMapLeaderboard END\n");
            } catch (Exception e) {
                e.printStackTrace();
                log("\ntestMapLeaderboard FAIL\n");
            }

            try {
                log("\ntestPlayID START\n");
                testPlayID(token);
                log("\ntestPlayID END\n");
            } catch (Exception e) {
                e.printStackTrace();
                log("\ntestPlayID FAIL\n");
            }

            try {
                log("\ntestPushPlayStart START\n");
                testPushPlayStart(token);
                log("\ntestPushPlayStart END\n");
            } catch (Exception e) {
                e.printStackTrace();
                log("\ntestPushPlayStart FAIL\n");
            }

            try {
                log("\ntestPushReplay START\n");
                testPushReplay(token);
                log("\ntestPushReplay END\n");
            } catch (Exception e) {
                e.printStackTrace();
                log("\ntestPushReplay FAIL\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
            log("\ntestLogin FAIL\n");
        }

        try {
            log("\ntestAvatar START\n");
            testAvatar();
            log("\ntestAvatar END\n");
        } catch (Exception e) {
            e.printStackTrace();
            log("\ntestAvatar FAIL\n");
        }

        try {
            log("\ntestProfile START\n");
            testProfile();
            log("\ntestProfile END\n");
        } catch (Exception e) {
            e.printStackTrace();
            log("\ntestProfile FAIL\n");
        }

        try {
            log("\ntestWorldLeaderboard START\n");
            testWorldLeaderboard();
            log("\ntestWorldLeaderboard END\n");
        } catch (Exception e) {
            e.printStackTrace();
            log("\ntestWorldLeaderboard FAIL\n");
        }

        try {
            log("\ntestUserLeaderboard START\n");
            testUserLeaderboard();
            log("\ntestUserLeaderboard END\n");
        } catch (Exception e) {
            e.printStackTrace();
            log("\ntestUserLeaderboard FAIL\n");
        }

        try {
            log("\ntestLeaderboardQuery START\n");
            testLeaderboardQuery();
            log("\ntestLeaderboardQuery END\n");
        } catch (Exception e) {
            e.printStackTrace();
            log("\ntestLeaderboardQuery FAIL\n");
        }

        try {
            log("\ntestRecentPlays START\n");
            testRecentPlays();
            log("\ntestRecentPlays END\n");
        } catch (Exception e) {
            e.printStackTrace();
            log("\ntestRecentPlays FAIL\n");
        }

        try {
            log("\ntestActiveUsers START\n");
            testActiveUsers();
            log("\ntestActiveUsers END\n");
        } catch (Exception e) {
            e.printStackTrace();
            log("\ntestActiveUsers FAIL\n");
        }

        try {
            log("\ntestSupporterList START\n");
            testSupporterList();
            log("\ntestSupporterList END\n");
        } catch (Exception e) {
            e.printStackTrace();
            log("\ntestSupporterList FAIL\n");
        }

        log("\nTEST END\n");
    }

    //----------------------------------------------------------------------------------------------------------------//

    public static UUID testLogin() throws Exception {
        CreateApi2TokenProp prop = new CreateApi2TokenProp();
        prop.setUsername(TEST_USERNAME);
        prop.setPasswd(TEST_PASSWORD);

        Result<CreateApi2TokenResult, Exception> res = Grove.V2.login.tokenCreate(prop);

        if (res.isErr()) {
            throw res.getErr();
        }

        log(
                "Login is " + res.isOk() + "\n" +
                        "LoginToken is " + res.getOk().getToken() + "\n" +
                        "LoginPasswordInvalid is " + res.getOk().getPasswdFalse() + "\n" +
                        "LoginUsernameInvalid is " + res.getOk().getUsernameFalse());

        return res.getOk().getToken();
    }

    // Tests that require login token:
    //----------------------------------------------------------------------------------------------------------------//

    public static void testToken(UUID token) throws Exception {
        SimpleTokenProp prop = new SimpleTokenProp();
        prop.setToken(token);

        Result<ExistOrFoundInfo<Long>, Exception> res = Grove.V2.login.tokenUserId(prop);

        if (res.isErr()) {
            throw res.getErr();
        }

        log(
                "Result UserID Work: " + res.isOk() + "\n" +
                        "Result UserID Found: " + res.getOk().valueIsSet() + "\n" +
                        "Result UserID : " + res.getOk().getValue()
        );
    }

    public static void testMapLeaderboard(UUID token) throws Exception {
        Api2MapFileRankProp prop = new Api2MapFileRankProp();
        prop.setFilename(TEST_FILE_NAME);
        prop.setFileHash(TEST_FILE_HASH);

        Result<ExistOrFoundInfo<List<MapTopPlays>>, Exception> res = Grove.V2.rank.mapFile(prop, token);

        if (res.isErr()) {
            throw res.getErr();
        }
        if (!res.getOk().valueIsSet()) {
            throw new Exception("Value res Result Has No Value");
        }

        List<MapTopPlays> leaderboard = res.getOk().getValue();

        for (MapTopPlays mapRow : leaderboard) {
            log("Leaderboard MapRank " +
                    "PlayID: " + mapRow.getPlayId() + " " +
                    "UserID: " + mapRow.getUserId() + " " +
                    "Place: " + mapRow.getPlayRank() + " " +
                    "Username: " + mapRow.getUsername() + " " +
                    "Score: " + mapRow.getScore() + " " +
                    "Mark: " + mapRow.getMark() + " " +
                    "Mods: " + mapRow.getMode() + " " +
                    "Accuracy: " + mapRow.getAccuracy() + " " +
                    "Combo: " + mapRow.getCombo() + " " +
                    "Date: " + mapRow.getDate()
            );
        }

        log("Result Leaderboard RankMap Work: " + res.isOk());
        log("Result Leaderboard RankMap Type: " + res.getOk().getValue().size());
    }

    public static void testPlayID(UUID token) throws Exception {
        Api2PlayById prop = new Api2PlayById();
        prop.setPlayId(TEST_PLAY_ID);

        Result<PlayInfoById, Exception> res = Grove.V2.play.getById(prop, token);

        if (res.isErr()) {
            throw res.getErr();
        }

        log("Region: " + res.getOk().getRegion() + " Username : " + res.getOk().getUsername());
        log("Result Play Recent Work: " + res.isOk());
    }


    public static void testPushPlayStart(UUID token) throws Exception {

        PushPlayStartProp prop = new PushPlayStartProp();
        prop.setId(0);
        prop.setUid(55374);
        prop.setFilename("Dicks");
        prop.setFileHash("drfgr4e57u83245dsrestdf");
        prop.setMark("");
        prop.setMode("|");
        prop.setScore(0);
        prop.setAccuracy(0);
        prop.setPerfect(0);
        prop.setGood(0);
        prop.setBad(0);
        prop.setCombo(0);
        prop.setMiss(0);
        prop.setGeki(0);
        prop.setKatu(0);

        Result<PushPlayStartResult200, Exception> push = Grove.V2.submit.pushPlayStart(prop, token);

        if (push.isErr()) {
            throw push.getErr();
        }

        log("Sending Push Start Map: " + "Push Play ID: " + push.getOk().getPlayId());
    }

    public static void testPushReplay(UUID token) throws Exception {
        PushPlayProp prop = new PushPlayProp();
        prop.setId(1);
        prop.setUid(55374);
        prop.setMark("XSS");
        prop.setCombo(2000);
        prop.setScore(2147483647);
        prop.setAccuracy(100000);
        prop.setPerfect(300);
        prop.setMode("|");
        prop.setGood(0);
        prop.setBad(0);
        prop.setMiss(0);
        prop.setGeki(0);
        prop.setKatu(0);

        Result<PushReplayResult200, Exception> push = Grove.V2.submit.pushReplay(prop, token);

        if (push.isErr()) {
            throw push.getErr();
        }

        log("Sending Map: Play ID: " + push.getOk().getBestPlayScoreId() + "\n" +
                "Userstats" + push.getOk().getUserStats()
        );
    }

    //----------------------------------------------------------------------------------------------------------------//

    public static void testAvatar() throws Exception {
        Result<byte[], Exception> res = Grove.V2.avatar.getAvatar(200, TEST_USER_ID);

        if (res.isErr()) {
            throw res.getErr();
        }

        log("Result Ava Work: " + res.isOk() + " " + "Result Ava byte Len: " + res.getOk().length);
    }

    public static void testWorldLeaderboard() throws Exception {
        LeaderBoardProp prop = new LeaderBoardProp();
        prop.setLimit(50);
        prop.setRegion("all");

        Result<ExistOrFoundInfo<List<LeaderBoardUser>>, Exception> res = Grove.V2.leaderboard.get(prop);

        if (res.isErr()) {
            throw res.getErr();
        }
        if (!res.getOk().valueIsSet()) {
            throw new Exception("Value res Result Has No Value");
        }

        List<LeaderBoardUser> leaderboard = res.getOk().getValue();

        for (LeaderBoardUser lbRow : leaderboard) {
            log("Leaderboard Top Worldwide " +
                    "UserID: " + lbRow.getId() + " " +
                    "Rank: " + lbRow.getRank() + " " +
                    "Username: " + lbRow.getUsername() + " " +
                    "Region: " + lbRow.getRegion() + " " +
                    "Score: " + lbRow.getOverallScore() + " " +
                    "Playcount: " + lbRow.getOverallPlaycount() + " " +
                    "Accuracy: " + lbRow.getOverallAccuracy() + " " +
                    "SS: " + lbRow.getOverallSs() + " " +
                    "S: " + lbRow.getOverallS() + " " +
                    "A: " + lbRow.getOverallA()
            );
        }

        log("Result Leaderboard Work: " + res.isOk());
        log("Result Leaderboard Type: " + res.getOk().getValue().size());
    }

    public static void testUserLeaderboard() throws Exception {
        LeaderBoardUserProp prop = new LeaderBoardUserProp();
        prop.setUserId(TEST_USER_ID);

        Result<ExistOrFoundInfo<LeaderBoardUser>, Exception> res = Grove.V2.leaderboard.getUser(prop);

        if (res.isErr()) {
            throw res.getErr();
        }
        if (!res.getOk().valueIsSet()) {
            throw new Exception("Value res Result Has No Value");
        }

        LeaderBoardUser userD = res.getOk().getValue();
        log("Leaderboard Top Worldwide " +
                "UserID: " + userD.getId() + " " +
                "Rank: " + userD.getRank() + " " +
                "Username: " + userD.getUsername() + " " +
                "Region: " + userD.getRegion() + " " +
                "Score: " + userD.getOverallScore() + " " +
                "Playcount: " + userD.getOverallPlaycount() + " " +
                "Accuracy: " + userD.getOverallAccuracy() + " " +
                "SS: " + userD.getOverallSs() + " " +
                "S: " + userD.getOverallS() + " " +
                "A: " + userD.getOverallA()
        );

        log("Result Leaderboard User Work: " + res.isOk());
    }

    public static void testLeaderboardQuery() throws Exception {

        LeaderBoardSearchUserProp prop = new LeaderBoardSearchUserProp();
        prop.setLimit(50);
        prop.setRegion("all");
        prop.setQuery(TEST_QUERY);

        Result<ExistOrFoundInfo<List<LeaderBoardUser>>, Exception> res = Grove.V2.leaderboard.getSearchUser(prop);

        if (res.isErr()) {
            throw res.getErr();
        }
        if (!res.getOk().valueIsSet()) {
            throw new Exception("Value res Result Has No Value");
        }
        List<LeaderBoardUser> results = res.getOk().getValue();

        for (LeaderBoardUser userSearch : results) {
            log("Search User " +
                    "UserID: " + userSearch.getId() + " " +
                    "Rank: " + userSearch.getRank() + " " +
                    "Username: " + userSearch.getUsername() + " " +
                    "Region: " + userSearch.getRegion() + " " +
                    "Score: " + userSearch.getOverallScore() + " " +
                    "Playcount: " + userSearch.getOverallPlaycount() + " " +
                    "Accuracy: " + userSearch.getOverallAccuracy() + " " +
                    "SS: " + userSearch.getOverallSs() + " " +
                    "S: " + userSearch.getOverallS() + " " +
                    "A: " + userSearch.getOverallA()
            );
        }

        log("Result Leaderboard Search User Work: " + res.isOk());
        log("Result Leaderboard Search User Type: " + res.getOk().getValue().size());
    }

    public static void testRecentPlays() throws Exception {
        RecentPlays prop = new RecentPlays();
        prop.setStartAt(0);
        prop.setFilterPlays("Any");
        prop.setOrderBy("Time_DESC");
        prop.setLimit(100);

        Result<ExistOrFoundInfo<List<BblScoreWithUsername>>, Exception> res = Grove.V2.play.getRecent(prop);

        if (res.isErr()) {
            throw res.getErr();
        }
        log("Result Play Recent Work: " + res.isOk());
    }

    public static void testActiveUsers() throws Exception {
        Result<ExistOrFoundInfo<StatisticActiveUser>, Exception> res = Grove.V2.statistic.activeUser();

        if (res.isErr()) {
            throw res.getErr();
        }

        StatisticActiveUser resActive = res.getOk().getValue();
        log("Result Active User: " +
                "Last 1Hr: " + resActive.getActiveUserLast1h() + " " +
                "Last 1Day: " + resActive.getActiveUserLast1Day() + " " +
                "Registered: " + resActive.getRegisterUser()
        );

    }

    public static void testSupporterList() throws Exception {
        Result<ExistOrFoundInfo<List<UsernameAndId>>, Exception> res = Grove.V2.statistic.allPatreon();

        if (res.isErr()) {
            throw res.getErr();
        }
        List<UsernameAndId> list = res.getOk().getValue();

        for (UsernameAndId user : list) {
            log("Patron User " +
                    "UserID: " + user.getId() + " " +
                    "Username: " + user.getUsername()
            );
        }
    }

    //----------------------------------------------------------------------------------------------------------------//

    // TODO Test profile statistics!
    public static void testProfile() throws Exception {
        ProfileStatsProp prop = new ProfileStatsProp();
        prop.setUserId(TEST_USER_ID);

        Result<ExistOrFoundInfo<ProfileStatistic>, Exception> res = Grove.V1.profileStats.profileStats(prop);

        if (res.isErr()) {
            throw res.getErr();
        }

        log("Rankin: " + res.getOk().getValue().getCountryRanking() + " " +
                "Result Work: " + res.isOk() + "\n" +
                "ResultFound: " + res.getOk().valueIsSet() + "\n" +
                "Result : " + res.getOk().getValue()
        );
    }

    //----------------------------------------------------------------------------------------------------------------//

    private static void log(String text) {
        System.out.println(text);
    }
}
