package com.achyutpal.scrolljourney.domain.calculator

import com.achyutpal.scrolljourney.domain.model.InstalledApp
import com.achyutpal.scrolljourney.domain.model.SelectedApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSelectionRulesTest {

    private val instagram = InstalledApp("com.instagram.android", "Instagram")
    private val youtube = InstalledApp("com.google.android.youtube", "YouTube")
    private val chrome = InstalledApp("com.android.chrome", "Chrome")
    private val maps = InstalledApp("com.google.android.apps.maps", "Maps")

    @Test
    fun `first run pre-selects installed default social apps only`() {
        val result = AppSelectionRules.initialSelection(listOf(instagram, youtube, chrome), saved = emptyList())
        assertEquals(setOf(instagram.packageName, youtube.packageName), result)
    }

    @Test
    fun `default app that is not installed is not selected`() {
        val result = AppSelectionRules.initialSelection(listOf(chrome), saved = emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `after saving, the saved choice wins over defaults`() {
        val saved = listOf(
            SelectedApp(instagram.packageName, "Instagram", enabled = false, addedAt = 1),
            SelectedApp(chrome.packageName, "Chrome", enabled = true, addedAt = 1),
        )
        val result = AppSelectionRules.initialSelection(listOf(instagram, youtube, chrome), saved)
        assertEquals(setOf(chrome.packageName), result)
    }

    @Test
    fun `reinstalled app that was disabled on uninstall stays unticked`() {
        val saved = listOf(
            SelectedApp(instagram.packageName, "Instagram", enabled = false, addedAt = 1),
            SelectedApp(youtube.packageName, "YouTube", enabled = true, addedAt = 1),
        )
        val result = AppSelectionRules.initialSelection(listOf(instagram, youtube), saved)
        assertEquals(setOf(youtube.packageName), result)
    }

    @Test
    fun `sort puts pre-selected first, then alphabetical`() {
        val sorted = AppSelectionRules.sortForDisplay(
            listOf(maps, youtube, chrome, instagram),
            preselected = setOf(youtube.packageName, instagram.packageName),
        )
        assertEquals(listOf("Instagram", "YouTube", "Chrome", "Maps"), sorted.map { it.label })
    }

    @Test
    fun `ticked apps become enabled records with addedAt = now when new`() {
        val records = AppSelectionRules.buildSelectionRecords(
            installed = listOf(instagram, chrome),
            saved = emptyList(),
            selected = setOf(instagram.packageName),
            now = 500,
        )
        assertEquals(listOf(SelectedApp(instagram.packageName, "Instagram", true, 500)), records)
    }

    @Test
    fun `re-ticking keeps the original addedAt`() {
        val saved = listOf(SelectedApp(instagram.packageName, "Instagram", enabled = false, addedAt = 100))
        val records = AppSelectionRules.buildSelectionRecords(
            installed = listOf(instagram),
            saved = saved,
            selected = setOf(instagram.packageName),
            now = 900,
        )
        assertEquals(SelectedApp(instagram.packageName, "Instagram", true, 100), records.single())
    }

    @Test
    fun `unticking a saved app disables it instead of deleting it`() {
        val saved = listOf(SelectedApp(instagram.packageName, "Instagram", enabled = true, addedAt = 100))
        val records = AppSelectionRules.buildSelectionRecords(
            installed = listOf(instagram, youtube),
            saved = saved,
            selected = setOf(youtube.packageName),
            now = 900,
        )
        assertTrue(SelectedApp(instagram.packageName, "Instagram", false, 100) in records)
        assertTrue(SelectedApp(youtube.packageName, "YouTube", true, 900) in records)
    }

    @Test
    fun `saved app that is no longer installed gets disabled but kept`() {
        val saved = listOf(SelectedApp(instagram.packageName, "Instagram", enabled = true, addedAt = 100))
        val records = AppSelectionRules.buildSelectionRecords(
            installed = listOf(youtube),
            saved = saved,
            selected = setOf(youtube.packageName),
            now = 900,
        )
        assertTrue(SelectedApp(instagram.packageName, "Instagram", false, 100) in records)
    }

    @Test
    fun `unticked app that was never saved produces no record`() {
        val records = AppSelectionRules.buildSelectionRecords(
            installed = listOf(instagram, chrome),
            saved = emptyList(),
            selected = setOf(instagram.packageName),
            now = 1,
        )
        assertTrue(records.none { it.packageName == chrome.packageName })
    }

    @Test
    fun `uninstalled tracked apps are switched off, others untouched`() {
        val tracked = listOf(
            SelectedApp("a", "A", enabled = true, addedAt = 1),
            SelectedApp("b", "B", enabled = true, addedAt = 2),
            SelectedApp("c", "C", enabled = false, addedAt = 3), // already off: nothing to do
        )
        val disabled = AppSelectionRules.disableUninstalled(tracked) { it != "b" && it != "c" }
        assertEquals(listOf(SelectedApp("b", "B", enabled = false, addedAt = 2)), disabled)
    }
}
