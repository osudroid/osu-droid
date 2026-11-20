package org.anddev.andengine.util.pool;

import java.util.ArrayList;

import org.anddev.andengine.engine.handler.IUpdateHandler;

/**
 * @author Valentin Milea
 * (c) 2010 Nicolas Gramlich 
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * 
 * @since 23:02:58 - 21.08.2010
 * @param <T>
 */
public abstract class PoolUpdateHandler<T extends PoolItem> implements IUpdateHandler {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	private final Pool<T> mPool;
	// BEGIN osu!droid modified - handle items outside the lock that posting waits for
	private final Object mScheduledPoolItemsLock = new Object();
	private ArrayList<T> mScheduledPoolItems = new ArrayList<>();
	private ArrayList<T> mHandledPoolItems = new ArrayList<>();
	// END osu!droid modified

	// ===========================================================
	// Constructors
	// ===========================================================

	public PoolUpdateHandler() {
		this.mPool = new Pool<T>() {
			@Override
			protected T onAllocatePoolItem() {
				return PoolUpdateHandler.this.onAllocatePoolItem();
			}
		};
	}

	public PoolUpdateHandler(final int pInitialPoolSize) {
		this.mPool = new Pool<T>(pInitialPoolSize) {
			@Override
			protected T onAllocatePoolItem() {
				return PoolUpdateHandler.this.onAllocatePoolItem();
			}
		};
	}

	public PoolUpdateHandler(final int pInitialPoolSize, final int pGrowth) {
		this.mPool = new Pool<T>(pInitialPoolSize, pGrowth) {
			@Override
			protected T onAllocatePoolItem() {
				return PoolUpdateHandler.this.onAllocatePoolItem();
			}
		};
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	protected abstract T onAllocatePoolItem();

	protected abstract void onHandlePoolItem(final T pPoolItem);

	// BEGIN osu!droid modified - handle items outside the lock that posting waits for
	// Previously, the lock was held while handling every item. A thread posting an item waited
	// for all of them to be handled, and items posted while handling were cleared without ever
	// being handled.
	// Now, the scheduled items are swapped out under the lock and handled outside of it.
	// Items posted while handling are handled on the next update.
	@Override
	public void onUpdate(final float pSecondsElapsed) {
		final ArrayList<T> handledPoolItems;

		synchronized (this.mScheduledPoolItemsLock) {
			if(this.mScheduledPoolItems.isEmpty()) {
				return;
			}

			handledPoolItems = this.mScheduledPoolItems;
			this.mScheduledPoolItems = this.mHandledPoolItems;
			this.mHandledPoolItems = handledPoolItems;
		}

		final Pool<T> pool = this.mPool;
		final int count = handledPoolItems.size();
		int i = 0;

		try {
			for(; i < count; i++) {
				final T item = handledPoolItems.get(i);
				this.onHandlePoolItem(item);
				pool.recyclePoolItem(item);
			}
		} finally {
			// If an item threw, recycle it and the items after it instead of handling them on a later update.
			for(; i < count; i++) {
				pool.recyclePoolItem(handledPoolItems.get(i));
			}

			handledPoolItems.clear();
		}
	}
	// END osu!droid modified

	@Override
	public void reset() {
		// BEGIN osu!droid modified - handle items outside the lock that posting waits for
		synchronized (this.mScheduledPoolItemsLock) {
			final ArrayList<T> scheduledPoolItems = this.mScheduledPoolItems;
			// END osu!droid modified
			final int count = scheduledPoolItems.size();

			final Pool<T> pool = this.mPool;
			for(int i = count - 1; i >= 0; i--) {
				pool.recyclePoolItem(scheduledPoolItems.get(i));
			}

			scheduledPoolItems.clear();
		}
	}

	// ===========================================================
	// Methods
	// ===========================================================

	public T obtainPoolItem() {
		return this.mPool.obtainPoolItem();
	}

	public void postPoolItem(final T pPoolItem) {
		// BEGIN osu!droid modified - handle items outside the lock that posting waits for
		synchronized (this.mScheduledPoolItemsLock) {
			// END osu!droid modified
			if(pPoolItem == null) {
				throw new IllegalArgumentException("PoolItem already recycled!");
			} else if(!this.mPool.ownsPoolItem(pPoolItem)) {
				throw new IllegalArgumentException("PoolItem from another pool!");
			}

			this.mScheduledPoolItems.add(pPoolItem);
		}
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
