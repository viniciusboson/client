// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.providers;

import android.content.UriMatcher;
import android.net.Uri;
import android.util.SparseArray;

import net.sqlcipher.database.SQLiteOpenHelper;

import java.util.concurrent.atomic.AtomicInteger;

/** A registry for {@link ProviderDelegate}s. */
class ProviderDelegateRegistry<T extends SQLiteOpenHelper> {

    private final UriMatcher mUriMatcher;
    private final AtomicInteger mCodeGenerator;

    private final SparseArray<ProviderDelegate<T>> mDelegates;

    ProviderDelegateRegistry() {
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mCodeGenerator = new AtomicInteger(1);
        mDelegates = new SparseArray<>();
    }

    /**
     * Registers the specified delegate to handle the specified path.
     * @throws IllegalStateException if the specified path is already being handled by another
     *                               delegate
     */
    void registerDelegate(String path, ProviderDelegate<T> delegate) {
        int existingCode = mUriMatcher.match(
            Uri.parse("content://" + Contracts.CONTENT_AUTHORITY + "/" + path));
        if (existingCode != UriMatcher.NO_MATCH) {
            throw new IllegalStateException(
                "Path '" + path + "' is already registered to be handled by '"
                    + mDelegates.get(existingCode).toString() + "'.");
        }
        int code = mCodeGenerator.getAndIncrement();
        mUriMatcher.addURI(Contracts.CONTENT_AUTHORITY, path, code);
        mDelegates.put(code, delegate);
    }

    /**
     * Returns the {@link ProviderDelegate} for the specified {@link Uri}.
     * @throws IllegalArgumentException if no matching delegate is registered
     */
    ProviderDelegate<T> getDelegate(Uri uri) {
        int code = mUriMatcher.match(uri);
        if (code == UriMatcher.NO_MATCH) {
            throw new IllegalArgumentException(
                "No ProviderDelegate registered for URI '" + uri.toString() + "'.");
        }

        return mDelegates.get(code);
    }
}
