package com.serenegiant.mediastore;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2019 saki t_saki@serenegiant.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/

import android.content.ContentResolver;
import android.graphics.Bitmap;

import com.serenegiant.utils.ThreadPool;

import java.util.concurrent.FutureTask;

import androidx.annotation.NonNull;

/**
 * Runnable to load image asynchronously
 */
public abstract class ImageLoader implements Runnable {
	@NonNull
	protected final LoaderDrawable mParent;
	@NonNull
	private final FutureTask<Bitmap> mTask;
	private int mMediaType;
	private int mHashCode;
	private long mId;
	private Bitmap mBitmap;

	/**
	 * コンストラクタ
	 * @param parent
	 */
    public ImageLoader(@NonNull final LoaderDrawable parent) {
    	mParent = parent;
		mTask = new FutureTask<Bitmap>(this, null);
    }

	public long id() {
		return mId;
	}

    /**
	 * start loading
	 * @param hashCode
     * @param id
     */
	public synchronized void startLoad(final int mediaType, final int hashCode, final long id) {
		mMediaType = mediaType;
		mHashCode = hashCode;
		mId = id;
		mBitmap = null;
		ThreadPool.queueEvent(mTask);
	}

	/**
	 * cancel loading
	 */
	public void cancelLoad() {
		mTask.cancel(true);
	}

	/**
	 * 実際の読み込み処理
	 * @param cr
	 * @param mediaType
	 * @param hashCode
	 * @param id
	 * @param requestWidth
	 * @param requestHeight
	 * @return
	 */
	protected abstract Bitmap loadBitmap(
		@NonNull final ContentResolver cr,
		final int mediaType, final int hashCode, final long id,
		final int requestWidth, final int requestHeight);

	@Override
	public void run() {
		int mediaType;
		int hashCode;
		long id;
		synchronized(this) {
			mediaType = mMediaType;
			hashCode = mHashCode;
			id = mId;
		}
		if (!mTask.isCancelled()) {
			mBitmap = loadBitmap(mParent.getContentResolver(),
				mediaType, hashCode, id,
				mParent.getIntrinsicWidth(), mParent.getIntrinsicHeight());
		}
		if (mTask.isCancelled() || (id != mId) || (mBitmap == null)) {
			return;	// return without callback
		}
		// set callback
		mParent.scheduleSelf(mParent, 0);
	}

	public Bitmap getBitmap() {
		return mBitmap;
	}
}