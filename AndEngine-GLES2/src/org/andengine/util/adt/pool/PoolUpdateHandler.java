package org.andengine.util.adt.pool;

import java.util.ArrayList;

import org.andengine.engine.handler.IUpdateHandler;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 * 
 * @author Valentin Milea
 * @author Nicolas Gramlich
 * 
 * @since 23:02:58 - 21.08.2010
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

	public PoolUpdateHandler(final int pInitialPoolSize, final int pGrowth, final int pAvailableItemCountMaximum) {
		this.mPool = new Pool<T>(pInitialPoolSize, pGrowth, pAvailableItemCountMaximum) {
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
	// The scheduled items are swapped out under the lock and handled outside of it, so posting never
	// waits for items being handled. Items posted while handling are handled on the next update, so an
	// item that posts another one cannot keep the update thread busy indefinitely.
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

	@Override
	public void reset() {
		synchronized (this.mScheduledPoolItemsLock) {
			final ArrayList<T> scheduledPoolItems = this.mScheduledPoolItems;
			final Pool<T> pool = this.mPool;

			for(int i = scheduledPoolItems.size() - 1; i >= 0; i--) {
				pool.recyclePoolItem(scheduledPoolItems.get(i));
			}

			scheduledPoolItems.clear();
		}
	}
	// END osu!droid modified

	// ===========================================================
	// Methods
	// ===========================================================

	public T obtainPoolItem() {
		return this.mPool.obtainPoolItem();
	}

	public void postPoolItem(final T pPoolItem) {
		if(pPoolItem == null) {
			throw new IllegalArgumentException("PoolItem already recycled!");
		} else if(!this.mPool.ownsPoolItem(pPoolItem)) {
			throw new IllegalArgumentException("PoolItem from another pool!");
		}

		// BEGIN osu!droid modified - handle items outside the lock that posting waits for
		synchronized (this.mScheduledPoolItemsLock) {
			this.mScheduledPoolItems.add(pPoolItem);
		}
		// END osu!droid modified
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
