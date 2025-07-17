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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import androidx.compose.material.icons.filled.Delete
import com.l5balloon.bigcontacts.widget.Contact
import com.l5balloon.bigcontacts.widget.loadContacts
import com.l5balloon.bigcontacts.widget.toJson
import com.l5balloon.bigcontacts.widget.toThemeList
import com.l5balloon.composecolorpicker.ColorPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val Context.dataStore by preferencesDataStore(name = "settings")
private val CUSTOM_THEMES_KEY = stringPreferencesKey("custom_themes")

suspend fun saveCustomThemes(context: Context, themes: List<WidgetTheme>) {
    context.applicationContext.dataStore.edit { prefs ->
        prefs[CUSTOM_THEMES_KEY] = themes.toJson()
    }
}

suspend fun loadCustomThemes(context: Context): List<WidgetTheme> {
    val prefs = context.applicationContext.dataStore.data.first()
    return prefs[CUSTOM_THEMES_KEY]?.toThemeList() ?: emptyList()
}

suspend fun deleteCustomTheme(context: Context, themeKey: String) {
    val currentThemes = loadCustomThemes(context)
    val updatedThemes = currentThemes.filterNot { it.key == themeKey }
    saveCustomThemes(context, updatedThemes)
}

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
}

@Composable
fun LoadingContactsUI() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.loading_contacts),
            fontSize = dpToSp(24.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ColorPickerDialog(
    initialColor: Color,
    previewBackgroundColor: Color,
    previewTextColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit,
    isBackground: Boolean
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val dialogHeight = screenHeight * 0.8f
    var pickedColor by remember { mutableStateOf(initialColor) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .widthIn(max = 480.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = dialogHeight)
                    .padding(vertical = 16.dp, horizontal = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.color_picker),
                    modifier = Modifier.align(Alignment.Start),
                    style = MaterialTheme.typography.titleLarge
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ColorPicker(
                        initialColor = initialColor,
                        onColorChanged = { pickedColor = it },
                        useAlpha = true,
                        showAlphaSlider = true,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                ThemePreview(
                    backgroundColor = if (isBackground) pickedColor else previewBackgroundColor,
                    textColor = if (isBackground) previewTextColor else pickedColor,
                    modifier = Modifier
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onColorSelected(pickedColor) }) {
                        Text(stringResource(R.string.select))
                    }
                }
            }
        }
    }
}

@Composable
fun ThemePreview(
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(80.dp)
                .background(backgroundColor, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.preview),
                style = TextStyle(
                    fontSize = dpToSp(36.dp),
                    fontWeight = Bold
                ),
                color = textColor,
            )
        }
    }
}

@Composable
fun AddThemeDialog(
    onAdd: (WidgetTheme) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var themeName by remember { mutableStateOf(TextFieldValue("")) }
    var backgroundColor by remember { mutableStateOf(Color.Black) }
    var textColor by remember { mutableStateOf(Color.White) }
    var showBackgroundColorPicker by remember { mutableStateOf(false) }
    var showTextColorPicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .widthIn(max = 480.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.add_theme),
                    modifier = Modifier.align(Alignment.Start),
                    style = MaterialTheme.typography.titleLarge
                )

                // Theme name input
                OutlinedTextField(
                    value = themeName,
                    onValueChange = { themeName = it },
                    label = { Text(stringResource(R.string.theme_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                // Background color selection
                Text(
                    stringResource(R.string.select_background_color),
                    modifier = Modifier.align(Alignment.Start)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Color preview
                    Box(
                        modifier = Modifier
                            .size(width = 120.dp, height = 56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(backgroundColor)
                            .border(1.dp, Color.Gray, RoundedCornerShape(16.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    // Color wheel icon (opens picker on click)
                    Icon(
                        painter = painterResource(id = R.drawable.ic_color_wheel),
                        contentDescription = stringResource(R.string.color_picker),
                        modifier = Modifier
                            .size(56.dp)
                            .clickable { showBackgroundColorPicker = true },
                        tint = Color.Unspecified
                    )
                }
                Spacer(Modifier.height(8.dp))

                // Text color selection
                Text(
                    stringResource(R.string.select_text_color),
                    modifier = Modifier.align(Alignment.Start)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Color preview
                    Box(
                        modifier = Modifier
                            .size(width = 120.dp, height = 56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(textColor)
                            .border(1.dp, Color.Gray, RoundedCornerShape(16.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_color_wheel),
                        contentDescription = stringResource(R.string.color_picker),
                        modifier = Modifier
                            .size(56.dp)
                            .clickable { showTextColorPicker = true },
                        tint = Color.Unspecified
                    )
                }
                Spacer(Modifier.height(16.dp))

                // Example preview
                ThemePreview(
                    backgroundColor = backgroundColor,
                    textColor = textColor,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        if (themeName.text.isNotBlank()) {
                            onAdd(
                                WidgetTheme(
                                    key = "custom_${System.currentTimeMillis()}",
                                    nameResId = null,
                                    name = themeName.text,
                                    backgroundColor = backgroundColor,
                                    textColor = textColor
                                )
                            )
                        } else {
                            Toast.makeText(context, context.getString(R.string.please_enter_theme_name), Toast.LENGTH_SHORT).show()
                        }
                    }) { Text(stringResource(R.string.add)) }
                }
            }
        }
    }

    // Background color color picker dialog
    if (showBackgroundColorPicker) {
        key(backgroundColor) {
            ColorPickerDialog(
                initialColor = backgroundColor,
                previewBackgroundColor = backgroundColor,
                previewTextColor = textColor,
                onColorSelected = {
                    backgroundColor = it
                    showBackgroundColorPicker = false
                },
                onDismiss = { showBackgroundColorPicker = false },
                isBackground = true
            )
        }
    }
    // Text color color picker dialog
    if (showTextColorPicker) {
        key(textColor) {
            ColorPickerDialog(
                initialColor = textColor,
                previewBackgroundColor = backgroundColor,
                previewTextColor = textColor,
                onColorSelected = {
                    textColor = it
                    showTextColorPicker = false
                },
                onDismiss = { showTextColorPicker = false },
                isBackground = false
            )
        }
    }
}

@Composable
fun WidgetConfigScreen(
    appWidgetId: Int,
    is4x1: Boolean,
    onConfigComplete: () -> Unit
) {
    val context = LocalContext.current
    var customThemes by remember { mutableStateOf(listOf<WidgetTheme>()) }
    val coroutineScope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf<WidgetTheme?>(null) }

    LaunchedEffect(Unit) {
        customThemes = loadCustomThemes(context)
    }
    var selectedTheme by remember { mutableStateOf(WidgetTheme.THEMES.first()) }
    var selectedContact by remember { mutableStateOf<Contact?>(null) }

    var hasPermission by remember { mutableStateOf(false) }
    val contacts = remember { mutableStateListOf<Contact>() }
    var isLoadingContacts by remember { mutableStateOf(false) }

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
            isLoadingContacts = true
            withContext(Dispatchers.IO) {
                val loadedContacts = loadContacts(context)
                contacts.clear()
                contacts.addAll(loadedContacts)

                delay(200L)
            }
            isLoadingContacts = false
        }
    }

    // Add theme dialog state
    var showAddThemeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues())
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
            if (isLoadingContacts) {
                LoadingContactsUI()
            } else {
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
                                    if (isSelected) Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(16.dp)
                                    )
                                    else Modifier
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.CenterStart
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

        // Theme list + add theme button
        val allThemes = WidgetTheme.THEMES + customThemes
        val themeListState = rememberLazyListState()
        val themeScope = rememberCoroutineScope()
        val rowHeight = 60.dp
        val maxRowHeight = rowHeight + 20.dp

        Box(modifier = Modifier.fillMaxWidth()) {
            LazyRow(
                state = themeListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = rowHeight),
                contentPadding = PaddingValues(vertical = 10.dp, horizontal = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(allThemes) { theme ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { selectedTheme = theme },
                            modifier = if (selectedTheme == theme) {
                                Modifier
                                    .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .defaultMinSize(minHeight = rowHeight)
                            } else {
                                Modifier.defaultMinSize(minHeight = rowHeight)
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.backgroundColor,
                                contentColor = theme.textColor
                            )
                        ) {
                            Text(
                                if (theme.nameResId != null) {
                                    stringResource(id = theme.nameResId)
                                } else {
                                    theme.name
                                } ?: "",
                                fontSize = 24.sp,
                                lineHeight = 26.sp
                            )
                        }

                        // Delete icon: only shown for custom themes
                        if (theme.nameResId == null) {
                            IconButton(onClick = {
                                showDeleteDialog = theme
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
                // Add user theme button
                item {
                    Button(
                        onClick = { showAddThemeDialog = true },
                        shape = CircleShape,
                        modifier = Modifier.size(rowHeight),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.add),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .width(32.dp)
                    .height(maxRowHeight)
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
                    .height(maxRowHeight)
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
                    .height(maxRowHeight)
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
                    .height(maxRowHeight)
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
                    is4x1 = is4x1,
                    backgroundColor = selectedTheme.backgroundColor.toArgb(),
                    textColor = selectedTheme.textColor.toArgb()
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

    // Add theme dialog
    if (showAddThemeDialog) {
        AddThemeDialog(
            onAdd = { theme ->
                val updated = customThemes + theme
                customThemes = updated
                coroutineScope.launch {
                    saveCustomThemes(context, updated)
                }
                showAddThemeDialog = false
            },
            onDismiss = { showAddThemeDialog = false }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.delete_theme)) },
            text = { Text(stringResource(R.string.delete_theme_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    val themeToDelete = showDeleteDialog
                    if (themeToDelete != null) {
                        coroutineScope.launch {
                            deleteCustomTheme(context, themeToDelete.key)
                            // State update
                            customThemes = loadCustomThemes(context)
                            // Change to default theme if selected theme is deleted
                            if (selectedTheme.key == themeToDelete.key) {
                                selectedTheme = WidgetTheme.THEMES.first()
                            }
                        }
                    }
                    showDeleteDialog = null
                }) { Text(stringResource(R.string.delete), color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}