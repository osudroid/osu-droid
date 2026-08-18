package org.anddev.andengine.engine.handler.runnable;

import java.util.ArrayList;

import org.anddev.andengine.engine.handler.IUpdateHandler;

/**
 * (c) 2010 Nicolas Gramlich 
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 10:24:39 - 18.06.2010
 */
public class RunnableHandler implements IUpdateHandler {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	private ArrayList<Runnable> mRunnables = new ArrayList<Runnable>();
	// BEGIN osu!droid modified: add a spare buffer to swap mRunnables with when onUpdate() is running.
	// See the method for explanation.
	private ArrayList<Runnable> mRunnablesSwapBuffer = new ArrayList<Runnable>();
	// END osu!droid modified

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public synchronized void onUpdate(final float pSecondsElapsed) {
		final ArrayList<Runnable> runnables = this.mRunnables;

		// BEGIN osu!droid modified: swap in the empty buffer so Runnables posted by run() below (directly, or
		// indirectly via postRunnable()) land in the new mRunnables instead of the list we're about to iterate
		// and clear (which will cause the posted Runnables, which has not been run yet, to be cleared).
		this.mRunnables = this.mRunnablesSwapBuffer;
		this.mRunnablesSwapBuffer = runnables;
		// END osu!droid modified

		final int runnableCount = runnables.size();
		for(int i = runnableCount - 1; i >= 0; i--) {
			runnables.get(i).run();
		}
		runnables.clear();
	}

	@Override
	public void reset() {
		this.mRunnables.clear();
	}

	// ===========================================================
	// Methods
	// ===========================================================

	public synchronized void postRunnable(final Runnable pRunnable) {
		this.mRunnables.add(pRunnable);
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
