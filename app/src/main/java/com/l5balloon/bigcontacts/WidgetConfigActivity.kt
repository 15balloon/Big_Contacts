package com.l5balloon.bigcontacts

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.graphics.toColorInt

import androidx.compose.foundation.Canvas
import androidx.compose.material3.Text
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.*
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

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

fun Float.toRadians() = (this / 180f) * Math.PI.toFloat()
fun Float.toDegrees() = (this * 180f) / Math.PI.toFloat()

fun makeColorWheelBitmap(width: Int, height: Int, value: Float): Bitmap {
    val bitmap = createBitmap(width, height)
    val centerX = width / 2f
    val centerY = height / 2f
    val radius = min(centerX, centerY)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val dx = x - centerX
            val dy = y - centerY
            val r = sqrt(dx * dx + dy * dy)
            if (r <= radius) {
                val hue = (atan2(dy, dx).toDegrees() + 360) % 360
                val sat = (r / radius).coerceIn(0f, 1f)
                val color = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
                bitmap[x, y] = color
            } else {
                bitmap[x, y] = android.graphics.Color.TRANSPARENT
            }
        }
    }
    return bitmap
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
fun BrightnessSlider(
    hue: Float,
    saturation: Float,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    // 그라데이션용 색상 샘플링 (10개)
    val colors = remember(hue, saturation) {
        List(11) { i ->
            val v = i / 10f
            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, v)))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(
                brush = Brush.horizontalGradient(colors),
                shape = RoundedCornerShape(8.dp)
            )
            .border(1.dp, Color.Black, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            ),
        )
    }
}

@Composable
fun RGBSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    channel: String, // "R", "G", "B"
    fixedR: Int,
    fixedG: Int,
    fixedB: Int,
    modifier: Modifier = Modifier
) {
    // 트랙용 그라데이션 색상 계산
    val gradientColors = remember(fixedR, fixedG, fixedB, channel) {
        when (channel) {
            "R" -> listOf(
                Color(0, fixedG, fixedB),
                Color(255, fixedG, fixedB)
            )
            "G" -> listOf(
                Color(fixedR, 0, fixedB),
                Color(fixedR, 255, fixedB)
            )
            "B" -> listOf(
                Color(fixedR, fixedG, 0),
                Color(fixedR, fixedG, 255)
            )
            else -> listOf(Color.Black, Color.White)
        }
    }

    Column(modifier = modifier) {
        Text(channel, modifier = Modifier.padding(bottom = 4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(
                    brush = Brush.horizontalGradient(gradientColors),
                    shape = RoundedCornerShape(8.dp)
                )
                .border(1.dp, Color.Black, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = 0f..255f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun RGBSliders(
    r: Int, g: Int, b: Int,
    onRChange: (Int) -> Unit,
    onGChange: (Int) -> Unit,
    onBChange: (Int) -> Unit
) {
    Column {
        RGBSlider(
            value = r,
            onValueChange = onRChange,
            channel = "R",
            fixedR = r, fixedG = g, fixedB = b
        )
        Spacer(Modifier.height(8.dp))
        RGBSlider(
            value = g,
            onValueChange = onGChange,
            channel = "G",
            fixedR = r, fixedG = g, fixedB = b
        )
        Spacer(Modifier.height(8.dp))
        RGBSlider(
            value = b,
            onValueChange = onBChange,
            channel = "B",
            fixedR = r, fixedG = g, fixedB = b
        )
    }
}

@Composable
fun BitmapColorWheelPicker(
    initialColor: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    // HSV 상태
    val hsv = remember { FloatArray(3) }
    var pickedColor by remember(initialColor) { mutableStateOf(initialColor) }
    var hexInput by remember(initialColor) { mutableStateOf(colorToHexNoAlpha(initialColor)) }
    var rgb by remember(initialColor) {
        mutableStateOf(
            Triple(
                (initialColor.red * 255).roundToInt(),
                (initialColor.green * 255).roundToInt(),
                (initialColor.blue * 255).roundToInt()
            )
        )
    }

    // Bitmap 캐싱 (밝기(V) 바뀔 때마다 새로 생성)
    val density = LocalDensity.current
    val sizePx = with(density) { size.roundToPx() }
    val bitmap = remember(hsv[2], sizePx) { makeColorWheelBitmap(sizePx, sizePx, hsv[2]) }

    // HSV → Color 변환
    fun updateColorFromHSV() {
        val colorInt = android.graphics.Color.HSVToColor(0xFF, hsv)
        val color = Color(colorInt)
        pickedColor = color
        hexInput = colorToHexNoAlpha(color)
        rgb = Triple(
            (color.red * 255).roundToInt(),
            (color.green * 255).roundToInt(),
            (color.blue * 255).roundToInt()
        )
        onColorChanged(color)
    }

    // HEX 입력 → HSV/Color 변환
    fun updateColorFromHex(hex: String) {
        val color = runCatching { Color(("#FF$hex").toColorInt()) }.getOrNull()
        if (color != null) {
            pickedColor = color
            android.graphics.Color.colorToHSV(color.toArgb(), hsv)
            rgb = Triple(
                (color.red * 255).roundToInt(),
                (color.green * 255).roundToInt(),
                (color.blue * 255).roundToInt()
            )
            onColorChanged(color)
        }
    }

    // Color 동기화 from RGB
    fun updateColorFromRGB(r: Int, g: Int, b: Int) {
        val color = Color(r, g, b)
        pickedColor = color
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        hexInput = colorToHexNoAlpha(color)
        onColorChanged(color)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 컬러휠
        Box(
            modifier = Modifier
                .size(size)
                .pointerInput(hsv[2]) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            if (change.changedToDown()) {
                                // 터치(탭) 처리
                                if (hsv[2] == 0f) continue // 밝기 0일 땐 움직이지 않게
                                val center = Offset(sizePx / 2f, sizePx / 2f)
                                val dx = change.position.x - center.x
                                val dy = change.position.y - center.y
                                val r = sqrt(dx * dx + dy * dy)
                                val radius = sizePx / 2f
                                if (r <= radius) {
                                    val hue = (atan2(dy, dx).toDegrees() + 360) % 360
                                    val sat = (r / radius).coerceIn(0f, 1f)
                                    hsv[0] = hue
                                    hsv[1] = sat
                                    updateColorFromHSV()
                                }
                            }
                            if (change.pressed) {
                                // 드래그 처리
                                if (hsv[2] == 0f) continue // 밝기 0일 땐 움직이지 않게
                                val center = Offset(sizePx / 2f, sizePx / 2f)
                                val dx = change.position.x - center.x
                                val dy = change.position.y - center.y
                                val r = sqrt(dx * dx + dy * dy)
                                val radius = sizePx / 2f
                                if (r <= radius) {
                                    val hue = (atan2(dy, dx).toDegrees() + 360) % 360
                                    val sat = (r / radius).coerceIn(0f, 1f)
                                    hsv[0] = hue
                                    hsv[1] = sat
                                    updateColorFromHSV()
                                }
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawImage(bitmap.asImageBitmap())
                // 현재 선택 위치 표시
                val radius = sizePx / 2f
                val selR = radius * hsv[1]
                val selX = center.x + cos(hsv[0].toRadians()) * selR
                val selY = center.y + sin(hsv[0].toRadians()) * selR
                drawCircle(
                    color = Color.White,
                    radius = 16f,
                    center = Offset(selX, selY),
                    style = Stroke(width = 3f)
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        // 밝기 슬라이더
        BrightnessSlider(
            hue = hsv[0],
            saturation = hsv[1],
            value = hsv[2],
            onValueChange = {
                hsv[2] = it
                updateColorFromHSV()
            }
        )
        Spacer(Modifier.height(16.dp))

        // RGB 슬라이더
        RGBSliders(
            r = rgb.first,
            g = rgb.second,
            b = rgb.third,
            onRChange = { r ->
                rgb = Triple(r, rgb.second, rgb.third)
                updateColorFromRGB(r, rgb.second, rgb.third)
            },
            onGChange = { g ->
                rgb = Triple(rgb.first, g, rgb.third)
                updateColorFromRGB(rgb.first, g, rgb.third)
            },
            onBChange = { b ->
                rgb = Triple(rgb.first, rgb.second, b)
                updateColorFromRGB(rgb.first, rgb.second, b)
            }
        )
        Spacer(Modifier.height(16.dp))

        // HEX 코드 입력
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(200.dp)
        ) {
            Text("#FF")
            OutlinedTextField(
                value = hexInput,
                onValueChange = {
                    val filtered = it.filter { c -> c.isLetterOrDigit() }.take(6).uppercase()
                    hexInput = filtered
                    if (filtered.length == 6 && isValidHexNoAlpha(filtered)) {
                        updateColorFromHex(filtered)
                    }
                },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
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
                    .padding(vertical = 16.dp, horizontal = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "색상 선택",
                    modifier = Modifier.align(Alignment.Start),
                    style = MaterialTheme.typography.titleLarge
                )
                BitmapColorWheelPicker(
                    initialColor = initialColor,
                    onColorChanged = { pickedColor = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
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
                        Text("취소")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onColorSelected(pickedColor) }) {
                        Text("선택")
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
                text = "미리보기",
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
                    "테마 추가",
                    modifier = Modifier.align(Alignment.Start),
                    style = MaterialTheme.typography.titleLarge
                )

                // 테마명 입력
                OutlinedTextField(
                    value = themeName,
                    onValueChange = { themeName = it },
                    label = { Text("테마명") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                // 배경색 선택
                Text(
                    "배경색 선택",
                    modifier = Modifier.align(Alignment.Start)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 색상 미리보기
                    Box(
                        modifier = Modifier
                            .size(width = 120.dp, height = 56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(backgroundColor)
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
                Text(
                    "글자색 선택",
                    modifier = Modifier.align(Alignment.Start)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 색상 미리보기
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
                        contentDescription = "색상 선택",
                        modifier = Modifier
                            .size(56.dp)
                            .clickable { showTextColorPicker = true },
                        tint = Color.Unspecified
                    )
                }
                Spacer(Modifier.height(16.dp))

                // 예시 미리보기
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
                        Text("취소")
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
                            // TODO : show toast
                        }
                    }) { Text("추가") }
                }
            }
        }
    }

    // 배경색 컬러 피커 다이얼로그
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
    // 글자색 컬러 피커 다이얼로그
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
                                if (isSelected) Modifier.border(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(16.dp)
                                )
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
                            if (theme.nameResId != null) {
                                stringResource(id = theme.nameResId)
                            } else {
                                theme.name
                            } ?: "",
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
        AddThemeDialog(
            onAdd = { theme ->
                customThemes = customThemes + theme
                selectedTheme = theme
                showAddThemeDialog = false
            },
            onDismiss = { showAddThemeDialog = false }
        )
    }
} 