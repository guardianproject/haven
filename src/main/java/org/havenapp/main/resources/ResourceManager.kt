package org.havenapp.main.resources

import android.content.Context

/**
 * An implementation of [IResourceManager] which requires [Context] for providing resources.
 * <p>
 * Created by Arka Prava Basu <arkaprava94@gmail.com> on 1/9/18.
 */
class ResourceManager(private val context: Context): IResourceManager {

    override fun getString(id: Int): String {
        return context.getString(id)
    }
}
