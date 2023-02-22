/*
	BASS 2.4 Java class
	Copyright (c) 1999-2022 Un4seen Developments Ltd.

	See the BASS.CHM file for more detailed documentation
*/

package com.un4seen.bass;

import java.io.IOException;
import java.nio.ByteBuffer;
import android.content.res.AssetManager;
import android.os.ParcelFileDescriptor;

@SuppressWarnings({"all"})
public class BASS
{
	public static final int BASSVERSION = 0x204;	// API version
	public static final String BASSVERSIONTEXT = "2.4";

	// Error codes returned by BASS_ErrorGetCode
	public static final int BASS_OK = 0;	// all is OK
	public static final int BASS_ERROR_MEM = 1;	// memory error
	public static final int BASS_ERROR_FILEOPEN = 2;	// can't open the file
	public static final int BASS_ERROR_DRIVER = 3;	// can't find a free/valid driver
	public static final int BASS_ERROR_BUFLOST = 4;	// the sample buffer was lost
	public static final int BASS_ERROR_HANDLE = 5;	// invalid handle
	public static final int BASS_ERROR_FORMAT = 6;	// unsupported sample format
	public static final int BASS_ERROR_POSITION = 7;	// invalid position
	public static final int BASS_ERROR_INIT = 8;	// BASS_Init has not been successfully called
	public static final int BASS_ERROR_START = 9;	// BASS_Start has not been successfully called
	public static final int BASS_ERROR_SSL = 10;	// SSL/HTTPS support isn't available
	public static final int BASS_ERROR_REINIT = 11;	// device needs to be reinitialized
	public static final int BASS_ERROR_ALREADY = 14;	// already initialized/paused/whatever
	public static final int BASS_ERROR_NOTAUDIO = 17;	// file does not contain audio
	public static final int BASS_ERROR_NOCHAN = 18;	// can't get a free channel
	public static final int BASS_ERROR_ILLTYPE = 19;	// an illegal type was specified
	public static final int BASS_ERROR_ILLPARAM = 20;	// an illegal parameter was specified
	public static final int BASS_ERROR_NO3D = 21;	// no 3D support
	public static final int BASS_ERROR_NOEAX = 22;	// no EAX support
	public static final int BASS_ERROR_DEVICE = 23;	// illegal device number
	public static final int BASS_ERROR_NOPLAY = 24;	// not playing
	public static final int BASS_ERROR_FREQ = 25;	// illegal sample rate
	public static final int BASS_ERROR_NOTFILE = 27;	// the stream is not a file stream
	public static final int BASS_ERROR_NOHW = 29;	// no hardware voices available
	public static final int BASS_ERROR_EMPTY = 31;	// the file has no sample data
	public static final int BASS_ERROR_NONET = 32;	// no internet connection could be opened
	public static final int BASS_ERROR_CREATE = 33;	// couldn't create the file
	public static final int BASS_ERROR_NOFX = 34;	// effects are not available
	public static final int BASS_ERROR_NOTAVAIL = 37;	// requested data/action is not available
	public static final int BASS_ERROR_DECODE = 38;	// the channel is a "decoding channel"
	public static final int BASS_ERROR_DX = 39;	// a sufficient DirectX version is not installed
	public static final int BASS_ERROR_TIMEOUT = 40;	// connection timedout
	public static final int BASS_ERROR_FILEFORM = 41;	// unsupported file format
	public static final int BASS_ERROR_SPEAKER = 42;	// unavailable speaker
	public static final int BASS_ERROR_VERSION = 43;	// invalid BASS version (used by add-ons)
	public static final int BASS_ERROR_CODEC = 44;	// codec is not available/supported
	public static final int BASS_ERROR_ENDED = 45;	// the channel/file has ended
	public static final int BASS_ERROR_BUSY = 46;	// the device is busy
	public static final int BASS_ERROR_UNSTREAMABLE = 47; // unstreamable file
	public static final int BASS_ERROR_PROTOCOL = 48;	// unsupported protocol
	public static final int BASS_ERROR_DENIED = 49;		// access denied
	public static final int BASS_ERROR_UNKNOWN = -1;	// some other mystery problem

	public static final int BASS_ERROR_JAVA_CLASS = 500;	// object class problem

	// BASS_SetConfig options
	public static final int BASS_CONFIG_BUFFER = 0;
	public static final int BASS_CONFIG_UPDATEPERIOD = 1;
	public static final int BASS_CONFIG_GVOL_SAMPLE = 4;
	public static final int BASS_CONFIG_GVOL_STREAM = 5;
	public static final int BASS_CONFIG_GVOL_MUSIC = 6;
	public static final int BASS_CONFIG_CURVE_VOL = 7;
	public static final int BASS_CONFIG_CURVE_PAN = 8;
	public static final int BASS_CONFIG_FLOATDSP = 9;
	public static final int BASS_CONFIG_3DALGORITHM = 10;
	public static final int BASS_CONFIG_NET_TIMEOUT = 11;
	public static final int BASS_CONFIG_NET_BUFFER = 12;
	public static final int BASS_CONFIG_PAUSE_NOPLAY = 13;
	public static final int BASS_CONFIG_NET_PREBUF = 15;
	public static final int BASS_CONFIG_NET_PASSIVE = 18;
	public static final int BASS_CONFIG_REC_BUFFER = 19;
	public static final int BASS_CONFIG_NET_PLAYLIST = 21;
	public static final int BASS_CONFIG_MUSIC_VIRTUAL = 22;
	public static final int BASS_CONFIG_VERIFY = 23;
	public static final int BASS_CONFIG_UPDATETHREADS = 24;
	public static final int BASS_CONFIG_DEV_BUFFER = 27;
	public static final int BASS_CONFIG_DEV_DEFAULT = 36;
	public static final int BASS_CONFIG_NET_READTIMEOUT = 37;
	public static final int BASS_CONFIG_HANDLES = 41;
	public static final int BASS_CONFIG_SRC = 43;
	public static final int BASS_CONFIG_SRC_SAMPLE = 44;
	public static final int BASS_CONFIG_ASYNCFILE_BUFFER = 45;
	public static final int BASS_CONFIG_OGG_PRESCAN = 47;
	public static final int BASS_CONFIG_DEV_NONSTOP = 50;
	public static final int BASS_CONFIG_VERIFY_NET = 52;
	public static final int BASS_CONFIG_DEV_PERIOD = 53;
	public static final int BASS_CONFIG_FLOAT = 54;
	public static final int BASS_CONFIG_NET_SEEK = 56;
	public static final int BASS_CONFIG_AM_DISABLE = 58;
	public static final int BASS_CONFIG_NET_PLAYLIST_DEPTH = 59;
	public static final int BASS_CONFIG_NET_PREBUF_WAIT = 60;
	public static final int BASS_CONFIG_ANDROID_SESSIONID = 62;
	public static final int BASS_CONFIG_ANDROID_AAUDIO = 67;
	public static final int BASS_CONFIG_SAMPLE_ONEHANDLE = 69;
	public static final int BASS_CONFIG_DEV_TIMEOUT = 70;
	public static final int BASS_CONFIG_NET_META = 71;
	public static final int BASS_CONFIG_NET_RESTRATE = 72;
	public static final int BASS_CONFIG_REC_DEFAULT = 73;
	public static final int BASS_CONFIG_NORAMP = 74;

	// BASS_SetConfigPtr options
	public static final int BASS_CONFIG_NET_AGENT = 16;
	public static final int BASS_CONFIG_NET_PROXY = 17;
	public static final int BASS_CONFIG_LIBSSL = 64;
	public static final int BASS_CONFIG_FILENAME = 75;

	public static final int BASS_CONFIG_THREAD = 0x40000000; // flag: thread-specific setting

	// BASS_Init flags
	public static final int BASS_DEVICE_8BITS = 1;	// unused
	public static final int BASS_DEVICE_MONO = 2;	// mono
	public static final int BASS_DEVICE_3D = 4;	// unused
	public static final int BASS_DEVICE_16BITS = 8;		// limit output to 16-bit
	public static final int BASS_DEVICE_REINIT = 128;		// reinitialize
	public static final int BASS_DEVICE_LATENCY = 0x100;	// unused
	public static final int BASS_DEVICE_SPEAKERS = 0x800; // force enabling of speaker assignment
	public static final int BASS_DEVICE_NOSPEAKER = 0x1000; // ignore speaker arrangement
	public static final int BASS_DEVICE_FREQ = 0x4000; // set device sample rate
	public static final int BASS_DEVICE_AUDIOTRACK = 0x20000; // use AudioTrack output
	public static final int BASS_DEVICE_SOFTWARE	= 0x80000;	// disable hardware/fastpath output

	// Device info structure
	public static class BASS_DEVICEINFO {
		public String name;	// description
		public String driver;	// driver
		public int flags;
	}

	// BASS_DEVICEINFO flags
	public static final int BASS_DEVICE_ENABLED = 1;
	public static final int BASS_DEVICE_DEFAULT = 2;
	public static final int BASS_DEVICE_INIT = 4;

	public static class BASS_INFO {
		public int flags;	// device capabilities (DSCAPS_xxx flags)
		public int hwsize;	// unused
		public int hwfree;	// unused
		public int freesam;	// unused
		public int free3d;	// unused
		public int minrate;	// unused
		public int maxrate;	// unused
		public int eax;	// unused
		public int minbuf;	// recommended minimum buffer length in ms
		public int dsver;	// DirectSound version
		public int latency;	// average delay (in ms) before start of playback
		public int initflags; // BASS_Init "flags" parameter
		public int speakers; // number of speakers available
		public int freq;		// current output rate
	}

	// Recording device info structure
	public static class BASS_RECORDINFO {
		public int flags;	// device capabilities (DSCCAPS_xxx flags)
		public int formats;	// supported standard formats (WAVE_FORMAT_xxx flags)
		public int inputs;	// number of inputs
		public boolean singlein;	// TRUE = only 1 input can be set at a time
		public int freq;		// current input rate
	}

	// Sample info structure
	public static class BASS_SAMPLE {
		public int freq;		// default playback rate
		public float volume;	// default volume (0-1)
		public float pan;		// default pan (-1=left, 0=middle, 1=right)
		public int flags;	// BASS_SAMPLE_xxx flags
		public int length;	// length (in bytes)
		public int max;		// maximum simultaneous playbacks
		public int origres;	// original resolution bits
		public int chans;	// number of channels
		public int mingap;	// minimum gap (ms) between creating channels
		public int mode3d;	// BASS_3DMODE_xxx mode
		public float mindist;	// minimum distance
		public float maxdist;	// maximum distance
		public int iangle;	// angle of inside projection cone
		public int oangle;	// angle of outside projection cone
		public float outvol;	// delta-volume outside the projection cone
		public int vam;		// unused
		public int priority;	// unused
	}

	public static final int BASS_SAMPLE_8BITS = 1;		// 8 bit
	public static final int BASS_SAMPLE_FLOAT = 256;	// 32-bit floating-point
	public static final int BASS_SAMPLE_MONO = 2;		// mono
	public static final int BASS_SAMPLE_LOOP = 4;		// looped
	public static final int BASS_SAMPLE_3D = 8;			// 3D functionality
	public static final int BASS_SAMPLE_SOFTWARE = 16;	// unused
	public static final int BASS_SAMPLE_MUTEMAX = 32;	// mute at max distance (3D only)
	public static final int BASS_SAMPLE_VAM = 64;		// unused
	public static final int BASS_SAMPLE_FX = 128;		// unused
	public static final int BASS_SAMPLE_OVER_VOL = 0x10000;	// override lowest volume
	public static final int BASS_SAMPLE_OVER_POS = 0x20000;	// override longest playing
	public static final int BASS_SAMPLE_OVER_DIST = 0x30000; // override furthest from listener (3D only)

	public static final int BASS_STREAM_PRESCAN = 0x20000;	// scan file for accurate seeking and length
	public static final int BASS_STREAM_AUTOFREE = 0x40000;	// automatically free the stream when it stops/ends
	public static final int BASS_STREAM_RESTRATE = 0x80000;	// restrict the download rate of internet file streams
	public static final int BASS_STREAM_BLOCK = 0x100000;	// download/play internet file stream in small blocks
	public static final int BASS_STREAM_DECODE = 0x200000;	// don't play the stream, only decode (BASS_ChannelGetData)
	public static final int BASS_STREAM_STATUS = 0x800000;	// give server status info (HTTP/ICY tags) in DOWNLOADPROC

	public static final int BASS_MP3_IGNOREDELAY = 0x200;	// ignore LAME/Xing/VBRI/iTunes delay & padding info
	public static final int BASS_MP3_SETPOS = BASS_STREAM_PRESCAN;

	public static final int BASS_MUSIC_FLOAT = BASS_SAMPLE_FLOAT;
	public static final int BASS_MUSIC_MONO = BASS_SAMPLE_MONO;
	public static final int BASS_MUSIC_LOOP = BASS_SAMPLE_LOOP;
	public static final int BASS_MUSIC_3D = BASS_SAMPLE_3D;
	public static final int BASS_MUSIC_FX = BASS_SAMPLE_FX;
	public static final int BASS_MUSIC_AUTOFREE = BASS_STREAM_AUTOFREE;
	public static final int BASS_MUSIC_DECODE = BASS_STREAM_DECODE;
	public static final int BASS_MUSIC_PRESCAN = BASS_STREAM_PRESCAN;	// calculate playback length
	public static final int BASS_MUSIC_CALCLEN = BASS_MUSIC_PRESCAN;
	public static final int BASS_MUSIC_RAMP = 0x200;	// normal ramping
	public static final int BASS_MUSIC_RAMPS = 0x400;	// sensitive ramping
	public static final int BASS_MUSIC_SURROUND = 0x800;	// surround sound
	public static final int BASS_MUSIC_SURROUND2 = 0x1000;	// surround sound (mode 2)
	public static final int BASS_MUSIC_FT2PAN = 0x2000; // apply FastTracker 2 panning to XM files
	public static final int BASS_MUSIC_FT2MOD = 0x2000;	// play .MOD as FastTracker 2 does
	public static final int BASS_MUSIC_PT1MOD = 0x4000;	// play .MOD as ProTracker 1 does
	public static final int BASS_MUSIC_NONINTER = 0x10000;	// non-interpolated sample mixing
	public static final int BASS_MUSIC_SINCINTER = 0x800000; // sinc interpolated sample mixing
	public static final int BASS_MUSIC_POSRESET = 0x8000;	// stop all notes when moving position
	public static final int BASS_MUSIC_POSRESETEX = 0x400000; // stop all notes and reset bmp/etc when moving position
	public static final int BASS_MUSIC_STOPBACK = 0x80000;	// stop the music on a backwards jump effect
	public static final int BASS_MUSIC_NOSAMPLE = 0x100000; // don't load the samples
	
	// Speaker assignment flags
	public static final int BASS_SPEAKER_FRONT = 0x1000000;	// front speakers
	public static final int BASS_SPEAKER_REAR = 0x2000000;	// rear speakers
	public static final int BASS_SPEAKER_CENLFE = 0x3000000;	// center & LFE speakers (5.1)
	public static final int BASS_SPEAKER_SIDE = 0x4000000;	// side speakers (7.1)
	public static int BASS_SPEAKER_N(int n) { return n<<24; }	// n'th pair of speakers (max 15)
	public static final int BASS_SPEAKER_LEFT = 0x10000000;	// modifier: left
	public static final int BASS_SPEAKER_RIGHT = 0x20000000;	// modifier: right
	public static final int BASS_SPEAKER_FRONTLEFT = BASS_SPEAKER_FRONT | BASS_SPEAKER_LEFT;
	public static final int BASS_SPEAKER_FRONTRIGHT = BASS_SPEAKER_FRONT | BASS_SPEAKER_RIGHT;
	public static final int BASS_SPEAKER_REARLEFT = BASS_SPEAKER_REAR | BASS_SPEAKER_LEFT;
	public static final int BASS_SPEAKER_REARRIGHT = BASS_SPEAKER_REAR | BASS_SPEAKER_RIGHT;
	public static final int BASS_SPEAKER_CENTER = BASS_SPEAKER_CENLFE | BASS_SPEAKER_LEFT;
	public static final int BASS_SPEAKER_LFE = BASS_SPEAKER_CENLFE | BASS_SPEAKER_RIGHT;
	public static final int BASS_SPEAKER_SIDELEFT = BASS_SPEAKER_SIDE | BASS_SPEAKER_LEFT;
	public static final int BASS_SPEAKER_SIDERIGHT = BASS_SPEAKER_SIDE | BASS_SPEAKER_RIGHT;
	public static final int BASS_SPEAKER_REAR2 = BASS_SPEAKER_SIDE;
	public static final int BASS_SPEAKER_REAR2LEFT = BASS_SPEAKER_SIDELEFT;
	public static final int BASS_SPEAKER_REAR2RIGHT = BASS_SPEAKER_SIDERIGHT;

	public static final int BASS_ASYNCFILE = 0x40000000;	// read file asynchronously

	public static final int BASS_RECORD_PAUSE = 0x8000;	// start recording paused

	// Channel info structure
	public static class BASS_CHANNELINFO {
		public int freq;		// default playback rate
		public int chans;	// channels
		public int flags;
		public int ctype;	// type of channel
		public int origres;	// original resolution
		public int plugin;
		public int sample;
		public String filename;
	}

	public static final int BASS_ORIGRES_FLOAT = 0x10000;

	// BASS_CHANNELINFO types
	public static final int BASS_CTYPE_SAMPLE = 1;
	public static final int BASS_CTYPE_RECORD = 2;
	public static final int BASS_CTYPE_STREAM = 0x10000;
	public static final int BASS_CTYPE_STREAM_VORBIS = 0x10002;
	public static final int BASS_CTYPE_STREAM_OGG = 0x10002;
	public static final int BASS_CTYPE_STREAM_MP1 = 0x10003;
	public static final int BASS_CTYPE_STREAM_MP2 = 0x10004;
	public static final int BASS_CTYPE_STREAM_MP3 = 0x10005;
	public static final int BASS_CTYPE_STREAM_AIFF = 0x10006;
	public static final int BASS_CTYPE_STREAM_CA = 0x10007;
	public static final int BASS_CTYPE_STREAM_MF = 0x10008;
	public static final int BASS_CTYPE_STREAM_AM = 0x10009;
	public static final int BASS_CTYPE_STREAM_SAMPLE = 0x1000a;
	public static final int BASS_CTYPE_STREAM_DUMMY = 0x18000;
	public static final int BASS_CTYPE_STREAM_DEVICE = 0x18001;
	public static final int BASS_CTYPE_STREAM_WAV = 0x40000; // WAVE flag (LOWORD=codec)
	public static final int BASS_CTYPE_STREAM_WAV_PCM = 0x50001;
	public static final int BASS_CTYPE_STREAM_WAV_FLOAT = 0x50003;
	public static final int BASS_CTYPE_MUSIC_MOD = 0x20000;
	public static final int BASS_CTYPE_MUSIC_MTM = 0x20001;
	public static final int BASS_CTYPE_MUSIC_S3M = 0x20002;
	public static final int BASS_CTYPE_MUSIC_XM = 0x20003;
	public static final int BASS_CTYPE_MUSIC_IT = 0x20004;
	public static final int BASS_CTYPE_MUSIC_MO3 = 0x00100; // MO3 flag

	public static class BASS_PLUGINFORM {
		public int ctype;		// channel type
		public String name;	// format description
		public String exts;	// file extension filter (*.ext1;*.ext2;etc...)
	}

	public static class BASS_PLUGININFO {
		public int version;					// version (same form as BASS_GetVersion)
		public int formatc;					// number of formats
		public BASS_PLUGINFORM[] formats;	// the array of formats
	}

	// 3D vector (for 3D positions/velocities/orientations)
	public static class BASS_3DVECTOR {
		public BASS_3DVECTOR() {}
		public BASS_3DVECTOR(float _x, float _y, float _z) { x=_x; y=_y; z=_z; }
		public float x;	// +=right, -=left
		public float y;	// +=up, -=down
		public float z;	// +=front, -=behind
	}

	// 3D channel modes
	public static final int BASS_3DMODE_NORMAL = 0;	// normal 3D processing
	public static final int BASS_3DMODE_RELATIVE = 1;	// position is relative to the listener
	public static final int BASS_3DMODE_OFF = 2;	// no 3D processing

	// software 3D mixing algorithms (used with BASS_CONFIG_3DALGORITHM)
	public static final int BASS_3DALG_DEFAULT = 0;
	public static final int BASS_3DALG_OFF = 1;
	public static final int BASS_3DALG_FULL = 2;
	public static final int BASS_3DALG_LIGHT = 3;

	// BASS_SampleGetChannel flags
	public static final int BASS_SAMCHAN_NEW = 1;	    // get a new playback channel
	public static final int BASS_SAMCHAN_STREAM = 2;	// create a stream

	public interface STREAMPROC
	{
		int STREAMPROC(int handle, ByteBuffer buffer, int length, Object user);
		/* User stream callback function.
		handle : The stream that needs writing
		buffer : Buffer to write the samples in
		length : Number of bytes to write
		user   : The 'user' parameter value given when calling BASS_StreamCreate
		RETURN : Number of bytes written. Set the BASS_STREAMPROC_END flag to end
				 the stream. */
	}

	public static final int BASS_STREAMPROC_END = 0x80000000;	// end of user stream flag

	// Special STREAMPROCs
	public static final int STREAMPROC_DUMMY = 0;		// "dummy" stream
	public static final int STREAMPROC_PUSH = -1;		// push stream
	public static final int STREAMPROC_DEVICE = -2;		// device mix stream
	public static final int STREAMPROC_DEVICE_3D = -3;	// device 3D mix stream

	// BASS_StreamCreateFileUser file systems
	public static final int STREAMFILE_NOBUFFER = 0;
	public static final int STREAMFILE_BUFFER = 1;
	public static final int STREAMFILE_BUFFERPUSH = 2;

	public interface BASS_FILEPROCS
	{
		// User file stream callback functions
		void FILECLOSEPROC(Object user);
		long FILELENPROC(Object user) throws IOException;
		int FILEREADPROC(ByteBuffer buffer, int length, Object user);
		boolean FILESEEKPROC(long offset, Object user);
	}

	// BASS_StreamPutFileData options
	public static final int BASS_FILEDATA_END = 0;	// end & close the file

	// BASS_StreamGetFilePosition modes
	public static final int BASS_FILEPOS_CURRENT = 0;
	public static final int BASS_FILEPOS_DECODE = BASS_FILEPOS_CURRENT;
	public static final int BASS_FILEPOS_DOWNLOAD = 1;
	public static final int BASS_FILEPOS_END = 2;
	public static final int BASS_FILEPOS_START = 3;
	public static final int BASS_FILEPOS_CONNECTED = 4;
	public static final int BASS_FILEPOS_BUFFER = 5;
	public static final int BASS_FILEPOS_SOCKET = 6;
	public static final int BASS_FILEPOS_ASYNCBUF = 7;
	public static final int BASS_FILEPOS_SIZE = 8;
	public static final int BASS_FILEPOS_BUFFERING = 9;
	public static final int BASS_FILEPOS_AVAILABLE = 10;

	public interface DOWNLOADPROC
	{
		void DOWNLOADPROC(ByteBuffer buffer, int length, Object user);
		/* Internet stream download callback function.
		buffer : Buffer containing the downloaded data... NULL=end of download
		length : Number of bytes in the buffer
		user   : The 'user' parameter value given when calling BASS_StreamCreateURL */
	}

	// BASS_ChannelSetSync types
	public static final int BASS_SYNC_POS = 0;
	public static final int BASS_SYNC_END = 2;
	public static final int BASS_SYNC_META = 4;
	public static final int BASS_SYNC_SLIDE = 5;
	public static final int BASS_SYNC_STALL = 6;
	public static final int BASS_SYNC_DOWNLOAD = 7;
	public static final int BASS_SYNC_FREE = 8;
	public static final int BASS_SYNC_SETPOS = 11;
	public static final int BASS_SYNC_MUSICPOS = 10;
	public static final int BASS_SYNC_MUSICINST = 1;
	public static final int BASS_SYNC_MUSICFX = 3;
	public static final int BASS_SYNC_OGG_CHANGE = 12;
	public static final int BASS_SYNC_DEV_FAIL = 14;
	public static final int BASS_SYNC_DEV_FORMAT = 15;
	public static final int BASS_SYNC_THREAD = 0x20000000;	// flag: call sync in other thread
	public static final int BASS_SYNC_MIXTIME = 0x40000000;	// flag: sync at mixtime, else at playtime
	public static final int BASS_SYNC_ONETIME = 0x80000000;	// flag: sync only once, else continuously

	public interface SYNCPROC
	{
		void SYNCPROC(int handle, int channel, int data, Object user);
		/* Sync callback function.
		handle : The sync that has occured
		channel: Channel that the sync occured in
		data   : Additional data associated with the sync's occurance
		user   : The 'user' parameter given when calling BASS_ChannelSetSync */
	}

	public interface DSPPROC
	{
		void DSPPROC(int handle, int channel, ByteBuffer buffer, int length, Object user);
		/* DSP callback function.
		handle : The DSP handle
		channel: Channel that the DSP is being applied to
		buffer : Buffer to apply the DSP to
		length : Number of bytes in the buffer
		user   : The 'user' parameter given when calling BASS_ChannelSetDSP */
	}

	public interface RECORDPROC
	{
		boolean RECORDPROC(int handle, ByteBuffer buffer, int length, Object user);
		/* Recording callback function.
		handle : The recording handle
		buffer : Buffer containing the recorded sample data
		length : Number of bytes
		user   : The 'user' parameter value given when calling BASS_RecordStart
		RETURN : true = continue recording, false = stop */
	}

	// BASS_ChannelIsActive return values
	public static final int BASS_ACTIVE_STOPPED = 0;
	public static final int BASS_ACTIVE_PLAYING =1;
	public static final int BASS_ACTIVE_STALLED = 2;
	public static final int BASS_ACTIVE_PAUSED = 3;
	public static final int BASS_ACTIVE_PAUSED_DEVICE = 4;

	// Channel attributes
	public static final int BASS_ATTRIB_FREQ = 1;
	public static final int BASS_ATTRIB_VOL = 2;
	public static final int BASS_ATTRIB_PAN = 3;
	public static final int BASS_ATTRIB_EAXMIX = 4;
	public static final int BASS_ATTRIB_NOBUFFER = 5;
	public static final int BASS_ATTRIB_VBR = 6;
	public static final int BASS_ATTRIB_CPU = 7;
	public static final int BASS_ATTRIB_SRC = 8;
	public static final int BASS_ATTRIB_NET_RESUME = 9;
	public static final int BASS_ATTRIB_SCANINFO = 10;
	public static final int BASS_ATTRIB_NORAMP = 11;
	public static final int BASS_ATTRIB_BITRATE = 12;
	public static final int BASS_ATTRIB_BUFFER = 13;
	public static final int BASS_ATTRIB_GRANULE = 14;
	public static final int BASS_ATTRIB_USER = 15;
	public static final int BASS_ATTRIB_TAIL = 16;
	public static final int BASS_ATTRIB_PUSH_LIMIT = 17;
	public static final int BASS_ATTRIB_DOWNLOADPROC = 18;
	public static final int BASS_ATTRIB_VOLDSP = 19;
	public static final int BASS_ATTRIB_VOLDSP_PRIORITY = 20;
	public static final int BASS_ATTRIB_MUSIC_AMPLIFY = 0x100;
	public static final int BASS_ATTRIB_MUSIC_PANSEP = 0x101;
	public static final int BASS_ATTRIB_MUSIC_PSCALER = 0x102;
	public static final int BASS_ATTRIB_MUSIC_BPM = 0x103;
	public static final int BASS_ATTRIB_MUSIC_SPEED = 0x104;
	public static final int BASS_ATTRIB_MUSIC_VOL_GLOBAL = 0x105;
	public static final int BASS_ATTRIB_MUSIC_VOL_CHAN = 0x200; // + channel #
	public static final int BASS_ATTRIB_MUSIC_VOL_INST = 0x300; // + instrument #

	// BASS_ChannelSlideAttribute flags
	public static final int BASS_SLIDE_LOG = 0x1000000;

	// BASS_ChannelGetData flags
	public static final int BASS_DATA_AVAILABLE = 0;			// query how much data is buffered
	public static final int BASS_DATA_NOREMOVE = 0x10000000;	// flag: don't remove data from recording buffer
	public static final int BASS_DATA_FIXED = 0x20000000;	// unused
	public static final int BASS_DATA_FLOAT = 0x40000000;	// flag: return floating-point sample data
	public static final int BASS_DATA_FFT256 = 0x80000000;	// 256 sample FFT
	public static final int BASS_DATA_FFT512 = 0x80000001;	// 512 FFT
	public static final int BASS_DATA_FFT1024 = 0x80000002;	// 1024 FFT
	public static final int BASS_DATA_FFT2048 = 0x80000003;	// 2048 FFT
	public static final int BASS_DATA_FFT4096 = 0x80000004;	// 4096 FFT
	public static final int BASS_DATA_FFT8192 = 0x80000005;	// 8192 FFT
	public static final int BASS_DATA_FFT16384 = 0x80000006;	// 16384 FFT
	public static final int BASS_DATA_FFT32768 = 0x80000007;	// 32768 FFT
	public static final int BASS_DATA_FFT_INDIVIDUAL = 0x10;	// FFT flag: FFT for each channel, else all combined
	public static final int BASS_DATA_FFT_NOWINDOW = 0x20;	// FFT flag: no Hanning window
	public static final int BASS_DATA_FFT_REMOVEDC = 0x40;	// FFT flag: pre-remove DC bias
	public static final int BASS_DATA_FFT_COMPLEX = 0x80;	// FFT flag: return complex data
	public static final int BASS_DATA_FFT_NYQUIST = 0x100;	// FFT flag: return extra Nyquist value

	// BASS_ChannelGetLevelEx flags
	public static final int BASS_LEVEL_MONO = 1;	    // get mono level
	public static final int BASS_LEVEL_STEREO = 2;	    // get stereo level
	public static final int BASS_LEVEL_RMS = 4;	        // get RMS levels
	public static final int BASS_LEVEL_VOLPAN = 8;	    // apply VOL/PAN attributes to the levels
	public static final int BASS_LEVEL_NOREMOVE = 16;	// don't remove data from recording buffer

	// BASS_ChannelGetTags types : what's returned
	public static final int BASS_TAG_ID3 = 0;	// ID3v1 tags : TAG_ID3
	public static final int BASS_TAG_ID3V2 = 1;	// ID3v2 tags : ByteBuffer
	public static final int BASS_TAG_OGG = 2;	// OGG comments : String array
	public static final int BASS_TAG_HTTP = 3;	// HTTP headers : String array
	public static final int BASS_TAG_ICY = 4;	// ICY headers : String array
	public static final int BASS_TAG_META = 5;	// ICY metadata : String
	public static final int BASS_TAG_APE = 6;	// APE tags : String array
	public static final int BASS_TAG_MP4 = 7;	// MP4/iTunes metadata : String array
	public static final int BASS_TAG_VENDOR = 9;	// OGG encoder : String
	public static final int BASS_TAG_LYRICS3 = 10;	// Lyric3v2 tag : String
	public static final int BASS_TAG_WAVEFORMAT = 14;	// WAVE format : ByteBuffer containing WAVEFORMATEEX structure
	public static final int BASS_TAG_AM_NAME = 16;	// Android Media codec name : String
	public static final int BASS_TAG_ID3V2_2 = 17;	// ID3v2 tags (2nd block) : ByteBuffer
	public static final int BASS_TAG_AM_MIME = 18;	// Android Media MIME type : String
	public static final int BASS_TAG_LOCATION = 19;	// redirected URL : String
	public static final int BASS_TAG_RIFF_INFO = 0x100; // RIFF "INFO" tags : String array
	public static final int BASS_TAG_RIFF_BEXT = 0x101; // RIFF/BWF "bext" tags : TAG_BEXT
	public static final int BASS_TAG_RIFF_CART = 0x102; // RIFF/BWF "cart" tags : TAG_CART
	public static final int BASS_TAG_RIFF_DISP = 0x103; // RIFF "DISP" text tag : String
	public static final int BASS_TAG_RIFF_CUE = 0x104; // RIFF "cue " chunk : TAG_CUE structure
	public static final int BASS_TAG_RIFF_SMPL = 0x105; // RIFF "smpl" chunk : TAG_SMPL structure
	public static final int BASS_TAG_APE_BINARY = 0x1000;	// + index #, binary APE tag : TAG_APE_BINARY
	public static final int BASS_TAG_MUSIC_NAME = 0x10000;	// MOD music name : String
	public static final int BASS_TAG_MUSIC_MESSAGE = 0x10001;	// MOD message : String
	public static final int BASS_TAG_MUSIC_ORDERS = 0x10002;	// MOD order list : ByteBuffer
	public static final int BASS_TAG_MUSIC_AUTH = 0x10003;	// MOD author : UTF-8 string
	public static final int BASS_TAG_MUSIC_INST = 0x10100;	// + instrument #, MOD instrument name : String
	public static final int BASS_TAG_MUSIC_CHAN = 0x10200;	// + channel #, MOD channel name : String
	public static final int BASS_TAG_MUSIC_SAMPLE = 0x10300;	// + sample #, MOD sample name : String
	public static final int BASS_TAG_BYTEBUFFER = 0x10000000;	// flag: return a ByteBuffer instead of a String or TAG_ID3

	// ID3v1 tag structure
	public static class TAG_ID3 {
		public String id;
		public String title;
		public String artist;
		public String album;
		public String year;
		public String comment;
		public byte genre;
		public byte track;
	}

	// Binary APE tag structure
	public static class TAG_APE_BINARY {
		public String key;
		public ByteBuffer data;
		public int length;
	}

	// BASS_ChannelGetLength/GetPosition/SetPosition modes
	public static final int BASS_POS_BYTE = 0;		// byte position
	public static final int BASS_POS_MUSIC_ORDER = 1;		// order.row position, MAKELONG(order,row)
	public static final int BASS_POS_OGG = 3;		// OGG bitstream number
	public static final int BASS_POS_END = 0x10;	// trimmed end position
	public static final int BASS_POS_LOOP = 0x11;	// loop start positiom
	public static final int BASS_POS_FLUSH = 0x1000000; // flag: flush decoder/FX buffers
	public static final int BASS_POS_RESET = 0x2000000; // flag: reset user file buffers
	public static final int BASS_POS_RELATIVE = 0x4000000; // flag: seek relative to the current position
	public static final int BASS_POS_INEXACT = 0x8000000; // flag: allow seeking to inexact position
	public static final int BASS_POS_DECODE = 0x10000000; // flag: get the decoding (not playing) position
	public static final int BASS_POS_DECODETO = 0x20000000; // flag: decode to the position instead of seeking
	public static final int BASS_POS_SCAN = 0x40000000; // flag: scan to the position

	// BASS_ChannelSetDevice/GetDevice option
	public static final int BASS_NODEVICE = 0x20000;

	// DX8 effect types, use with BASS_ChannelSetFX
	public static final int BASS_FX_DX8_CHORUS = 0;
	public static final int BASS_FX_DX8_COMPRESSOR = 1;
	public static final int BASS_FX_DX8_DISTORTION = 2;
	public static final int BASS_FX_DX8_ECHO = 3;
	public static final int BASS_FX_DX8_FLANGER = 4;
	public static final int BASS_FX_DX8_GARGLE = 5;
	public static final int BASS_FX_DX8_I3DL2REVERB = 6;
	public static final int BASS_FX_DX8_PARAMEQ = 7;
	public static final int BASS_FX_DX8_REVERB = 8;
	public static final int BASS_FX_VOLUME = 9;

	public static class BASS_DX8_CHORUS {
		public float fWetDryMix;
		public float fDepth;
		public float fFeedback;
		public float fFrequency;
		public int lWaveform;	// 0=triangle, 1=sine
		public float fDelay;
		public int lPhase;		// BASS_DX8_PHASE_xxx
	}

	public static class BASS_DX8_DISTORTION {
		public float fGain;
		public float fEdge;
		public float fPostEQCenterFrequency;
		public float fPostEQBandwidth;
		public float fPreLowpassCutoff;
	}

	public static class BASS_DX8_ECHO {
		public float fWetDryMix;
		public float fFeedback;
		public float fLeftDelay;
		public float fRightDelay;
		public boolean lPanDelay;
	}

	public static class BASS_DX8_FLANGER {
		public float fWetDryMix;
		public float fDepth;
		public float fFeedback;
		public float fFrequency;
		public int lWaveform;	// 0=triangle, 1=sine
		public float fDelay;
		public int lPhase;		// BASS_DX8_PHASE_xxx
	}

	public static class BASS_DX8_PARAMEQ {
		public float fCenter;
		public float fBandwidth;
		public float fGain;
	}

	public static class BASS_DX8_REVERB {
		public float fInGain;
		public float fReverbMix;
		public float fReverbTime;
		public float fHighFreqRTRatio;
	}

	public static final int BASS_DX8_PHASE_NEG_180 = 0;
	public static final int BASS_DX8_PHASE_NEG_90 = 1;
	public static final int BASS_DX8_PHASE_ZERO = 2;
	public static final int BASS_DX8_PHASE_90 = 3;
	public static final int BASS_DX8_PHASE_180 = 4;

	public static class BASS_FX_VOLUME_PARAM {
		public float fTarget;
		public float fCurrent;
		public float fTime;
		public int lCurve;
	}

	public static class Asset {
		public Asset() {}
		public Asset(AssetManager m, String f) { manager=m; file=f; }
		public AssetManager manager;
		public String file;
	}

	public static class FloatValue {
		public float value;
	}

	public static native boolean BASS_SetConfig(int option, int value);
	public static native int BASS_GetConfig(int option);
	public static native boolean BASS_SetConfigPtr(int option, Object value);
	public static native Object BASS_GetConfigPtr(int option);
	public static native int BASS_GetVersion();
	public static native int BASS_ErrorGetCode();
	public static native boolean BASS_GetDeviceInfo(int device, BASS_DEVICEINFO info);
	public static native boolean BASS_Init(int device, int freq, int flags);
	public static native boolean BASS_Free();
	public static native boolean BASS_SetDevice(int device);
	public static native int BASS_GetDevice();
	public static native boolean BASS_GetInfo(BASS_INFO info);
	public static native boolean BASS_Start();
	public static native boolean BASS_Stop();
	public static native boolean BASS_Pause();
	public static native int BASS_IsStarted();
	public static native boolean BASS_Update(int length);
	public static native float BASS_GetCPU();
	public static native boolean BASS_SetVolume(float volume);
	public static native float BASS_GetVolume();

	public static native boolean BASS_Set3DFactors(float distf, float rollf, float doppf);
	public static native boolean BASS_Get3DFactors(FloatValue distf, FloatValue rollf, FloatValue doppf);
	public static native boolean BASS_Set3DPosition(BASS_3DVECTOR pos, BASS_3DVECTOR vel, BASS_3DVECTOR front, BASS_3DVECTOR top);
	public static native boolean BASS_Get3DPosition(BASS_3DVECTOR pos, BASS_3DVECTOR vel, BASS_3DVECTOR front, BASS_3DVECTOR top);
	public static native void BASS_Apply3D();

	public static native int BASS_PluginLoad(String file, int flags);
	public static native boolean BASS_PluginFree(int handle);
	public static native boolean BASS_PluginEnable(int handle, boolean enable);
	public static native BASS_PLUGININFO BASS_PluginGetInfo(int handle);

	public static native int BASS_SampleLoad(String file, long offset, int length, int max, int flags);
	public static native int BASS_SampleLoad(ByteBuffer file, long offset, int length, int max, int flags);
	public static native int BASS_SampleLoad(Asset file, long offset, int length, int max, int flags);
	public static native int BASS_SampleLoad(ParcelFileDescriptor file, long offset, int length, int max, int flags);
	public static native int BASS_SampleCreate(int length, int freq, int chans, int max, int flags);
	public static native boolean BASS_SampleFree(int handle);
	public static native boolean BASS_SampleSetData(int handle, ByteBuffer buffer);
	public static native boolean BASS_SampleGetData(int handle, ByteBuffer buffer);
	public static native boolean BASS_SampleGetInfo(int handle, BASS_SAMPLE info);
	public static native boolean BASS_SampleSetInfo(int handle, BASS_SAMPLE info);
	public static native int BASS_SampleGetChannel(int handle, boolean onlynew);
	public static native int BASS_SampleGetChannels(int handle, int[] channels);
	public static native boolean BASS_SampleStop(int handle);

	public static native int BASS_StreamCreate(int freq, int chans, int flags, STREAMPROC proc, Object user);
	public static native int BASS_StreamCreateFile(String file, long offset, long length, int flags);
	public static native int BASS_StreamCreateFile(ByteBuffer file, long offset, long length, int flags);
	public static native int BASS_StreamCreateFile(ParcelFileDescriptor file, long offset, long length, int flags);
	public static native int BASS_StreamCreateFile(Asset asset, long offset, long length, int flags);
	public static native int BASS_StreamCreateURL(String url, int offset, int flags, DOWNLOADPROC proc, Object user);
	public static native int BASS_StreamCreateFileUser(int system, int flags, BASS_FILEPROCS procs, Object user);
	public static native boolean BASS_StreamFree(int handle);
	public static native long BASS_StreamGetFilePosition(int handle, int mode);
	public static native int BASS_StreamPutData(int handle, ByteBuffer buffer, int length);
	public static native int BASS_StreamPutFileData(int handle, ByteBuffer buffer, int length);

	public static native int BASS_MusicLoad(String file, long offset, int length, int flags, int freq);
	public static native int BASS_MusicLoad(ByteBuffer file, long offset, int length, int flags, int freq);
	public static native int BASS_MusicLoad(Asset asset, long offset, int length, int flags, int freq);
	public static native int BASS_MusicLoad(ParcelFileDescriptor asset, long offset, int length, int flags, int freq);
	public static native boolean BASS_MusicFree(int handle);

	public static native boolean BASS_RecordGetDeviceInfo(int device, BASS_DEVICEINFO info);
	public static native boolean BASS_RecordInit(int device);
	public static native boolean BASS_RecordFree();
	public static native boolean BASS_RecordSetDevice(int device);
	public static native int BASS_RecordGetDevice();
	public static native boolean BASS_RecordGetInfo(BASS_RECORDINFO info);
	public static native String BASS_RecordGetInputName(int input);
	public static native boolean BASS_RecordSetInput(int input, int flags, float volume);
	public static native int BASS_RecordGetInput(int input, FloatValue volume);
	public static native int BASS_RecordStart(int freq, int chans, int flags, RECORDPROC proc, Object user);

	public static native double BASS_ChannelBytes2Seconds(int handle, long pos);
	public static native long BASS_ChannelSeconds2Bytes(int handle, double pos);
	public static native int BASS_ChannelGetDevice(int handle);
	public static native boolean BASS_ChannelSetDevice(int handle, int device);
	public static native int BASS_ChannelIsActive(int handle);
	public static native boolean BASS_ChannelGetInfo(int handle, BASS_CHANNELINFO info);
	public static native Object BASS_ChannelGetTags(int handle, int tags);
	public static native long BASS_ChannelFlags(int handle, int flags, int mask);
	public static native boolean BASS_ChannelLock(int handle, boolean lock);
	public static native boolean BASS_ChannelFree(int handle);
	public static native boolean BASS_ChannelPlay(int handle, boolean restart);
	public static native boolean BASS_ChannelStart(int handle);
	public static native boolean BASS_ChannelStop(int handle);
	public static native boolean BASS_ChannelPause(int handle);
	public static native boolean BASS_ChannelUpdate(int handle, int length);
	public static native boolean BASS_ChannelSetAttribute(int handle, int attrib, float value);
	public static native boolean BASS_ChannelGetAttribute(int handle, int attrib, FloatValue value);
	public static native boolean BASS_ChannelSlideAttribute(int handle, int attrib, float value, int time);
	public static native boolean BASS_ChannelIsSliding(int handle, int attrib);
	public static native boolean BASS_ChannelSetAttributeEx(int handle, int attrib, ByteBuffer value, int size);
	public static native boolean BASS_ChannelSetAttributeDOWNLOADPROC(int handle, DOWNLOADPROC proc, Object user);
	public static native int BASS_ChannelGetAttributeEx(int handle, int attrib, ByteBuffer value, int size);
	public static native boolean BASS_ChannelSet3DAttributes(int handle, int mode, float min, float max, int iangle, int oangle, float outvol);
	public static native boolean BASS_ChannelGet3DAttributes(int handle, Integer mode, FloatValue min, FloatValue max, Integer iangle, Integer oangle, FloatValue outvol);
	public static native boolean BASS_ChannelSet3DPosition(int handle, BASS_3DVECTOR pos, BASS_3DVECTOR orient, BASS_3DVECTOR vel);
	public static native boolean BASS_ChannelGet3DPosition(int handle, BASS_3DVECTOR pos, BASS_3DVECTOR orient, BASS_3DVECTOR vel);
	public static native long BASS_ChannelGetLength(int handle, int mode);
	public static native boolean BASS_ChannelSetPosition(int handle, long pos, int mode);
	public static native long BASS_ChannelGetPosition(int handle, int mode);
	public static native int BASS_ChannelGetLevel(int handle);
	public static native boolean BASS_ChannelGetLevelEx(int handle, float[] levels, float length, int flags);
	public static native int BASS_ChannelGetData(int handle, ByteBuffer buffer, int length);
	public static native int BASS_ChannelSetSync(int handle, int type, long param, SYNCPROC proc, Object user);
	public static native boolean BASS_ChannelRemoveSync(int handle, int sync);
	public static native boolean BASS_ChannelSetLink(int handle, int chan);
	public static native boolean BASS_ChannelRemoveLink(int handle, int chan);
	public static native int BASS_ChannelSetDSP(int handle, DSPPROC proc, Object user, int priority);
	public static native boolean BASS_ChannelRemoveDSP(int handle, int dsp);
	public static native int BASS_ChannelSetFX(int handle, int type, int priority);
	public static native boolean BASS_ChannelRemoveFX(int handle, int fx);

	public static native boolean BASS_FXSetParameters(int handle, Object params);
	public static native boolean BASS_FXGetParameters(int handle, Object params);
	public static native boolean BASS_FXSetPriority(int handle, int priority);
	public static native boolean BASS_FXReset(int handle);

	static native int BASS_StreamCreateConst(int freq, int chans, int flags, int proc, Object user);
	public static int BASS_StreamCreate(int freq, int chans, int flags, int proc, Object user) {
		return BASS_StreamCreateConst(freq, chans, flags, proc, user);
	}
	
	public static class Utils {
		public static int LOBYTE(int n) { return n&0xff; }
		public static int HIBYTE(int n) { return (n>>8)&0xff; }
		public static int LOWORD(int n) { return n&0xffff; }
		public static int HIWORD(int n) { return (n>>16)&0xffff; }
		public static int MAKEWORD(int a, int b) { return (a&0xff)|((b&0xff)<<8); }
		public static int MAKELONG(int a, int b) { return (a&0xffff)|(b<<16); }
	}

    static {
        System.loadLibrary("bass");
    }
}
