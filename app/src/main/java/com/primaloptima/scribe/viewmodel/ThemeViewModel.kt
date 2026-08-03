package com.primaloptima.scribe.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.primaloptima.scribe.ScribeApp
import com.primaloptima.scribe.util.SAFHelper
import com.primaloptima.scribe.util.ThemeDataStoreRepo
import com.primaloptima.scribe.util.model.AppTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val themeManager = (application as ScribeApp).themeManager
    private val dataStoreRepo = ThemeDataStoreRepo(application)

    private val _themes = MutableLiveData<List<AppTheme>>()
    val themes: LiveData<List<AppTheme>> = _themes

    private val _activeTheme = MutableLiveData<AppTheme>()
    val activeTheme: LiveData<AppTheme> = _activeTheme

    init {
        reload()
        viewModelScope.launch {
            dataStoreRepo.activeThemeIdFlow.collectLatest { themeId ->
                if (themeManager.activeTheme().id != themeId) {
                    themeManager.setActiveTheme(themeId)
                    reload()
                }
            }
        }
    }

    fun reload() {
        _themes.value = themeManager.allThemes()
        _activeTheme.value = themeManager.activeTheme()
    }

    fun setActive(id: String) {
        themeManager.setActiveTheme(id)
        viewModelScope.launch {
            dataStoreRepo.setActiveThemeId(id)
        }
        reload()
    }

    fun save(theme: AppTheme) {
        themeManager.saveCustomTheme(theme)
        viewModelScope.launch {
            dataStoreRepo.setThemeBackgroundImage(
                themeId = theme.id,
                uri = theme.backgroundImageUri,
                opacity = theme.backgroundImageOpacity ?: 0.35f
            )
            val json = (getApplication() as ScribeApp).prefs.customThemesJson
            dataStoreRepo.setCustomThemesJson(json)
        }
        reload()
    }

    fun delete(id: String) {
        themeManager.deleteCustomTheme(id)
        if (themeManager.activeTheme().id == id) {
            setActive("paper")
        }
        viewModelScope.launch {
            val json = (getApplication() as ScribeApp).prefs.customThemesJson
            dataStoreRepo.setCustomThemesJson(json)
            // Wipe the theme's private image folder so nothing accumulates on disk.
            // deleteThemeImageFolder is a fast synchronous file operation — fine on IO.
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                SAFHelper.deleteThemeImageFolder(getApplication(), id)
            }
        }
        reload()
    }

    fun duplicate(id: String): AppTheme? {
        val source = themeManager.allThemes().firstOrNull { it.id == id } ?: return null
        val copy = themeManager.duplicateTheme(id) ?: return null
        viewModelScope.launch {
            // If the source theme has images, copy them into the duplicate's own folder
            // so the two themes are completely independent on disk. We also rewrite the
            // URI fields in the saved copy to point at the new folder, not the source's.
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val hadImages = SAFHelper.copyThemeImageFolder(getApplication(), id, copy.id)
                if (hadImages) {
                    // Rewrite bgImageUri and bgOriginalUri to the new folder path
                    val newDir = java.io.File(getApplication<android.app.Application>().filesDir, "bg_images/${copy.id}")
                    fun remapUri(oldUri: String?): String? {
                        if (oldUri.isNullOrEmpty()) return oldUri
                        val oldFile = android.net.Uri.parse(oldUri).path?.let { java.io.File(it) } ?: return oldUri
                        val newFile = java.io.File(newDir, oldFile.name)
                        return if (newFile.exists()) android.net.Uri.fromFile(newFile).toString() else oldUri
                    }
                    val updated = copy.copy(
                        backgroundImageUri = remapUri(copy.backgroundImageUri),
                        backgroundImageOriginalUri = remapUri(copy.backgroundImageOriginalUri)
                    )
                    themeManager.saveCustomTheme(updated)
                    val json = (getApplication() as ScribeApp).prefs.customThemesJson
                    dataStoreRepo.setCustomThemesJson(json)
                }
            }
            reload()
        }
        reload()
        return copy
    }

    fun generateId(): String =
        System.currentTimeMillis().toString() + Math.random().toString().takeLast(6)
}
