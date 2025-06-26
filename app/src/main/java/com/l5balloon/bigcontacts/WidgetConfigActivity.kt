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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import androidx.core.graphics.toColorInt

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
fun HexColorPicker(
    initialColor: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    var pickedColor by remember { mutableStateOf(initialColor) }
    var hexInput by remember { mutableStateOf(colorToHexNoAlpha(initialColor)) }
    val controller = rememberColorPickerController()

    // HEX 입력이 바뀌면 색상도 바꾼다 (alpha는 항상 FF)
    fun updateColorFromHex(hex: String) {
        val color = runCatching { Color(("#FF$hex").toColorInt()) }.getOrNull()
        if (color != null) {
            pickedColor = color
            onColorChanged(color)
        }
    }

    Column(modifier = modifier) {
        HsvColorPicker(
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth(),
            controller = controller,
            onColorChanged = { envelope ->
                // alpha는 무시하고 항상 FF로 변환
                val hexNoAlpha = colorToHexNoAlpha(envelope.color)
                pickedColor = Color("#$hexNoAlpha".toColorInt())
                hexInput = hexNoAlpha
                onColorChanged(pickedColor)
            }
        )
        Spacer(Modifier.height(16.dp))
        // Hex 코드
        Text(
            "HEX 코드",
            style = TextStyle(
                fontSize = dpToSp(36.dp)
            )
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "#FF",
                style = TextStyle(
                    fontSize = dpToSp(36.dp)
                )
            )
            OutlinedTextField(
                value = hexInput,
                onValueChange = {
                    // 6 글자만(RRGGBB) 허용
                    val filtered = it.filter { c -> c.isLetterOrDigit() }.take(6).uppercase()
                    hexInput = filtered
                    if (filtered.length == 6 && isValidHexNoAlpha(filtered)) updateColorFromHex(filtered)
                },
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    fontSize = dpToSp(36.dp)
                )
            )
        }
        Spacer(Modifier.height(8.dp))
        // 미리보기
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("미리보기: ")
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(pickedColor, shape = CircleShape)
                    .border(1.dp, Color.Gray, shape = CircleShape)
            )
        }
    }
}

// RRGGBB만 허용
fun isValidHexNoAlpha(hex: String): Boolean {
    val regex = Regex("([A-Fa-f0-9]{6})$")
    return regex.matches(hex)
}

// Color → RRGGBB 변환
fun colorToHexNoAlpha(color: Color): String {
    val intColor = color.toArgb()
    val rgb = intColor and 0x00FFFFFF
    return String.format("%06X", rgb)
}

@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var pickedColor by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("색상 선택") },
        text = {
            Column {
                HexColorPicker(
                    initialColor = initialColor,
                    onColorChanged = { pickedColor = it }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onColorSelected(pickedColor) }) {
                Text("선택")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
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

    // 사용자 정의 테마 목록
    var customThemes by remember { mutableStateOf(listOf<WidgetTheme>()) }

    // 테마 추가 다이얼로그 상태
    var showAddThemeDialog by remember { mutableStateOf(false) }
    var newThemeName by remember { mutableStateOf(TextFieldValue("")) }
    var newThemeBackgroundColor by remember { mutableStateOf(Color.Black) }
    var newThemeTextColor by remember { mutableStateOf(Color.White) }
    var showBackgroundColorPicker by remember { mutableStateOf(false) }
    var showTextColorPicker by remember { mutableStateOf(false) }

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

        // 테마 목록 + 테마 추가 버튼
        val allThemes = WidgetTheme.THEMES + customThemes
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
                items(allThemes) { theme ->
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
                // 사용자 테마 추가 버튼
                item {
                    Button(
                        onClick = { showAddThemeDialog = true },
                        shape = CircleShape,
                        modifier = Modifier.size(rowHeight),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("+", fontSize = 32.sp)
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

    // 테마 추가 다이얼로그
    if (showAddThemeDialog) {
        AlertDialog(
            onDismissRequest = { showAddThemeDialog = false },
            title = { Text("테마 추가") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 테마명 입력
                    Text("테마명")
                    OutlinedTextField(
                        value = newThemeName,
                        onValueChange = { newThemeName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    // 배경색 선택
                    Text("배경색 선택")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 미리보기 라운드 사각형
                        Box(
                            modifier = Modifier
                                .size(width = 120.dp, height = 56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(newThemeBackgroundColor)
                                .border(1.dp, Color.Gray, RoundedCornerShape(16.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        // 컬러휠 아이콘 (클릭 시 picker)
                        Icon(
                            painter = painterResource(id = R.drawable.ic_color_wheel),
                            contentDescription = "색상 선택",
                            modifier = Modifier
                                .size(56.dp)
                                .clickable { showBackgroundColorPicker = true },
                            tint = Color.Unspecified
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    // 글자색 선택
                    Text("글자색 선택")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(width = 120.dp, height = 56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(newThemeTextColor)
                                .border(1.dp, Color.Gray, RoundedCornerShape(16.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_color_wheel),
                            contentDescription = "색상 선택",
                            modifier = Modifier
                                .size(56.dp)
                                .clickable { showTextColorPicker = true },
                            tint = Color.Unspecified
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    // 예시 미리보기
                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .height(80.dp)
                            .background(newThemeBackgroundColor, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "미리보기",
                            color = newThemeTextColor,
                            fontSize = dpToSp(36.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newThemeName.text.isNotBlank()) {
                            val customTheme = WidgetTheme(
                                key = "custom_${System.currentTimeMillis()}",
                                nameResId = R.string.theme_custom,
                                backgroundColor = newThemeBackgroundColor,
                                textColor = newThemeTextColor
                            )
                            customThemes = customThemes + customTheme
                            selectedTheme = customTheme
                            showAddThemeDialog = false
                            newThemeName = TextFieldValue("")
                        } else {
                            // TODO : show toast
                        }
                    }
                ) { Text("추가") }
            },
            dismissButton = {
                Button(onClick = { showAddThemeDialog = false }) { Text("취소") }
            }
        )
    }

    // 배경색 컬러 피커 다이얼로그
    if (showBackgroundColorPicker) {
        ColorPickerDialog(
            initialColor = newThemeBackgroundColor,
            onColorSelected = {
                newThemeBackgroundColor = it
                showBackgroundColorPicker = false
                              },
            onDismiss = { showBackgroundColorPicker = false }
        )
    }
    // 글자색 컬러 피커 다이얼로그
    if (showTextColorPicker) {
        ColorPickerDialog(
            initialColor = newThemeTextColor,
            onColorSelected = {
                newThemeTextColor = it
                showTextColorPicker = false
            },
            onDismiss = { showTextColorPicker = false }
        )
    }
} 