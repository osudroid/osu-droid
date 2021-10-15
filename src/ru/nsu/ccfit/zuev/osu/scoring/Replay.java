package ru.nsu.ccfit.zuev.osu.scoring;

import android.graphics.PointF;

import org.anddev.andengine.util.Debug;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.game.GameScene;
import ru.nsu.ccfit.zuev.osu.game.cursor.flashlight.FlashLightEntity;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osuplus.R;

public class Replay {

    public static final byte RESULT_300 = 4;
    public static final byte RESULT_100 = 3;
    public static final byte RESULT_50 = 2;
    public static final byte RESULT_0 = 1;
    public static final byte ID_DOWN = 0;
    public static final byte ID_UP = 2;

    public static final byte ID_MOVE = 1;
    public static EnumSet<GameMod> mod = EnumSet.noneOf(GameMod.class);
    public static EnumSet<GameMod> oldMod = EnumSet.noneOf(GameMod.class);
    private static int pointsSkipped = 0;
    public ArrayList<MoveArray> cursorMoves = new ArrayList<MoveArray>();
    public int[] cursorIndex;
    public int[] lastMoveIndex;
    public ReplayObjectData[] objectData = null;
    public int replayVersion;
    public StatisticV2 stat = null;
    private String md5 = "";
    private String mapfile = "";
    private String mapname = "";
    public static float oldChangeSpeed = 1.0f;
    public static float oldForceAR = 9.0f;
    public static boolean oldEnableForceAR = false;
    public static float oldFLFollowDelay = FlashLightEntity.defaultMoveDelayS;

    public Replay() {
        cursorMoves.add(new MoveArray(200));
        cursorMoves.add(new MoveArray(50));
        for (int i = 2; i < GameScene.CursorCount; i++) {
            cursorMoves.add(new MoveArray(15));
        }
        cursorIndex = new int[GameScene.CursorCount];
        lastMoveIndex = new int[GameScene.CursorCount];
        for (int i = 0; i < GameScene.CursorCount; i++) {
            cursorIndex[i] = 0;
            lastMoveIndex[i] = -1;
        }

        pointsSkipped = 0;
    }

    public void setMap(String mapname, String file, String md5) {
        this.mapname = mapname;
        this.md5 = md5;
        this.mapfile = file;
    }

    public void setObjectCount(int count) {
        objectData = new ReplayObjectData[count];
    }

    public void addObjectResult(int id, short accuracy, BitSet ticks) {
        if (id < 0 || objectData == null || id >= objectData.length)
            return;

        ReplayObjectData data = objectData[id] == null ? new ReplayObjectData() : objectData[id];
        data.accuracy = accuracy;
        data.tickSet = ticks;
        objectData[id] = data;
    }

    public void addObjectScore(int id, byte score) {
        if (id < 0 || objectData == null || id >= objectData.length)
            return;

        if (objectData[id] == null)
            objectData[id] = new ReplayObjectData();
        objectData[id].result = score;
    }

    public void addPress(final float time, final PointF pos, final int pid) {
        if (pid > GameScene.CursorCount) return;

        int itime = Math.max(0, (int) (time * 1000));
        cursorMoves.get(pid).pushBack(itime, (short) pos.x, (short) pos.y, ID_DOWN);
    }

    public void addMove(final float time, final PointF pos, final int pid) {
        if (pid > GameScene.CursorCount) return;

        int itime = Math.max(0, (int) (time * 1000));
        cursorMoves.get(pid).pushBack(itime, (short) pos.x, (short) pos.y, ID_MOVE);
    }

    public void addUp(final float time, final int pid) {
        if (pid > GameScene.CursorCount) return;

        int itime = Math.max(0, (int) (time * 1000));
        cursorMoves.get(pid).pushBack(itime, ID_UP);
    }

    public void save(final String filename) {
        for (int i = 0; i < cursorMoves.size(); i++)
            Debug.i("Replay contains " + cursorMoves.get(i).size + " moves for finger " + i);
        Debug.i("Skipped " + pointsSkipped + " points");
        Debug.i("Replay contains " + objectData.length + " objects");
        ObjectOutputStream os;
        ZipOutputStream zip;
        try {
            zip = new ZipOutputStream(new FileOutputStream(new File(filename)));
            zip.setMethod(ZipOutputStream.DEFLATED);
            zip.setLevel(Deflater.DEFAULT_COMPRESSION);
            zip.putNextEntry(new ZipEntry("data"));
            os = new ObjectOutputStream(zip);
        } catch (final FileNotFoundException e) {
            Debug.e("File not found " + filename, e);
            return;
        } catch (final IOException e) {
            Debug.e("IOException: " + e.getMessage(), e);
            return;
        }

        try {
            os.writeObject(new ReplayVersion());
            os.writeObject(mapname);
            os.writeObject(mapfile);
            os.writeObject(md5);

            if (stat != null) {
                os.writeLong(stat.getTime());
                os.writeInt(stat.getHit300k());
                os.writeInt(stat.getHit300());
                os.writeInt(stat.getHit100k());
                os.writeInt(stat.getHit100());
                os.writeInt(stat.getHit50());
                os.writeInt(stat.getMisses());
                os.writeInt(stat.getModifiedTotalScore());
                os.writeInt(stat.getMaxCombo());
                os.writeFloat(stat.getAccuracy());
                os.writeBoolean(stat.isPerfect());
                os.writeObject(stat.getPlayerName());
                os.writeObject(stat.getMod());
                //Add in replay version 4
                os.writeObject(stat.getExtraModString());
            }

            os.writeInt(cursorMoves.size());
            //Storing all moves
            for (final MoveArray move : cursorMoves) {
                move.writeTo(os);
            }
            os.writeInt(objectData.length);
            for (ReplayObjectData data : objectData) {
                if (data == null) data = new ReplayObjectData();
                os.writeShort(data.accuracy);
                if (data.tickSet == null || data.tickSet.length() == 0) {
                    os.writeByte(0);
                } else {
                    byte[] bytes = new byte[(data.tickSet.length() + 7) / 8];
                    for (int i = 0; i < data.tickSet.length(); i++) {
                        if (data.tickSet.get(i)) {
                            bytes[bytes.length - i / 8 - 1] |= 1 << (i % 8);
                        }
                    }
                    os.writeByte(bytes.length);
                    os.write(bytes);
                }
                os.writeByte(data.result);
            }
        } catch (final IOException e) {
            Debug.e("IOException: " + e.getMessage(), e);
            return;
        }

        try {
            os.flush();
            zip.flush();
            zip.closeEntry();
            zip.flush();
        } catch (final IOException e) {
            Debug.e("IOException: " + e.getMessage(), e);
        }

    }

    public boolean loadInfo(final String filename) {
        ObjectInputStream os;
        try {
            final ZipInputStream zip = new ZipInputStream(new FileInputStream(
                    new File(filename)));
            zip.getNextEntry();
            os = new ObjectInputStream(zip);
            // zip.close();
        } catch (final Exception e) {
            Debug.e("Cannot load replay: " + e.getMessage(), e);
            return false;
        }

        Debug.i("Loading replay " + filename);

        cursorMoves.clear();
        int version = 0;
        try {
            Object firstObject = os.readObject();
            Debug.i("Readed object: " + firstObject.getClass().getName());
            if (firstObject.getClass().equals(ReplayVersion.class)) {
                Debug.i("Other replay version");
                version = ((ReplayVersion) firstObject).version;
                replayVersion = version;
                mapname = (String) os.readObject();

            } else {
                mapname = (String) firstObject;
            }
            mapfile = (String) os.readObject();
            md5 = (String) os.readObject();

            Debug.i(mapname);
            Debug.i(mapfile);
            Debug.i(md5);

            if (version >= 3) {
                stat = new StatisticV2();
                stat.setTime(os.readLong());
                stat.setHit300k(os.readInt());
                stat.setHit300(os.readInt());
                stat.setHit100k(os.readInt());
                stat.setHit100(os.readInt());
                stat.setHit50(os.readInt());
                stat.setMisses(os.readInt());
                stat.setForcedScore(os.readInt());
                stat.setMaxCombo(os.readInt());
                stat.setAccuracy(os.readFloat());
                stat.setPerfect(os.readBoolean());
                stat.setPlayerName((String) os.readObject());
                stat.setMod((EnumSet<GameMod>) os.readObject());
            }

            if  (version >= 4) {
                stat.setExtraModFromString((String) os.readObject());
            }

        } catch (EOFException e) {
            Debug.e("O_o eof...");
            ToastLogger.showTextId(R.string.replay_corrupted, true);
            return false;

        } catch (Exception e) {
            ToastLogger.showTextId(R.string.replay_corrupted, true);
            Debug.e("Cannot load replay: " + e.getMessage(), e);
            return false;
        }

        return true;
    }

    public boolean load(final String filename) {
        ObjectInputStream os;
        try {
            final ZipInputStream zip = new ZipInputStream(new FileInputStream(
                    new File(filename)));
            zip.getNextEntry();
            os = new ObjectInputStream(zip);
            // zip.close();
        } catch (final Exception e) {
            Debug.e("Cannot load replay: " + e.getMessage(), e);
            return false;
        }

        Debug.i("Loading replay " + filename);

        cursorMoves.clear();
        int version = 0;
        try {
            String mname;
            Object firstObject = os.readObject();
            Debug.i("Readed object: " + firstObject.getClass().getName());
            if (firstObject.getClass().equals(ReplayVersion.class)) {
                Debug.i("Other replay version");
                version = ((ReplayVersion) firstObject).version;
                replayVersion = version;
                mname = (String) os.readObject();

            } else {
                mname = (String) firstObject;
            }
            String mfile = (String) os.readObject();
            String mmd5 = (String) os.readObject();

            if (mname.equals(mapname) == false && mfile.equals(mapfile) == false) {
                Debug.i("Replay doesn't match the map!");
                Debug.i(mapname + " ::: " + mname);
                Debug.i(mapfile + " ::: " + mfile);
                Debug.i(md5 + " ::: " + mmd5);
                ToastLogger.showTextId(R.string.replay_wrongmap, true);

                os.close();
                return false;
            }

            if (version >= 3) {
                stat = new StatisticV2();
                stat.setTime(os.readLong());
                stat.setHit300k(os.readInt());
                stat.setHit300(os.readInt());
                stat.setHit100k(os.readInt());
                stat.setHit100(os.readInt());
                stat.setHit50(os.readInt());
                stat.setMisses(os.readInt());
                stat.setForcedScore(os.readInt());
                stat.setMaxCombo(os.readInt());
                stat.setAccuracy(os.readFloat());
                stat.setPerfect(os.readBoolean());
                stat.setPlayerName((String) os.readObject());
                stat.setMod((EnumSet<GameMod>) os.readObject());
            }

            if  (version >= 4) {
                stat.setExtraModFromString((String) os.readObject());
            }

            int msize = os.readInt();
            for (int i = 0; i < msize; i++) {
                cursorMoves.add(MoveArray.readFrom(os, this));
            }

            os.readInt();
            for (int i = 0; i < objectData.length; i++) {
                ReplayObjectData data = new ReplayObjectData();
                data.accuracy = os.readShort();
                int len = os.readByte();
                if (len > 0) {
                    data.tickSet = new BitSet();
                    byte[] bytes = new byte[len];
                    os.read(bytes);
                    for (int j = 0; j < len * 8; j++) {
                        data.tickSet.set(j, (bytes[len - j / 8 - 1] & 1 << (j % 8)) != 0);
                    }
                }
                if (version >= 1) {
                    data.result = os.readByte();
                }
                objectData[i] = data;
            }
        } catch (EOFException e) {
            Debug.e("O_o eof...");
            ToastLogger.showTextId(R.string.replay_corrupted, true);
            return false;

        } catch (Exception e) {
            ToastLogger.showTextId(R.string.replay_corrupted, true);
            Debug.e("Cannot load replay: " + e.getMessage(), e);
            return false;
        }

        for (int i = 0; i < cursorMoves.size(); i++)
            Debug.i("Loaded " + cursorMoves.get(i).size + " moves for finger " + i);
        Debug.i("Loaded " + objectData.length + " objects");
        return true;
    }

    public void countMarks(float difficulty) {
    }

    public StatisticV2 getStat() {
        return stat;
    }

    public void setStat(StatisticV2 stat) {
        this.stat = stat;
    }

    public String getMd5() {
        return md5;
    }

    public String getMapfile() {
        return mapfile;
    }

    public String getMapname() {
        return mapname;
    }

    public static class ReplayVersion implements Serializable {
        private static final long serialVersionUID = 4643121693566795335L;
        int version = 4;
        // version 4: Add ExtraModString's save and load in save()/load()/loadInfo()
    }

    public static class ReplayObjectData {
        public short accuracy = 0;
        public BitSet tickSet = null;
        public byte result = 0;
    }

    public static class MoveArray {
        public int[] time;
        public short[] x;
        public short[] y;
        public byte[] id;

        public int size;
        public int allocated;

        public MoveArray(int startSize) {
            allocated = startSize;
            size = 0;
            time = new int[allocated];
            x = new short[allocated];
            y = new short[allocated];
            id = new byte[allocated];
        }

        public static MoveArray readFrom(ObjectInputStream is, Replay replay) throws IOException {
            int size = is.readInt();
            MoveArray array = new MoveArray(size);

            for (int i = 0; i < size; i++) {
                array.time[i] = is.readInt();
                array.id[i] = (byte) (array.time[i] & 3);
                array.time[i] >>= 2;
                if (array.id[i] != ID_UP) {
                    PointF gamePoint = new PointF((short) (is.readShort() / Config.getTextureQuality()),
                            (short) (is.readShort() / Config.getTextureQuality()));
					/*if (GameHelper.isHardrock())
					{
						array.y[i] = Utils.flipY(array.y[i]);
					}*/
                    if (replay.replayVersion == 1) {
                        PointF realPoint = Utils.trackToRealCoords(Utils.realToTrackCoords(gamePoint, 1024, 600, true));
                        array.x[i] = (short) realPoint.x;
                        array.y[i] = (short) realPoint.y;
                    } else if (replay.replayVersion > 1) {
                        PointF realPoint = Utils.trackToRealCoords(gamePoint);
                        array.x[i] = (short) realPoint.x;
                        array.y[i] = (short) realPoint.y;
                    }
                }
                array.size = size;
            }

            return array;
        }

        public void reallocate(int newSize) {
            if (newSize <= allocated) return;
            int[] newTime = new int[newSize];
            short[] newX = new short[newSize];
            short[] newY = new short[newSize];
            byte[] newId = new byte[newSize];

            System.arraycopy(time, 0, newTime, 0, size);
            System.arraycopy(x, 0, newX, 0, size);
            System.arraycopy(y, 0, newY, 0, size);
            System.arraycopy(id, 0, newId, 0, size);

            time = newTime;
            x = newX;
            y = newY;
            id = newId;

            allocated = newSize;
        }

        public boolean checkNewPoint(short px, short py) {
            if (size < 2) return false;
            float tx = (px + x[size - 2]) * 0.5f;
            float ty = (py + y[size - 2]) * 0.5f;

            return (Utils.sqr(x[size - 1] - tx) + Utils.sqr(y[size - 1] - ty)) <= 25;
        }

        public void pushBack(int time, short x, short y, byte id) {
            int idx = size;
            if (id == ID_MOVE && checkNewPoint(x, y)) {
                idx = size - 1;
                pointsSkipped++;
            } else {
                if (size + 1 >= allocated)
                    reallocate((allocated * 3) / 2);
                size++;
            }
            this.time[idx] = time;
            this.x[idx] = x;
            this.y[idx] = y;
            this.id[idx] = id;
        }

        public void pushBack(int time, byte id) {
            if (size >= allocated)
                reallocate((allocated * 3) / 2);
            this.time[size] = time;
            this.id[size] = id;
            size++;
        }

        public void writeTo(ObjectOutputStream os) throws IOException {
            os.writeInt(size);
            for (int i = 0; i < size; i++) {
                os.writeInt((time[i] << 2) + id[i]);
                if (id[i] != ID_UP) {
                    os.writeShort(x[i] * Config.getTextureQuality());
                    os.writeShort(y[i] * Config.getTextureQuality());
                }
            }
        }
    }
}
