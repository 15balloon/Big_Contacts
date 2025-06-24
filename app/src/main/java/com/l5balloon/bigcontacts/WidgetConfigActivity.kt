package com.l5balloon.bigcontacts

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.l5balloon.bigcontacts.data.WidgetData
import com.l5balloon.bigcontacts.data.WidgetTheme
import com.l5balloon.bigcontacts.ui.theme.BigContactsTheme
import com.l5balloon.bigcontacts.widget.BigContactsWidget
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch

data class Contact(val id: Long, val lookupKey: String, val name: String)

class WidgetConfigActivity : ComponentActivity() {

    companion object {
        private const val WIDGET_4X1_CLASSNAME = "4x1"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        onBackPressedDispatcher.addCallback(this) {
            finish()
        }

        setResult(RESULT_CANCELED)

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        val is4x1 = intent.component?.className?.contains(WIDGET_4X1_CLASSNAME) ?: false
        
        setContent {
            BigContactsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WidgetConfigScreen(
                        appWidgetId = appWidgetId,
                        is4x1 = is4x1,
                        onConfigComplete = {
                            val resultValue = Intent().apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            }
                            setResult(RESULT_OK, resultValue)
                            finish()
                        }
                    )
                }
            }
        }
    }

    fun loadContacts(context: Context): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
        )
        val cursor = context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " ASC"
        )
        cursor?.use {
            val idColumn = it.getColumnIndex(ContactsContract.Contacts._ID)
            val lookupKeyColumn = it.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)
            val nameColumn = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val lookupKey = it.getString(lookupKeyColumn)
                val name = it.getString(nameColumn)
                contacts.add(Contact(id, lookupKey, name))
            }
        }
        return contacts
    }
}

@Composable
fun WidgetConfigScreen(
    appWidgetId: Int,
    is4x1: Boolean,
    onConfigComplete: () -> Unit
) {
    var selectedTheme by remember { mutableStateOf(WidgetTheme.THEMES.first()) }
    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    val context = LocalContext.current

    var hasPermission by remember { mutableStateOf(false) }
    val contacts = remember { mutableStateListOf<Contact>() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
        }
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_CONTACTS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            val loadedContacts = (context as WidgetConfigActivity).loadContacts(context)
            contacts.clear()
            contacts.addAll(loadedContacts)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(id = R.string.widget_config_title),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.headlineMedium,
            fontSize = 24.sp,
            lineHeight = 26.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        if (hasPermission) {
            Text(
                stringResource(id = R.string.select_contact),
                style = MaterialTheme.typography.titleMedium,
                fontSize = 24.sp,
                lineHeight = 26.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(contacts) { contact ->
                    val isSelected = selectedContact?.id == contact.id
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 72.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedContact = contact }
                            .then(
                                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                                else Modifier
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = contact.name,
                                fontSize = 24.sp,
                                lineHeight = 26.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Clip,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.permission_rationale),
                    textAlign = TextAlign.Center,
                    fontSize = 24.sp,
                    lineHeight = 26.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", context.packageName, null)
                    intent.data = uri
                    context.startActivity(intent)
                }) {
                    Text(
                        stringResource(id = R.string.go_to_settings),
                        fontSize = 24.sp,
                        lineHeight = 26.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(id = R.string.widget_config_theme_title),
            style = MaterialTheme.typography.titleMedium,
            fontSize = 24.sp,
            lineHeight = 26.sp
        )

        val themeListState = rememberLazyListState()
        val themeScope = rememberCoroutineScope()
        val rowHeight = 80.dp
        Box(modifier = Modifier.fillMaxWidth()) {
            LazyRow(
                state = themeListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = rowHeight),
                contentPadding = PaddingValues(horizontal = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(WidgetTheme.THEMES) { theme ->
                    Button(
                        onClick = { selectedTheme = theme },
                        modifier = if (selectedTheme == theme) {
                            Modifier.border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        } else {
                            Modifier
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.backgroundColor,
                            contentColor = theme.textColor
                        )
                    ) {
                        Text(
                            stringResource(id = theme.nameResId),
                            fontSize = 24.sp,
                            lineHeight = 26.sp
                        )
                    }
                }
            }
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .width(32.dp)
                    .defaultMinSize(minHeight = rowHeight)
                    .background(
                        Brush.horizontalGradient(
                            listOf(MaterialTheme.colorScheme.background, Color.Transparent)
                        )
                    )
            )
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .width(32.dp)
                    .defaultMinSize(minHeight = rowHeight)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                        )
                    )
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.previous),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp)
                    .size(32.dp)
                    .defaultMinSize(minHeight = rowHeight)
                    .clickable {
                        themeScope.launch {
                            themeListState.animateScrollBy(-600f)
                        }
                    },
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.next),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
                    .size(32.dp)
                    .defaultMinSize(minHeight = rowHeight)
                    .clickable {
                        themeScope.launch {
                            themeListState.animateScrollBy(600f)
                        }
                    },
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                if (selectedContact == null) {
                    Toast.makeText(context, R.string.please_select_contact, Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val finalContact = selectedContact!!
                val lookupUri = ContactsContract.Contacts.getLookupUri(finalContact.id, finalContact.lookupKey)

                val widgetData = WidgetData(
                    contactName = finalContact.name,
                    contactLookupUri = lookupUri.toString(),
                    theme = selectedTheme.key,
                    is4x1 = is4x1
                )
                
                BigContactsWidget.updateWidget(
                    context = context,
                    appWidgetId = appWidgetId,
                    widgetData = widgetData
                )
                
                onConfigComplete()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(id = R.string.config_complete),
                fontSize = 24.sp,
                lineHeight = 26.sp
            )
        }
    }
} 