package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.HistoryEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.CalculatorViewModel

data class CalcTheme(
    val id: String,
    val name: String,
    val colors: List<Color>
)

val themeList = listOf(
    CalcTheme("slate", "کاسمیک", listOf(Color(0xFF0F172A), Color(0xFF020617))),
    CalcTheme("aurora", "شفق قطبی", listOf(Color(0xFF0F172A), Color(0xFF0D9488), Color(0xFF4C1D95))),
    CalcTheme("cyberpunk", "سایبر", listOf(Color(0xFF03001E), Color(0xFF7303C0), Color(0xFFEC38BC))),
    CalcTheme("emerald", "زمردین", listOf(Color(0xFF022C22), Color(0xFF064E3B), Color(0xFF020617))),
    CalcTheme("sunset", "غروب", listOf(Color(0xFF1E1B4B), Color(0xFF7C2D12), Color(0xFF311042))),
    CalcTheme("minimal", "تاریک", listOf(Color(0xFF050505), Color(0xFF1E293B)))
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val expression by viewModel.expression.collectAsState()
    val realTimeResult by viewModel.realTimeResult.collectAsState()
    val isDegree by viewModel.isDegree.collectAsState()
    val showHistory by viewModel.showHistory.collectAsState()
    val historyList by viewModel.historyList.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val showThemeSelector by viewModel.showThemeSelector.collectAsState()

    val clipboardManager = LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    // Adaptive Check based on orientation and screen width
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isLargeScreen = configuration.screenWidthDp >= 600

    // Compute active wallpaper gradient Brush
    val backgroundBrush = when (selectedTheme) {
        "minimal" -> Brush.verticalGradient(listOf(Color(0xFF050505), Color(0xFF0F1115)))
        "slate" -> Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF020617)))
        "aurora" -> Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF0F766E), Color(0xFF4C1D95)))
        "cyberpunk" -> Brush.verticalGradient(listOf(Color(0xFF1F0022), Color(0xFF3F0042), Color(0xFF000000)))
        "emerald" -> Brush.verticalGradient(listOf(Color(0xFF022C22), Color(0xFF064E3B), Color(0xFF021612)))
        "sunset" -> Brush.verticalGradient(listOf(Color(0xFF2E1500), Color(0xFF7C2D12), Color(0xFF1A0B2E)))
        else -> Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF020617)))
    }

    Scaffold(
        modifier = modifier.fillMaxSize().background(backgroundBrush),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Calculator Section
            Column(
                modifier = Modifier
                    .weight(if (isLandscape || isLargeScreen) 1.5f else 1f)
                    .fillMaxHeight()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Display Card (Expression & Result Output with Gradient Borders)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(CardDisplayBackground.copy(alpha = 0.85f))
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Title / Top action row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // RAD / DEG indicator and toggle
                            Surface(
                                onClick = { viewModel.toggleAngleMode() },
                                shape = RoundedCornerShape(12.dp),
                                color = KeyFunctionBg.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isDegree) AccentTeal else KeyOperatorBg)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isDegree) "DEG" else "RAD",
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Clipboard or History Toggle
                            Row {
                                IconButton(onClick = { viewModel.toggleThemeSelector() }) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = "تم‌ها",
                                        tint = if (showThemeSelector) AccentTeal else TextPrimary
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (expression.isNotEmpty()) {
                                            clipboardManager.setText(AnnotatedString(expression))
                                            Toast.makeText(context, "کپی شد", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = expression.isNotEmpty()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "کپی کردن عبارت",
                                        tint = if (expression.isNotEmpty()) TextPrimary else TextSecondary.copy(alpha = 0.4f)
                                    )
                                }

                                IconButton(onClick = { viewModel.toggleHistory() }) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "تاریخچه",
                                        tint = if (showHistory) AccentTeal else TextPrimary
                                    )
                                }
                            }
                        }

                        // Display text (Dynamic expression with horizontal scaling/scrolling)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.End
                        ) {
                            // Main Expression text
                            val scrollState = rememberScrollState()
                            LaunchedEffect(expression) {
                                scrollState.scrollTo(scrollState.maxValue)
                            }
                            Text(
                                text = expression.ifEmpty { "0" },
                                style = LocalTextStyle.current.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Light,
                                    fontSize = if (expression.length > 15) 28.sp else 38.sp,
                                    textAlign = TextAlign.End
                                ),
                                color = if (expression.isEmpty()) TextSecondary.copy(alpha = 0.5f) else TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(scrollState)
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = {
                                            if (expression.isNotEmpty()) {
                                                clipboardManager.setText(AnnotatedString(expression))
                                                Toast.makeText(context, "کپی شد", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Dynamic Live Result Text
                            AnimatedVisibility(
                                visible = realTimeResult.isNotEmpty(),
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Text(
                                    text = realTimeResult,
                                    style = LocalTextStyle.current.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 24.sp,
                                        textAlign = TextAlign.End
                                    ),
                                    color = if (realTimeResult.startsWith("Error")) AccentTeal else AccentTealLight,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Beautiful theme selector scrollable row
                AnimatedVisibility(
                    visible = showThemeSelector,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(CardDisplayBackground.copy(alpha = 0.6f))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            themeList.forEach { theme ->
                                val isSelected = theme.id == selectedTheme
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { viewModel.selectTheme(theme.id) }
                                        .background(
                                            if (isSelected) AccentTeal.copy(alpha = 0.15f)
                                            else Color.Transparent
                                        )
                                        .padding(8.dp)
                                        .width(68.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Brush.verticalGradient(theme.colors))
                                            .then(
                                                if (isSelected) Modifier.border(2.dp, AccentTeal, CircleShape) else Modifier
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = theme.name,
                                        color = if (isSelected) AccentTealLight else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Keypad Area (Responsive Multi-layer grid layout)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Scientific Functions Section (Horizontal swiper panel or 2x6 inline grid)
                    var isSciExpanded by remember { mutableStateOf(!isLandscape && !isLargeScreen) }

                    // Expandable toggle button
                    if (!isLandscape && !isLargeScreen) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                onClick = { isSciExpanded = !isSciExpanded },
                                shape = RoundedCornerShape(16.dp),
                                color = KeyFunctionBg.copy(alpha = 0.3f),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isSciExpanded) "مخفی کردن توابع" else "نمایش توابع مهندسی",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (isSciExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "توابع ریاضی",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Sliding/Collapsible Scientific Functions Grid
                    AnimatedVisibility(
                        visible = isSciExpanded || isLandscape || isLargeScreen,
                        enter = slideInVertically(animationSpec = spring(stiffness = 500f)) { -it / 2 } + fadeIn(),
                        exit = slideOutVertically() { -it / 2 } + fadeOut()
                    ) {
                        ScientificGrid(
                            onKeyPress = { viewModel.onKeyPress(it) },
                            isLandscape = isLandscape || isLargeScreen
                        )
                    }

                    // Base Numeric and Operator Grid
                    BaseGrid(
                        onKeyPress = { viewModel.onKeyPress(it) },
                        isLandscape = isLandscape || isLargeScreen
                    )
                }
            }

            // Right Column Side Panel: History (Adaptive - visible inline or overlay slide-in)
            if (isLandscape || isLargeScreen || showHistory) {
                Surface(
                    modifier = Modifier
                        .weight(if (isLandscape || isLargeScreen) 0.8f else 1.2f)
                        .fillMaxHeight(),
                    color = CardDisplayBackground.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
                ) {
                    HistoryPanel(
                        historyList = historyList,
                        onLoadItem = { viewModel.loadHistoryItem(it) },
                        onDeleteItem = { viewModel.deleteHistoryItem(it) },
                        onClearAll = { viewModel.clearHistory() },
                        onClose = { viewModel.toggleHistory() },
                        showCloseButton = !isLandscape && !isLargeScreen
                    )
                }
            }
        }
    }
}

@Composable
fun ScientificGrid(
    onKeyPress: (String) -> Unit,
    isLandscape: Boolean
) {
    val items = listOf(
        listOf("sin", "cos", "tan", "^", "√"),
        listOf("ln", "log", "π", "e", "!")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { op ->
                    CalculatorButton(
                        text = op,
                        backgroundColor = KeyFunctionBg,
                        contentColor = TextSecondary,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val actualOp = when (op) {
                                "√" -> "sqrt"
                                else -> op
                            }
                            onKeyPress(actualOp)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BaseGrid(
    onKeyPress: (String) -> Unit,
    isLandscape: Boolean
) {
    val rows = listOf(
        listOf("C", "⌫", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("+/-", "0", ".", "=")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { buttonText ->
                    val isOperator = buttonText in listOf("÷", "×", "-", "+", "=")
                    val isAction = buttonText in listOf("C", "⌫", "%", "+/-")

                    val bg = when {
                        buttonText == "=" -> KeyOperatorBg
                        isOperator -> KeyOperatorBg.copy(alpha = 0.9f)
                        isAction -> KeyFunctionBg
                        else -> KeyNumberBg
                    }

                    val fg = when {
                        buttonText == "=" -> Color.White
                        isOperator -> Color.White
                        isAction -> TextPrimary
                        else -> TextPrimary
                    }

                    CalculatorButton(
                        text = buttonText,
                        backgroundColor = bg,
                        contentColor = fg,
                        modifier = Modifier.weight(1f),
                        onClick = { onKeyPress(buttonText) }
                    )
                }
            }
        }
    }
}

@Composable
fun CalculatorButton(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(if (text in listOf("sin", "cos", "tan", "ln", "log", "π", "e", "!", "^", "√")) 42.dp else 54.dp),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        tonalElevation = 2.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (text == "⌫") {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = "Clear character",
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = text,
                    color = contentColor,
                    fontSize = if (text.length > 3) 14.sp else 19.sp,
                    fontWeight = if (text in listOf("=", "C")) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun HistoryPanel(
    historyList: List<HistoryEntity>,
    onLoadItem: (HistoryEntity) -> Unit,
    onDeleteItem: (Int) -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit,
    showCloseButton: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "تاریخچه محاسبات",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (historyList.isNotEmpty()) {
                    IconButton(onClick = onClearAll) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "حذف همه تاریخچه",
                            tint = Color.Red.copy(alpha = 0.8f)
                        )
                    }
                }
                if (showCloseButton) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن پنل تاریخچه",
                            tint = TextPrimary
                        )
                    }
                }
            }
        }

        Divider(
            color = TextSecondary.copy(alpha = 0.2f),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = "محاسباتی یافت نشد",
                        tint = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "تاریخچه خالی است",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historyList, key = { it.id }) { item ->
                    HistoryItemView(
                        item = item,
                        onLoad = { onLoadItem(item) },
                        onDelete = { onDeleteItem(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItemView(
    item: HistoryEntity,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLoad() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = KeyNumberBg.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.expression,
                    color = TextPrimary.copy(alpha = 0.8f),
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "پاک کردن",
                        tint = TextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "= ${item.result}",
                color = AccentTeal,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
