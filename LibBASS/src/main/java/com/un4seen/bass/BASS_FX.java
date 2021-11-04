/*===========================================================================
 BASS_FX 2.4 - Copyright (c) 2002-2018 (: JOBnik! :) [Arthur Aminov, ISRAEL]
                                                     [http://www.jobnik.org]

      bugs/suggestions/questions:
        forum  : http://www.un4seen.com/forum/?board=1
                 http://www.jobnik.org/forums
        e-mail : bass_fx@jobnik.org
     --------------------------------------------------

 NOTE: This header will work only with BASS_FX version 2.4.12
       Check www.un4seen.com or www.jobnik.org for any later versions.

 * Requires BASS 2.4 (available at http://www.un4seen.com)
===========================================================================*/

package com.un4seen.bass;

public class BASS_FX
{
	// BASS_CHANNELINFO types
	public static final int BASS_CTYPE_STREAM_TEMPO = 0x1f200;
	public static final int BASS_CTYPE_STREAM_REVERSE = 0x1f201;

	// Tempo / Reverse / BPM / Beat flag
	public static final int BASS_FX_FREESOURCE = 0x10000;	// Free the source handle as well?

	// BASS_FX Version
	public static native int BASS_FX_GetVersion();

	/*===========================================================================
		DSP (Digital Signal Processing)
	===========================================================================*/
	
	/*
		Multi-channel order of each channel is as follows:
		 3 channels       left-front, right-front, center.
		 4 channels       left-front, right-front, left-rear/side, right-rear/side.
		 5 channels       left-front, right-front, center, left-rear/side, right-rear/side.
		 6 channels (5.1) left-front, right-front, center, LFE, left-rear/side, right-rear/side.
		 8 channels (7.1) left-front, right-front, center, LFE, left-rear/side, right-rear/side, left-rear center, right-rear center.
	*/

	// DSP channels flags
	public static final int BASS_BFX_CHANALL = -1;	// all channels at once (as by default)
	public static final int BASS_BFX_CHANNONE = 0;	// disable an effect for all channels
	public static final int BASS_BFX_CHAN1 = 1;		// left-front channel
	public static final int BASS_BFX_CHAN2 = 2;		// right-front channel
	public static final int BASS_BFX_CHAN3 = 4;		// see above info
	public static final int BASS_BFX_CHAN4 = 8;		// see above info
	public static final int BASS_BFX_CHAN5 = 16;	// see above info
	public static final int BASS_BFX_CHAN6 = 32;	// see above info
	public static final int BASS_BFX_CHAN7 = 64;	// see above info
	public static final int BASS_BFX_CHAN8 = 128;	// see above info

	// if you have more than 8 channels (7.1), use this function
	public static int BASS_BFX_CHANNEL_N(int n) { return (1<<((n)-1)); }

	// DSP effects
	public static final int BASS_FX_BFX_ROTATE = 0x10000;		// A channels volume ping-pong	/ multi channel
	public static final int BASS_FX_BFX_ECHO = 0x10001;			// Echo							/ 2 channels max	(deprecated)
	public static final int BASS_FX_BFX_FLANGER = 0x10002;		// Flanger						/ multi channel		(deprecated)
	public static final int BASS_FX_BFX_VOLUME = 0x10003;		// Volume						/ multi channel
	public static final int BASS_FX_BFX_PEAKEQ = 0x10004;		// Peaking Equalizer			/ multi channel
	public static final int BASS_FX_BFX_REVERB = 0x10005;		// Reverb						/ 2 channels max	(deprecated)
	public static final int BASS_FX_BFX_LPF = 0x10006;			// Low Pass Filter 24dB			/ multi channel		(deprecated)
	public static final int BASS_FX_BFX_MIX = 0x10007;			// Swap, remap and mix channels	/ multi channel
	public static final int BASS_FX_BFX_DAMP = 0x10008;			// Dynamic Amplification		/ multi channel
	public static final int BASS_FX_BFX_AUTOWAH = 0x10009;		// Auto Wah						/ multi channel
	public static final int BASS_FX_BFX_ECHO2 = 0x1000a;		// Echo 2						/ multi channel		(deprecated)
	public static final int BASS_FX_BFX_PHASER = 0x1000b;		// Phaser						/ multi channel
	public static final int BASS_FX_BFX_ECHO3 = 0x1000c;		// Echo 3						/ multi channel		(deprecated)
	public static final int BASS_FX_BFX_CHORUS = 0x1000d;		// Chorus/Flanger				/ multi channel
	public static final int BASS_FX_BFX_APF = 0x1000e;			// All Pass Filter				/ multi channel		(deprecated)
	public static final int BASS_FX_BFX_COMPRESSOR = 0x1000f;	// Compressor					/ multi channel		(deprecated)
	public static final int BASS_FX_BFX_DISTORTION = 0x10010;	// Distortion					/ multi channel
	public static final int BASS_FX_BFX_COMPRESSOR2 = 0x10011;	// Compressor 2					/ multi channel
	public static final int BASS_FX_BFX_VOLUME_ENV = 0x10012;	// Volume envelope				/ multi channel
	public static final int BASS_FX_BFX_BQF = 0x10013;			// BiQuad filters				/ multi channel
	public static final int BASS_FX_BFX_ECHO4 = 0x10014;		// Echo 4						/ multi channel
	public static final int BASS_FX_BFX_PITCHSHIFT = 0x10015;	// Pitch shift using FFT		/ multi channel		(not available on mobile)
	public static final int BASS_FX_BFX_FREEVERB = 0x10016;		// Reverb using "Freeverb" algo	/ multi channel

	/*
	    Deprecated effects in 2.4.10 version:
		------------------------------------
		BASS_FX_BFX_ECHO		-> use BASS_FX_BFX_ECHO4
		BASS_FX_BFX_ECHO2		-> use BASS_FX_BFX_ECHO4
		BASS_FX_BFX_ECHO3		-> use BASS_FX_BFX_ECHO4
		BASS_FX_BFX_REVERB		-> use BASS_FX_BFX_FREEVERB
		BASS_FX_BFX_FLANGER		-> use BASS_FX_BFX_CHORUS
		BASS_FX_BFX_COMPRESSOR	-> use BASS_FX_BFX_COMPRESSOR2
		BASS_FX_BFX_APF			-> use BASS_FX_BFX_BQF with BASS_BFX_BQF_ALLPASS filter
		BASS_FX_BFX_LPF			-> use 2x BASS_FX_BFX_BQF with BASS_BFX_BQF_LOWPASS filter and appropriate fQ values
	*/

	// Rotate
	public static class BASS_BFX_ROTATE {
		public float fRate;						// rotation rate/speed in Hz (A negative rate can be used for reverse direction)
		public int	 lChannel;					// BASS_BFX_CHANxxx flag/s (supported only even number of channels)
	}

	// Echo (deprecated)
	public static class BASS_BFX_ECHO {
		public float fLevel;					// [0....1....n] linear
		public int   lDelay;					// [1200..30000]
	}

	// Flanger (deprecated)
	public static class BASS_BFX_FLANGER {
		public float fWetDry;					// [0....1....n] linear
		public float fSpeed;					// [0......0.09]
		public int   lChannel;					// BASS_BFX_CHANxxx flag/s
	}

	// Volume
	public static class BASS_BFX_VOLUME {
		public int	 lChannel;					// BASS_BFX_CHANxxx flag/s or 0 for global volume control
		public float fVolume;					// [0....1....n] linear
	}

	// Peaking Equalizer
	public static class BASS_BFX_PEAKEQ {
		public int   lBand;						// [0...............n] more bands means more memory & cpu usage
		public float fBandwidth;				// [0.1...........<10] in octaves - fQ is not in use (Bandwidth has a priority over fQ)
		public float fQ;						// [0...............1] the EE kinda definition (linear) (if Bandwidth is not in use)
		public float fCenter;					// [1Hz..<info.freq/2] in Hz
		public float fGain;						// [-15dB...0...+15dB] in dB (can be above/below these limits)
		public int   lChannel;					// BASS_BFX_CHANxxx flag/s
	}

	// Reverb (deprecated)
	public static class BASS_BFX_REVERB {
		public float fLevel;					// [0....1....n] linear
		public int   lDelay;					// [1200..10000]
	}

	// Low Pass Filter (deprecated)
	public static class BASS_BFX_LPF {
		public float fResonance;				// [0.01...........10]
		public float fCutOffFreq;				// [1Hz...info.freq/2] cutoff frequency
		public int   lChannel;					// BASS_BFX_CHANxxx flag/s
	}

	// Swap, remap and mix
	public static class BASS_BFX_MIX {
		public int[] lChannel;					// an array of channels to mix using BASS_BFX_CHANxxx flag/s (lChannel[0] is left channel...)
	}

	// Dynamic Amplification
	public static class BASS_BFX_DAMP {
		public float fTarget;					// target volume level						[0<......1] linear
		public float fQuiet; 					// quiet  volume level						[0.......1] linear
		public float fRate;						// amp adjustment rate						[0.......1] linear
		public float fGain;						// amplification level						[0...1...n] linear
		public float fDelay;					// delay in seconds before increasing level	[0.......n] linear
		public int   lChannel;					// BASS_BFX_CHANxxx flag/s
	}

	// Auto Wah
	public static class BASS_BFX_AUTOWAH {
		public float fDryMix;					// dry (unaffected) signal mix				[-2......2]
		public float fWetMix;					// wet (affected) signal mix				[-2......2]
		public float fFeedback;					// output signal to feed back into input	[-1......1]
		public float fRate;						// rate of sweep in cycles per second		[0<....<10]
		public float fRange;					// sweep range in octaves					[0<....<10]
		public float fFreq;						// base frequency of sweep Hz				[0<...1000]
		public int   lChannel;					// BASS_BFX_CHANxxx flag/s
	}

	// Echo 2 (deprecated)
	public static class BASS_BFX_ECHO2 {
		public float fDryMix;					// dry (unaffected) signal mix				[-2......2]
		public float fWetMix;					// wet (affected) signal mix				[-2......2]
		public float fFeedback;					// output signal to feed back into input	[-1......1]
		public float fDelay;					// delay sec								[0<......n]
		public int   lChannel;					// BASS_BFX_CHANxxx flag/s
	}

	// Phaser
	public static class BASS_BFX_PHASER {
		public float fDryMix;					// dry (unaffected) signal mix				[-2......2]
		public float fWetMix;					// wet (affected) signal mix				[-2......2]
		public float fFeedback;					// output signal to feed back into input	[-1......1]
		public float fRate;						// rate of sweep in cycles per second		[0<....<10]
		public float fRange;					// sweep range in octaves					[0<....<10]
		public float fFreq;						// base frequency of sweep					[0<...1000]
		public int   lChannel;					// BASS_BFX_CHANxxx flag/s
	}

	// Echo 3 (deprecated)
	public static class BASS_BFX_ECHO3 {
		public float fDryMix;					// dry (unaffected) signal mix				[-2......2]
		public float fWetMix;					// wet (affected) signal mix				[-2......2]
		public float fDelay;					// delay sec								[0<......n]
		public int   lChannel;					// BASS_BFX_CHANxxx flag/s
	}

	// Chorus/Flanger
	public static class BASS_BFX_CHORUS {
		public float fDryMix;					// dry (unaffected) signal mix				[-2......2]
		public float fWetMix;					// wet (affected) signal mix				[-2......2]
		public float fFeedback;					// output signal to feed back into input	[-1......1]
		public float fMinSweep;					// minimal delay ms							[0<...6000]
		public float fMaxSweep;					// maximum delay ms							[0<...6000]
		public float fRate;						// rate ms/s								[0<...1000]
		public int   lChannel;					// BASS_BFX_CHANxxx flag/s
	}

	// All Pass Filter (deprecated)
	public static class BASS_BFX_APF {
		public float fGain;						// reverberation time						[-1=<..<=1]
		public float fDelay;					// delay sec								[0<....<=n]
		public int   lChannel;					// BASS_BFX_CHANxxx flag/s
	}

	// Compressor (deprecated)
	public static class BASS_BFX_COMPRESSOR {
		public float fThreshold;				// compressor threshold						[0<=...<=1]
		public float fAttacktime;				// attack time ms							[0<.<=1000]
		public float fReleasetime;				// release time ms							[0<.<=5000]
		public int   lChannel;					// BASS_BFX_CHANxxx flag/s
	}

	// Distortion
	public static class BASS_BFX_DISTORTION {
		public float fDrive;					// distortion drive							[0<=...<=5]
		public float fDryMix;					// dry (unaffected) signal mix				[-5<=..<=5]
		public float fWetMix;					// wet (affected) signal mix				[-5<=..<=5]
		public float fFeedback;					// output signal to feed back into input	[-1<=..<=1]
		public float fVolume;					// distortion volume						[0=<...<=2]
		public int   lChannel;					// BASS_BFX_CHANxxx flag/s
	}

	// Compressor 2
	public static class BASS_BFX_COMPRESSOR2 {
		public float fGain;						// output gain of signal after compression	[-60....60] in dB
		public float fThreshold;				// point at which compression begins		[-60.....0] in dB
		public float fRatio;					// compression ratio						[1.......n]
		public float fAttack;					// attack time in ms						[0.01.1000]
		public float fRelease;					// release time in ms						[0.01.5000]
		public int   lChannel;					// BASS_BFX_CHANxxx flag/s
	}

	// Volume envelope
	public static class BASS_BFX_VOLUME_ENV {
		public int                 lChannel;	// BASS_BFX_CHANxxx flag/s
		public int                 lNodeCount;	// number of nodes
		public BASS_BFX_ENV_NODE[] pNodes;		// the nodes
		public boolean             bFollow;		// follow source position
	}

	public static class BASS_BFX_ENV_NODE {
		public double pos;						// node position in seconds (1st envelope node must be at position 0)
		public float  val;						// node value
	}

	// BiQuad Filters
	public static final int	BASS_BFX_BQF_LOWPASS = 0;
	public static final int	BASS_BFX_BQF_HIGHPASS = 1;
	public static final int	BASS_BFX_BQF_BANDPASS = 2;			// constant 0 dB peak gain
	public static final int	BASS_BFX_BQF_BANDPASS_Q = 3;		// constant skirt gain, peak gain = Q
	public static final int	BASS_BFX_BQF_NOTCH = 4;
	public static final int	BASS_BFX_BQF_ALLPASS = 5;
	public static final int	BASS_BFX_BQF_PEAKINGEQ = 6;
	public static final int	BASS_BFX_BQF_LOWSHELF = 7;
	public static final int	BASS_BFX_BQF_HIGHSHELF = 8;

	public static class BASS_BFX_BQF {
		public int   lFilter;					// BASS_BFX_BQF_xxx filter types
		public float fCenter;					// [1Hz..<info.freq/2] Cutoff (central) frequency in Hz
		public float fGain;						// [-15dB...0...+15dB] Used only for PEAKINGEQ and Shelving filters in dB (can be above/below these limits)
		public float fBandwidth;				// [0.1...........<10] Bandwidth in octaves (fQ is not in use (fBandwidth has a priority over fQ))
												// 						(between -3 dB frequencies for BANDPASS and NOTCH or between midpoint
												// 						(fGgain/2) gain frequencies for PEAKINGEQ)
		public float fQ;						// [0.1.............1] The EE kinda definition (linear) (if fBandwidth is not in use)
		public float fS;						// [0.1.............1] A "shelf slope" parameter (linear) (used only with Shelving filters)
												// 						when fS = 1, the shelf slope is as steep as you can get it and remain monotonically
												// 						increasing or decreasing gain with frequency.
		public int   lChannel;					// BASS_BFX_CHANxxx flag/s
	}

	// Echo 4
	public static class BASS_BFX_ECHO4 {
		public float   fDryMix;					// dry (unaffected) signal mix				[-2.......2]
		public float   fWetMix;					// wet (affected) signal mix				[-2.......2]
		public float   fFeedback;				// output signal to feed back into input	[-1.......1]
		public float   fDelay;					// delay sec								[0<.......n]
		public boolean bStereo;					// echo adjoining channels to each other	[TRUE/FALSE]
		public int     lChannel;				// BASS_BFX_CHANxxx flag/s
	}

	// Pitch shift (not available on mobile)
	public static class BASS_BFX_PITCHSHIFT {
		public float fPitchShift;				// A factor value which is between 0.5 (one octave down) and 2 (one octave up) (1 won't change the pitch) [1 default]
												// (fSemitones is not in use, fPitchShift has a priority over fSemitones)
		public float fSemitones;				// Semitones (0 won't change the pitch) [0 default]
		public int   lFFTsize;					// Defines the FFT frame size used for the processing. Typical values are 1024, 2048 and 4096 [2048 default]
												// It may be any value <= 8192 but it MUST be a power of 2
		public int   lOsamp;					// Is the STFT oversampling factor which also determines the overlap between adjacent STFT frames [8 default]
												// It should at least be 4 for moderate scaling ratios. A value of 32 is recommended for best quality (better quality = higher CPU usage)
		public int   lChannel;					// BASS_BFX_CHANxxx flag/s
	}

	// Freeverb
	public static final int	BASS_BFX_FREEVERB_MODE_FREEZE = 1;

	public static class BASS_BFX_FREEVERB {
		public float fDryMix;					// dry (unaffected) signal mix				[0........1], def. 0
		public float fWetMix;					// wet (affected) signal mix				[0........3], def. 1.0f
		public float fRoomSize;					// room size								[0........1], def. 0.5f
		public float fDamp;						// damping									[0........1], def. 0.5f
		public float fWidth;					// stereo width								[0........1], def. 1
		public int   lMode;						// 0 or BASS_BFX_FREEVERB_MODE_FREEZE, def. 0 (no freeze)
		public int   lChannel;					// BASS_BFX_CHANxxx flag/s
	}

	/*===========================================================================
		set dsp fx			- BASS_ChannelSetFX
		remove dsp fx		- BASS_ChannelRemoveFX
		set parameters		- BASS_FXSetParameters
		retrieve parameters - BASS_FXGetParameters
		reset the state		- BASS_FXReset
	===========================================================================*/

	/*===========================================================================
		Tempo, Pitch scaling and Sample rate changers
	===========================================================================*/
	
	// NOTE: Enable Tempo supported flags in BASS_FX_TempoCreate and the others to source handle.

	// tempo attributes (BASS_ChannelSet/GetAttribute)
	public static final int BASS_ATTRIB_TEMPO = 0x10000;
	public static final int BASS_ATTRIB_TEMPO_PITCH = 0x10001;
	public static final int BASS_ATTRIB_TEMPO_FREQ = 0x10002;

	// tempo attributes options
	public static final int BASS_ATTRIB_TEMPO_OPTION_USE_AA_FILTER = 0x10010;		// TRUE (default) / FALSE (default for multi-channel on mobile devices for lower CPU usage)
	public static final int BASS_ATTRIB_TEMPO_OPTION_AA_FILTER_LENGTH = 0x10011;	// 32 default (8 .. 128 taps)
	public static final int BASS_ATTRIB_TEMPO_OPTION_USE_QUICKALGO = 0x10012;		// TRUE (default on mobile devices for lower CPU usage) / FALSE (default)
	public static final int BASS_ATTRIB_TEMPO_OPTION_SEQUENCE_MS = 0x10013;			// 82 default, 0 = automatic
	public static final int BASS_ATTRIB_TEMPO_OPTION_SEEKWINDOW_MS = 0x10014;		// 28 default, 0 = automatic
	public static final int BASS_ATTRIB_TEMPO_OPTION_OVERLAP_MS = 0x10015;			// 8  default
	public static final int BASS_ATTRIB_TEMPO_OPTION_PREVENT_CLICK = 0x10016;		// TRUE / FALSE (default)
	// tempo algorithm flags
	public static final int BASS_FX_TEMPO_ALGO_LINEAR = 0x200;
	public static final int BASS_FX_TEMPO_ALGO_CUBIC = 0x400;						// default
	public static final int BASS_FX_TEMPO_ALGO_SHANNON = 0x800;

	public static native int   BASS_FX_TempoCreate(int chan, int flags);
	public static native int   BASS_FX_TempoGetSource(int chan);
	public static native float BASS_FX_TempoGetRateRatio(int chan);

	/*===========================================================================
		Reverse playback
	===========================================================================*/
	
	// NOTES: 1. MODs won't load without BASS_MUSIC_PRESCAN flag.
	//		  2. Enable Reverse supported flags in BASS_FX_ReverseCreate and the others to source handle.

	// reverse attribute (BASS_ChannelSet/GetAttribute)
	public static final int BASS_ATTRIB_REVERSE_DIR = 0x11000;

	// playback directions
	public static final int BASS_FX_RVS_REVERSE = -1;
	public static final int BASS_FX_RVS_FORWARD = 1;

	public static native int BASS_FX_ReverseCreate(int chan, float dec_block, int flags);
	public static native int BASS_FX_ReverseGetSource(int chan);

	/*===========================================================================
		BPM (Beats Per Minute)
	===========================================================================*/

	// bpm flags
	public static final int BASS_FX_BPM_BKGRND = 1;			// if in use, then you can do other processing while detection's in progress. Available only in Windows platforms (BPM/Beat)
	public static final int BASS_FX_BPM_MULT2 = 2;			// if in use, then will auto multiply bpm by 2 (if BPM < minBPM*2)

	// translation options (deprecated)
	public static final int BASS_FX_BPM_TRAN_X2 = 0;		// multiply the original BPM value by 2 (may be called only once & will change the original BPM as well!)
	public static final int BASS_FX_BPM_TRAN_2FREQ = 1;		// BPM value to Frequency
	public static final int BASS_FX_BPM_TRAN_FREQ2 = 2;		// Frequency to BPM value
	public static final int BASS_FX_BPM_TRAN_2PERCENT = 3;	// BPM value to Percents
	public static final int BASS_FX_BPM_TRAN_PERCENT2 = 4;	// Percents to BPM value

	public interface BPMPROC
	{
		void BPMPROC(int chan, float bpm, Object user);
	}

	public interface BPMPROGRESSPROC
	{
		void BPMPROGRESSPROC(int chan, float percent, Object user);
	}

	// back-compatibility
	public interface BPMPROCESSPROC
	{
		void BPMPROCESSPROC(int chan, float percent, Object user);
	}

	public static native float   BASS_FX_BPM_DecodeGet(int chan, double startSec, double endSec, int minMaxBPM, int flags, Object proc, Object user);
	public static native boolean BASS_FX_BPM_CallbackSet(int handle, BPMPROC proc, double period, int minMaxBPM, int flags, Object user);
	public static native boolean BASS_FX_BPM_CallbackReset(int handle);
	public static native float   BASS_FX_BPM_Translate(int handle, float val2tran, int trans);	// deprecated
	public static native boolean BASS_FX_BPM_Free(int handle);

	/*===========================================================================
		Beat position trigger
	===========================================================================*/

	public interface BPMBEATPROC
	{
		void BPMBEATPROC(int chan, double beatpos, Object user);
	}

	public static native boolean BASS_FX_BPM_BeatCallbackSet(int handle, BPMBEATPROC proc, Object user);
	public static native boolean BASS_FX_BPM_BeatCallbackReset(int handle);
	public static native boolean BASS_FX_BPM_BeatDecodeGet(int chan, double startSec, double endSec, int flags, BPMBEATPROC proc, Object user);
	public static native boolean BASS_FX_BPM_BeatSetParameters(int handle, float bandwidth, float centerfreq, float beat_rtime);
	public static native boolean BASS_FX_BPM_BeatGetParameters(int handle, Float bandwidth, Float centerfreq, Float beat_rtime);
	public static native boolean BASS_FX_BPM_BeatFree(int handle);

	/*===========================================================================
		Macros
	===========================================================================*/

	// translate linear level to logarithmic dB
	public static double BASS_BFX_Linear2dB(double level)
	{
		return (20*Math.log10(level));
	}

	// translate logarithmic dB level to linear
	public static double BASS_BFX_dB2Linear(double dB)
	{
		return (Math.pow(10,(dB)/20));
	}

    static {
        System.loadLibrary("bass_fx");
    }
}
