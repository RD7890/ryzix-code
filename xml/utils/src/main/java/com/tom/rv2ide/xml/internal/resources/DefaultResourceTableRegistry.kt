/*
   *  This file is part of Ryzix Code.
   *
   *  Ryzix Code is free software: you can redistribute it and/or modify
   *  it under the terms of the GNU General Public License as published by
   *  the Free Software Foundation, either version 3 of the License, or
   *  (at your option) any later version.
   *
   *  Ryzix Code is distributed in the hope that it will be useful,
   *  but WITHOUT ANY WARRANTY; without even the implied warranty of
   *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   *  GNU General Public License for more details.
   *
   *  You should have received a copy of the GNU General Public License
   *   along with Ryzix Code.  If not, see <https://www.gnu.org/licenses/>.
   */

  package com.tom.rv2ide.xml.internal.resources

  import com.android.SdkConstants
  import com.android.SdkConstants.FN_INTENT_ACTIONS_ACTIVITY
  import com.android.SdkConstants.FN_INTENT_ACTIONS_BROADCAST
  import com.android.SdkConstants.FN_INTENT_ACTIONS_SERVICE
  import com.android.SdkConstants.FN_INTENT_CATEGORIES
  import com.google.auto.service.AutoService
  import com.tom.rv2ide.xml.internal.resources.DefaultResourceTableRegistry.SingleLineValueEntryType.ACTIVITY_ACTIONS
  import com.tom.rv2ide.xml.internal.resources.DefaultResourceTableRegistry.SingleLineValueEntryType.BROADCAST_ACTIONS
  import com.tom.rv2ide.xml.internal.resources.DefaultResourceTableRegistry.SingleLineValueEntryType.CATEGORIES
  import com.tom.rv2ide.xml.internal.resources.DefaultResourceTableRegistry.SingleLineValueEntryType.FEATURES
  import com.tom.rv2ide.xml.internal.resources.DefaultResourceTableRegistry.SingleLineValueEntryType.SERVICE_ACTIONS
  import com.tom.rv2ide.xml.res.IResourceTable
  import com.tom.rv2ide.xml.resources.ResourceTableRegistry
  import com.tom.rv2ide.xml.resources.ResourceTableRegistry.Companion.PCK_ANDROID
  import java.io.File
  import java.util.concurrent.ConcurrentHashMap
  import org.slf4j.LoggerFactory

  /**
   * Default implementation of [ResourceTableRegistry].
   *
   * Resource-table parsing (AAPT2) has been removed from this stripped build.
   * All resource-table lookups return null; single-line data files (activity
   * actions, broadcast actions, etc.) are still read from the platform directory.
   */
  @AutoService(ResourceTableRegistry::class)
  class DefaultResourceTableRegistry : ResourceTableRegistry {

    /**
     * Represents the type of single-line entries read from platform data files.
     *
     * @property filename The file that contains the single-line entries.
     */
    internal enum class SingleLineValueEntryType(val filename: String) {
      ACTIVITY_ACTIONS(FN_INTENT_ACTIONS_ACTIVITY),
      BROADCAST_ACTIONS(FN_INTENT_ACTIONS_BROADCAST),
      SERVICE_ACTIONS(FN_INTENT_ACTIONS_SERVICE),
      CATEGORIES(FN_INTENT_CATEGORIES),
      FEATURES("features.txt"),
    }

    private val singleLineValueEntries =
      ConcurrentHashMap<String, ConcurrentHashMap<SingleLineValueEntryType, List<String>>>()

    companion object {
      private val log = LoggerFactory.getLogger(DefaultResourceTableRegistry::class.java)
    }

    override var isLoggingEnabled: Boolean = true

    override fun forPackage(name: String, vararg resDirs: File): IResourceTable? = null

    override fun forPlatformDir(platform: File): IResourceTable? {
      getManifestAttrTable(platform)
      getActivityActions(platform)
      getBroadcastActions(platform)
      getServiceActions(platform)
      getCategories(platform)
      getFeatures(platform)
      return super.forPlatformDir(platform)
    }

    override fun getManifestAttrTable(platform: File): IResourceTable? = null

    override fun getActivityActions(platform: File): List<String> =
      getSingleLineEntry(platform, ACTIVITY_ACTIONS)

    override fun getBroadcastActions(platform: File): List<String> =
      getSingleLineEntry(platform, BROADCAST_ACTIONS)

    override fun getServiceActions(platform: File): List<String> =
      getSingleLineEntry(platform, SERVICE_ACTIONS)

    override fun getCategories(platform: File): List<String> =
      getSingleLineEntry(platform, CATEGORIES)

    override fun getFeatures(platform: File): List<String> =
      getSingleLineEntry(platform, FEATURES)

    override fun removeTable(packageName: String) {}

    override fun clear() {
      singleLineValueEntries.clear()
    }

    private fun getSingleLineEntry(platform: File, type: SingleLineValueEntryType): List<String> {
      var entries = singleLineValueEntries[platform.path]
      if (entries == null) {
        entries = readSingleLineEntry(platform, type)
        singleLineValueEntries[platform.path] = entries
      }
      return entries[type]
        ?: run {
          readSingleLineEntriesTo(platform, type, entries)
          entries[type] ?: emptyList()
        }
    }

    private fun readSingleLineEntry(
      platform: File,
      type: SingleLineValueEntryType,
    ): ConcurrentHashMap<SingleLineValueEntryType, List<String>> {
      val map = ConcurrentHashMap<SingleLineValueEntryType, List<String>>()
      readSingleLineEntriesTo(platform, type, map)
      return map
    }

    private fun readSingleLineEntriesTo(
      platform: File,
      type: SingleLineValueEntryType,
      map: ConcurrentHashMap<SingleLineValueEntryType, List<String>>,
    ) {
      val file = File(platform, "${SdkConstants.FD_DATA}/${type.filename}")
      if (!file.exists() || !file.canRead()) {
        return
      }
      map[type] = file.readLines()
    }
  }
  