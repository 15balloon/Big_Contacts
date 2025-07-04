package com.l5balloon.bigcontacts

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.l5balloon.bigcontacts.ui.theme.BigContactsTheme
import android.widget.Toast
import android.content.Context.RECEIVER_EXPORTED
import com.l5balloon.bigcontacts.widget.BigContactsWidgetReceiver2x1
import com.l5balloon.bigcontacts.widget.BigContactsWidgetReceiver4x1
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight.Companion.Bold

class MainActivity : ComponentActivity() {

    private val widgetPinnedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context?.startActivity(homeIntent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                widgetPinnedReceiver,
                IntentFilter(ACTION_WIDGET_PINNED),
                RECEIVER_EXPORTED
            )
        } else {
            registerReceiver(
                widgetPinnedReceiver,
                IntentFilter(ACTION_WIDGET_PINNED)
            )
        }

        setContent {
            BigContactsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        onAddWidget = { componentName ->
                            val appWidgetManager = getSystemService(AppWidgetManager::class.java)
                            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                val successCallback = PendingIntent.getBroadcast(
                                    this,
                                    0,
                                    Intent(ACTION_WIDGET_PINNED),
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                appWidgetManager.requestPinAppWidget(componentName, null, successCallback)
                            } else {
                                Toast.makeText(this, R.string.error_pin_widget_not_supported, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(widgetPinnedReceiver)
    }

    companion object {
        private const val ACTION_WIDGET_PINNED = "com.l5balloon.bigcontacts.action.WIDGET_PINNED"
    }
}

@Composable
fun dpToSp(dp: Dp) = with(LocalDensity.current) { dp.toSp() }

@Composable
fun MainScreen(onAddWidget: (ComponentName) -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val availableWidth = screenWidth - 16.dp

    val widget2x1Width = (availableWidth / 2) - 8.dp
    val widget2x1Height = widget2x1Width * (50f / 110f)

    val widget4x1Width = availableWidth - 8.dp
    val widget4x1Height = widget4x1Width * (50f / 220f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues())

    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.add_widget),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Button(
                onClick = {
                    onAddWidget(ComponentName(context, BigContactsWidgetReceiver2x1::class.java))
                },
                modifier = Modifier
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.add_widget_2x1),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        style = TextStyle(
                            fontSize = dpToSp(32.dp),
                        ),
                    )

                    Text(
                        text = stringResource(R.string.example_label),
                        modifier = Modifier.align(Alignment.Start),
                        style = TextStyle(
                            fontSize = dpToSp(24.dp),
                        ),
                    )

                    Box(
                        modifier = Modifier
                            .width(widget2x1Width)
                            .height(widget2x1Height)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.widget_preview_name_short),
                            style = TextStyle(
                                fontSize = dpToSp(36.dp),
                                fontWeight = Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Button(
                onClick = {
                    onAddWidget(ComponentName(context, BigContactsWidgetReceiver4x1::class.java))
                },
                modifier = Modifier
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.add_widget_4x1),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        style = TextStyle(
                            fontSize = dpToSp(32.dp),
                        ),
                    )

                    Text(
                        text = stringResource(R.string.example_label),
                        modifier = Modifier.align(Alignment.Start),
                        style = TextStyle(
                            fontSize = dpToSp(24.dp),
                        ),
                    )

                    Box(
                        modifier = Modifier
                            .width(widget4x1Width)
                            .height(widget4x1Height)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.widget_preview_name_long),
                            style = TextStyle(
                                fontSize = dpToSp(36.dp),
                                fontWeight = Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
            ) {
                TextButton(
                    onClick = {
                        context.startActivity(Intent(context, LicenseActivity::class.java))
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.font_license_title),
                        style = TextStyle(
                            fontSize = dpToSp(12.dp)
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                }
                TextButton(
                    onClick = {
                        context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.open_source_license_title),
                        style = TextStyle(
                            fontSize = dpToSp(12.dp)
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                }
            }
        }
    }
}