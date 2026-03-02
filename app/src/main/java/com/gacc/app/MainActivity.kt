package com.gacc.app

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import android.app.DatePickerDialog
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.res.painterResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.InputStreamReader
import java.io.BufferedReader
import java.io.OutputStreamWriter

// --- Data Models ---
data class Expense(
    val id: String,
    val payer: String,
    val category: String,
    val detail: String? = "",
    val amount: Int,
    val timestamp: Long,
    val isSettled: Boolean = false
)

class MainActivity : ComponentActivity() {

    private val gson = Gson()
    private val PREF_NAME = "GAcc_Prefs"
    private val EXPENSES_KEY = "expenses"
    
    // Colors for the app
    private val BgColor = Color(0xFF0D0D12)
    private val CardBg = Color.White.copy(alpha = 0.05f)
    private val CardBorder = Color.White.copy(alpha = 0.1f)
    private val AccentColor = Color(0xFF7C3AED)
    private val SuccessColor = Color(0xFF10B981)
    private val DangerColor = Color(0xFFEF4444)
    private val TextSecondary = Color(0xFFA0A0B0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 支援 Edge-to-Edge 邊緣滿版顯示
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            var expenses by remember { mutableStateOf(loadExpenses()) }
            var currentTab by remember { mutableStateOf(0) } // 0: 記帳, 1: 結算
            var currentDate by remember { mutableStateOf(Calendar.getInstance()) }
            
            fun saveNewExpense(expense: Expense) {
                val updated = expenses + expense
                expenses = updated
                saveExpenses(updated)
            }

            fun deleteExpenseObj(id: String) {
                val updated = expenses.filter { it.id != id }
                expenses = updated
                saveExpenses(updated)
            }

            // Dark Theme basic
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = BgColor,
                    surface = BgColor,
                    primary = AccentColor,
                    onPrimary = Color.White,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgColor
                ) {
                    Scaffold(
                        modifier = Modifier.systemBarsPadding(),
                        bottomBar = {
                            BottomNavBar(currentTab) { currentTab = it }
                        }
                    ) { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(BgColor)
                                .padding(paddingValues)
                        ) {
                        if (currentTab == 0) {
                            RecordScreen(
                                onSave = { expense ->
                                    saveNewExpense(expense)
                                    Toast.makeText(this@MainActivity, "記帳成功！", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else if (currentTab == 1) {
                            SettleScreen(
                                expenses = expenses,
                                currentDate = currentDate,
                                onMonthChange = { offset ->
                                    val newDate = currentDate.clone() as Calendar
                                    newDate.add(Calendar.MONTH, offset)
                                    currentDate = newDate
                                },
                                onDelete = { deleteExpenseObj(it) },
                                onSettle = { startMs, endMs, person ->
                                    val updated = expenses.map { e ->
                                        if (e.payer == person && !e.category.startsWith("借還款") && e.timestamp in startMs..endMs) {
                                            e.copy(isSettled = true)
                                        } else {
                                            e
                                        }
                                    }
                                    expenses = updated
                                    saveExpenses(updated)
                                }
                            )
                        } else {
                            SettingsScreen(
                                expenses = expenses,
                                onImportSuccess = { importedList ->
                                    expenses = importedList
                                    saveExpenses(importedList)
                                    Toast.makeText(this@MainActivity, "匯入成功！", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
                }
            }
        }
    }

    private fun loadExpenses(): List<Expense> {
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(EXPENSES_KEY, "[]")
        val type = object : TypeToken<List<Expense>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    private fun saveExpenses(list: List<Expense>) {
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(list)
        prefs.edit().putString(EXPENSES_KEY, json).apply()
    }
}

// --- Composable Components ---

@Composable
fun BottomNavBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val bgColor = Color(0xFF0D0D12).copy(alpha = 0.95f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        NavBarItem(
            text = "記帳",
            isSelected = selectedTab == 0,
            modifier = Modifier.weight(1f)
        ) { onTabSelected(0) }
        NavBarItem(
            text = "結算明細",
            isSelected = selectedTab == 1,
            modifier = Modifier.weight(1f)
        ) { onTabSelected(1) }
        NavBarItem(
            text = "設定",
            isSelected = selectedTab == 2,
            modifier = Modifier.weight(1f)
        ) { onTabSelected(2) }
    }
}

@Composable
fun NavBarItem(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val color = if (isSelected) Color.White else Color(0xFFA0A0B0)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(1.dp, Color.White, RoundedCornerShape(12.dp))
                else Modifier.border(1.dp, Color.Transparent, RoundedCornerShape(12.dp))
            )
            .padding(vertical = 4.dp)
    ) {
        val icon = when(text) {
            "記帳" -> "✍️"
            "結算明細" -> "🧾"
            else -> "⚙️"
        }
        Text(text = icon, fontSize = 16.sp)
        Text(text = text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top=2.dp))
    }
}

@Composable
fun RecordScreen(onSave: (Expense) -> Unit) {
    val users = listOf("自己", "親愛兒", "家人")
    val categories = listOf("餐飲", "日常用品", "代付", "借還款")
    
    var selectedUser by remember { mutableStateOf(users[0]) }
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var selectedSubCategory by remember { mutableStateOf("借") }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var detailText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

    val sdfDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val context = androidx.compose.ui.platform.LocalContext.current
    val focusManager = LocalFocusManager.current

    val CardBg = Color.White.copy(alpha = 0.05f)
    val CardBorder = Color.White.copy(alpha = 0.1f)
    val AccentGradient = Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899)))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Glass Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CardBg)
                .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Text("日期", color = Color(0xFFA0A0B0), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .clickable {
                        DatePickerDialog(context, { _, y, m, d ->
                            val newCal = Calendar.getInstance()
                            newCal.set(y, m, d)
                            selectedDate = newCal
                        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
                    }
                    .padding(12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(sdfDate.format(selectedDate.time), color = Color.White, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("誰的花費", color = Color(0xFFA0A0B0), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                users.forEach { user ->
                    SelectionButton(
                        text = user,
                        isSelected = selectedUser == user,
                        modifier = Modifier.weight(1f)
                    ) { selectedUser = user }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("花費項目", color = Color(0xFFA0A0B0), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            // Use FlowRow style for categories
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.take(2).forEach { cat ->
                        SelectionButton(cat, selectedCategory == cat, Modifier.weight(1f)) { selectedCategory = cat }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.drop(2).forEach { cat ->
                        SelectionButton(cat, selectedCategory == cat, Modifier.weight(1f)) { selectedCategory = cat }
                    }
                }
            }
            
            if (selectedCategory == "借還款") {
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("借", "還").forEach { sub ->
                        SelectionButton(sub, selectedSubCategory == sub, Modifier.weight(1f)) { selectedSubCategory = sub }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("輸入細項", color = Color(0xFFA0A0B0), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = detailText,
                onValueChange = { detailText = it },
                placeholder = { Text("例如：池上便當...", color = Color(0xFFA0A0B0), fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7C3AED),
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("多少錢？", color = Color(0xFFA0A0B0), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("0", color = Color(0xFFA0A0B0), fontSize = 28.sp) },
                leadingIcon = { Text("NT$", color = Color(0xFFA0A0B0), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start=12.dp)) },
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7C3AED),
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    val amt = amountText.toIntOrNull()
                    if (amt == null || amt < 0) return@Button
                    
                    val realCategory = if (selectedCategory == "借還款") "借還款-$selectedSubCategory" else selectedCategory
                    
                    onSave(Expense(
                        id = UUID.randomUUID().toString(),
                        payer = selectedUser,
                        category = realCategory,
                        detail = detailText,
                        amount = amt,
                        timestamp = selectedDate.timeInMillis
                    ))
                    amountText = ""
                    detailText = ""
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentGradient),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Text("儲存紀錄", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SelectionButton(text: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bgColor = if (isSelected) Color(0xFF7C3AED) else Color.White.copy(alpha = 0.03f)
    val textColor = if (isSelected) Color.White else Color(0xFFA0A0B0)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = textColor, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
fun SettleScreen(
    expenses: List<Expense>,
    currentDate: Calendar,
    onMonthChange: (Int) -> Unit,
    onDelete: (String) -> Unit,
    onSettle: (Long, Long, String) -> Unit
) {
    val year = currentDate.get(Calendar.YEAR)
    val month = currentDate.get(Calendar.MONTH)
    val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    val users = listOf("自己", "親愛兒", "家人")
    var selectedPerson by remember { mutableStateOf(users[0]) }

    val monthExpenses = expenses.filter {
        val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
        cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
    }.sortedByDescending { it.timestamp }

    val filteredExpenses = monthExpenses.filter { it.payer == selectedPerson }
    val normalTotal = filteredExpenses.filter { it.category != "代付" && !it.category.startsWith("借還款") }.sumOf { it.amount }
    val proxyTotal = filteredExpenses.filter { it.category == "代付" }.sumOf { it.amount }
    
    // 計算累計至當月的借還款餘額 (延續到下個月折抵的功能)
    val accumulatedExpenses = expenses.filter {
        val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
        val expYear = cal.get(Calendar.YEAR)
        val expMonth = cal.get(Calendar.MONTH)
        expYear < year || (expYear == year && expMonth <= month)
    }.filter { it.payer == selectedPerson }
    
    val borrowTotal = accumulatedExpenses.filter { it.category == "借還款-借" }.sumOf { it.amount }
    val repayTotal = accumulatedExpenses.filter { it.category == "借還款-還" }.sumOf { it.amount }
    
    val total = normalTotal - proxyTotal
    val borrowBalance = borrowTotal - repayTotal
    
    val unsettledNormal = accumulatedExpenses.filter { !it.category.startsWith("借還款") && it.category != "代付" && !it.isSettled }.sumOf { it.amount }
    val unsettledProxy = accumulatedExpenses.filter { it.category == "代付" && !it.isSettled }.sumOf { it.amount }
    val settleBalance = unsettledNormal - unsettledProxy
    
    var showSettleDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            // Summary Card
            val CardBg = Color.White.copy(alpha = 0.05f)
            val CardBorder = Color.White.copy(alpha = 0.1f)
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardBg)
                    .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Text("本月花費統計", fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha=0.2f), RoundedCornerShape(12.dp)).padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onMonthChange(-1) }) { Text("<", color=Color(0xFFA0A0B0), fontSize=20.sp) }
                    Text(String.format("%d 年 %02d 月", year, month + 1), fontWeight = FontWeight.Medium)
                    IconButton(onClick = { onMonthChange(1) }) { Text(">", color=Color(0xFFA0A0B0), fontSize=20.sp) }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("查看對象", color = Color(0xFFA0A0B0), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    users.forEach { user ->
                        SelectionButton(
                            text = user,
                            isSelected = selectedPerson == user,
                            modifier = Modifier.weight(1f)
                        ) { selectedPerson = user }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${selectedPerson}的總支出", color = Color(0xFFA0A0B0), fontSize = 13.sp)
                    Text("NT$ ${String.format("%,d", total)}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                
                if (selectedPerson != "自己") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("剩餘借款", color = Color(0xFFA0A0B0), fontSize = 13.sp)
                        val balanceColor = if (borrowBalance >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                        Text("NT$ ${String.format("%,d", borrowBalance)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = balanceColor)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CardBorder))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("剩餘結清金額", color = Color(0xFFA0A0B0), fontSize = 13.sp)
                            Text("(上月未結清 + 本月前 - 代付)", color = Color(0xFFA0A0B0), fontSize = 10.sp)
                        }
                        val settleColor = if (settleBalance >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("NT$ ${String.format("%,d", settleBalance)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = settleColor)
                            Text("已結清", color = Color(0xFF7C3AED), fontSize = 12.sp, modifier = Modifier.border(1.dp, Color(0xFF7C3AED), RoundedCornerShape(4.dp)).clickable{ showSettleDialog = true }.padding(horizontal=6.dp, vertical=2.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("${selectedPerson}的花費明細", fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom=8.dp))
        }

        if (filteredExpenses.isEmpty()) {
            item {
                Text("這個月還沒有花費唷！", color = Color(0xFFA0A0B0), modifier = Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center)
            }
        } else {
            items(filteredExpenses) { expense ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical=4.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha=0.03f)).padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val categoryColor = when {
                                expense.category == "代付" -> Color(0xFFF59E0B)
                                expense.category == "借還款-借" -> Color(0xFFEF4444)
                                expense.category == "借還款-還" -> Color(0xFF00BCD4)
                                else -> Color.Unspecified
                            }
                            val displayCategory = if (expense.category.startsWith("借還款")) expense.category.split("-").last() + "款" else expense.category
                            Text(displayCategory, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = categoryColor)
                            if (expense.isSettled) {
                                Text("(已結)", fontSize = 11.sp, color = Color(0xFF10B981), modifier = Modifier.padding(start = 4.dp))
                            }
                            if (!expense.detail.isNullOrBlank()) {
                                Text(" • ${expense.detail}", fontSize = 11.sp, color = Color(0xFFA0A0B0), modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${sdf.format(Date(expense.timestamp))}", color = Color(0xFFA0A0B0), fontSize = 10.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val amountColor = when {
                            expense.category == "代付" -> Color(0xFFF59E0B)
                            expense.category == "借還款-借" -> Color(0xFFEF4444)
                            expense.category == "借還款-還" -> Color(0xFF00BCD4)
                            else -> Color.White
                        }
                        val prefix = when {
                            expense.category == "代付" -> "- NT$"
                            expense.category == "借還款-借" -> "- NT$"
                            expense.category == "借還款-還" -> "+ NT$"
                            else -> "NT$"
                        }
                        Text("$prefix ${String.format("%,d", expense.amount)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = amountColor)
                        Text("刪除", color = Color(0xFFEF4444), fontSize = 10.sp, modifier = Modifier.clickable { onDelete(expense.id) }.padding(6.dp))
                    }
                }
            }
        }
    }

    if (showSettleDialog) {
        var settleOption by remember { mutableStateOf("month") }
        var customStartDate by remember { mutableStateOf(Calendar.getInstance()) }
        var customEndDate by remember { mutableStateOf(Calendar.getInstance()) }
        val context = androidx.compose.ui.platform.LocalContext.current
        
        AlertDialog(
            onDismissRequest = { showSettleDialog = false },
            containerColor = Color(0xFF1C1C23),
            title = { Text("結清設定", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { settleOption = "month" }) {
                        RadioButton(selected = settleOption == "month", onClick = { settleOption = "month" }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF7C3AED)))
                        Text("整個月 (${year}/${month+1}及以前)", color = Color.White)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { settleOption = "custom" }) {
                        RadioButton(selected = settleOption == "custom", onClick = { settleOption = "custom" }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF7C3AED)))
                        Text("自訂區間", color = Color.White)
                    }
                    if (settleOption == "custom") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(customStartDate.time), color=Color(0xFF7C3AED), modifier=Modifier.clickable{
                                DatePickerDialog(context, {_, y, m, d -> 
                                    val newCal = Calendar.getInstance()
                                    newCal.set(y, m, d, 0, 0, 0)
                                    customStartDate = newCal
                                }, customStartDate.get(Calendar.YEAR), customStartDate.get(Calendar.MONTH), customStartDate.get(Calendar.DAY_OF_MONTH)).show()
                            })
                            Text("至", color=Color.White)
                            Text(SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(customEndDate.time), color=Color(0xFF7C3AED), modifier=Modifier.clickable{
                                DatePickerDialog(context, {_, y, m, d -> 
                                    val newCal = Calendar.getInstance()
                                    newCal.set(y, m, d, 23, 59, 59)
                                    customEndDate = newCal
                                }, customEndDate.get(Calendar.YEAR), customEndDate.get(Calendar.MONTH), customEndDate.get(Calendar.DAY_OF_MONTH)).show()
                            })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val startMs: Long
                    val endMs: Long
                    if (settleOption == "month") {
                        val endCal = Calendar.getInstance().apply {
                            set(year, month, 1, 23, 59, 59)
                            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                        }
                        startMs = 0L
                        endMs = endCal.timeInMillis
                    } else {
                        customStartDate.set(Calendar.HOUR_OF_DAY, 0)
                        customStartDate.set(Calendar.MINUTE, 0)
                        customStartDate.set(Calendar.SECOND, 0)
                        customEndDate.set(Calendar.HOUR_OF_DAY, 23)
                        customEndDate.set(Calendar.MINUTE, 59)
                        customEndDate.set(Calendar.SECOND, 59)
                        startMs = customStartDate.timeInMillis
                        endMs = customEndDate.timeInMillis
                    }
                    onSettle(startMs, endMs, selectedPerson)
                    showSettleDialog = false
                }) { Text("確認結清", color = Color(0xFF7C3AED)) }
            },
            dismissButton = {
                TextButton(onClick = { showSettleDialog = false }) { Text("取消", color = Color(0xFFA0A0B0)) }
            }
        )
    }
}

@Composable
fun SettingsScreen(
    expenses: List<Expense>,
    onImportSuccess: (List<Expense>) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
    }
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write("id,payer,category,detail,amount,timestamp,isSettled\n")
                        for (e in expenses) {
                            val detailEscaped = e.detail?.replace("\"", "\"\"") ?: ""
                            val detailFinal = if (detailEscaped.contains(",") || detailEscaped.contains("\"") || detailEscaped.contains("\n")) {
                                "\"$detailEscaped\""
                            } else {
                                detailEscaped
                            }
                            writer.write("${e.id},${e.payer},${e.category},$detailFinal,${e.amount},${e.timestamp},${e.isSettled}\n")
                        }
                    }
                }
                Toast.makeText(context, "匯出成功！", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "匯出失敗：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        val lines = reader.readLines()
                        if (lines.isNotEmpty()) {
                            val newExpenses = mutableListOf<Expense>()
                            // Start from 1 to skip header
                            for (i in 1 until lines.size) {
                                val line = lines[i]
                                if (line.isNotBlank()) {
                                    val tokens = mutableListOf<String>()
                                    var inQuotes = false
                                    var currentToken = StringBuilder()
                                    for (char in line) {
                                        if (char == '\"') {
                                            inQuotes = !inQuotes
                                        } else if (char == ',' && !inQuotes) {
                                            tokens.add(currentToken.toString())
                                            currentToken.setLength(0)
                                        } else {
                                            currentToken.append(char)
                                        }
                                    }
                                    tokens.add(currentToken.toString())

                                    if (tokens.size >= 6) {
                                        newExpenses.add(Expense(
                                            id = tokens[0],
                                            payer = tokens[1],
                                            category = tokens[2],
                                            detail = tokens[3].replace("\"\"", "\""),
                                            amount = tokens[4].toIntOrNull() ?: 0,
                                            timestamp = tokens[5].toLongOrNull() ?: 0L,
                                            isSettled = if (tokens.size >= 7) tokens[6].toBooleanStrictOrNull() ?: false else false
                                        ))
                                    }
                                }
                            }
                            if (newExpenses.isNotEmpty()) {
                                onImportSuccess(newExpenses)
                            } else {
                                Toast.makeText(context, "找不到有效的紀錄可以匯入！", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "檔案是空的！", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "匯入失敗：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val cardBg = Color.White.copy(alpha = 0.05f)
        val cardBorder = Color.White.copy(alpha = 0.1f)
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("資料備份與還原", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                    val filename = "GAcc_Backup_${sdf.format(java.util.Date())}.csv"
                    exportLauncher.launch(filename)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
            ) {
                Text("匯出備份 (CSV 檔)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = {
                    importLauncher.launch(arrayOf("text/comma-separated-values", "text/csv", "*/*", "application/csv"))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C3AED))
            ) {
                Text("匯入備份 (CSV 檔)", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("⚠️ 警告：匯入功能將會覆蓋您目前的裝置內資料，\n請務必確認後再進行操作喔！", color = Color(0xFFEF4444), fontSize = 12.sp, textAlign = TextAlign.Center)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("當前版本：v$versionName", color = Color(0xFFA0A0B0), fontSize = 12.sp)
        
        Spacer(modifier = Modifier.height(16.dp))
        var isCheckingUpdate by remember { mutableStateOf(false) }
        Button(
            onClick = {
                UpdateHelper.checkForUpdate(
                    context = context,
                    currentVersionName = versionName,
                    onChecking = { isCheckingUpdate = true },
                    onNoUpdate = { 
                        isCheckingUpdate = false
                        Toast.makeText(context, "目前已經是最新版本囉！", Toast.LENGTH_SHORT).show()
                    },
                    onUpdateFound = {
                        isCheckingUpdate = false
                    }
                )
            },
            modifier = Modifier
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            enabled = !isCheckingUpdate
        ) {
            Text(if (isCheckingUpdate) "正在檢查..." else "檢查最新版本", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
