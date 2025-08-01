package com.l5balloon.bigcontacts.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.unit.ColorProvider
import com.l5balloon.bigcontacts.R
import com.l5balloon.bigcontacts.WidgetConfigActivity
import com.l5balloon.bigcontacts.data.WidgetData
import com.l5balloon.bigcontacts.data.WidgetKeys
import com.l5balloon.bigcontacts.data.WidgetTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RefreshAndOpenContactAction : ActionCallback {
    companion object {
        val PARAM_LOOKUP_URI = ActionParameters.Key<String>("lookupUri")
    }
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // Refresh all widget
        val glanceManager = GlanceAppWidgetManager(context)
        val glanceIds = glanceManager.getGlanceIds(BigContactsWidget::class.java)
        for (id in glanceIds) {
            BigContactsWidget().update(context, id)
        }

        // open contacts
        val lookupUri = parameters[PARAM_LOOKUP_URI]
        if (lookupUri != null) {
            val intent = Intent(Intent.ACTION_VIEW, lookupUri.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}

class BigContactsWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)

        provideContent {
            val prefs = currentState<Preferences>()
            val contactLookupUri = prefs[WidgetKeys.ContactLookupUri]
            val contactName = contactLookupUri?.let { lookupUri ->
                queryContactName(context, lookupUri)
            }

            val action: Action = if (contactLookupUri != null) {
                actionRunCallback<RefreshAndOpenContactAction>(
                    actionParametersOf(RefreshAndOpenContactAction.PARAM_LOOKUP_URI to contactLookupUri)
                )
            } else {
                val intent = Intent(context, WidgetConfigActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                actionStartActivity(intent)
            }

            val widgetThemeKey = prefs[WidgetKeys.Theme] ?: WidgetKeys.DEFAULT_THEME_KEY
            val backgroundColor = prefs[WidgetKeys.BackgroundColor]
            val textColor = prefs[WidgetKeys.TextColor]
            val widgetTheme = WidgetTheme.THEMES.find { it.key == widgetThemeKey }
                ?: if (backgroundColor != null && textColor != null) {
                    WidgetTheme(
                        key = widgetThemeKey,
                        nameResId = null,
                        name = null,
                        backgroundColor = androidx.compose.ui.graphics.Color(backgroundColor),
                        textColor = androidx.compose.ui.graphics.Color(textColor)
                    )
                } else {
                    WidgetTheme.THEMES.first()
                }

            val tapToConfigureText = context.getString(R.string.tap_to_configure)

            WidgetContent(
                contactName = contactName,
                widgetTheme = widgetTheme,
                tapToConfigureText = tapToConfigureText,
                action = action
            )
        }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetId: Int, widgetData: WidgetData) {
            CoroutineScope(Dispatchers.IO).launch {
                val glanceManager = GlanceAppWidgetManager(context)
                val glanceId = glanceManager.getGlanceIdBy(appWidgetId)
                if (glanceId != null) {
                    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                        val mutablePrefs = prefs.toMutablePreferences()
                        mutablePrefs[WidgetKeys.ContactName] = widgetData.contactName
                        mutablePrefs[WidgetKeys.ContactLookupUri] = widgetData.contactLookupUri
                        mutablePrefs[WidgetKeys.Is4x1] = widgetData.is4x1
                        mutablePrefs[WidgetKeys.Theme] = widgetData.theme
                        mutablePrefs[WidgetKeys.BackgroundColor] = widgetData.backgroundColor
                        mutablePrefs[WidgetKeys.TextColor] = widgetData.textColor
                        mutablePrefs
                    }
                    BigContactsWidget().update(context, glanceId)
                }
            }
        }
    }
}

@Composable
@GlanceComposable
private fun WidgetContent(
    contactName: String?,
    widgetTheme: WidgetTheme,
    tapToConfigureText: String,
    action: Action
) {
    val context = LocalContext.current
    val backgroundColor = ColorProvider(widgetTheme.backgroundColor)
    val textColor = widgetTheme.textColor.toArgb()

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable(action)
    ) {
        AndroidRemoteViews(
            remoteViews = RemoteViews(context.packageName, R.layout.widget_layout_main).apply {
                val textToShow = contactName ?: tapToConfigureText
                setTextViewText(R.id.widget_text, textToShow)
                setTextColor(R.id.widget_text, textColor)
            }
        )
    }
}

class BigContactsWidgetReceiver2x1 : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BigContactsWidget()
}

class BigContactsWidgetReceiver4x1 : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BigContactsWidget()
}