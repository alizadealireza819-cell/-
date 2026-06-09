package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.CalculatorDatabase
import com.example.data.database.HistoryEntity
import com.example.data.repository.HistoryRepository
import com.example.util.ExpressionEvaluator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HistoryRepository
    private val sharedPrefs = application.getSharedPreferences("calculator_settings", Context.MODE_PRIVATE)

    init {
        val database = CalculatorDatabase.getDatabase(application)
        repository = HistoryRepository(database.historyDao())
    }

    val historyList: StateFlow<List<HistoryEntity>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _expression = MutableStateFlow("")
    val expression: StateFlow<String> = _expression.asStateFlow()

    private val _realTimeResult = MutableStateFlow("")
    val realTimeResult: StateFlow<String> = _realTimeResult.asStateFlow()

    private val _isDegree = MutableStateFlow(false)
    val isDegree: StateFlow<Boolean> = _isDegree.asStateFlow()

    private val _showHistory = MutableStateFlow(false)
    val showHistory: StateFlow<Boolean> = _showHistory.asStateFlow()

    private val _selectedTheme = MutableStateFlow(sharedPrefs.getString("selected_theme", "slate") ?: "slate")
    val selectedTheme: StateFlow<String> = _selectedTheme.asStateFlow()

    private val _showThemeSelector = MutableStateFlow(false)
    val showThemeSelector: StateFlow<Boolean> = _showThemeSelector.asStateFlow()

    fun selectTheme(themeId: String) {
        _selectedTheme.value = themeId
        sharedPrefs.edit().putString("selected_theme", themeId).apply()
    }

    fun toggleThemeSelector() {
        _showThemeSelector.value = !_showThemeSelector.value
    }

    fun toggleAngleMode() {
        _isDegree.value = !_isDegree.value
        recalculateRealTime()
    }

    fun toggleHistory() {
        _showHistory.value = !_showHistory.value
    }

    fun onKeyPress(key: String) {
        when (key) {
            "C" -> {
                _expression.value = ""
                _realTimeResult.value = ""
            }
            "⌫" -> {
                val expr = _expression.value
                if (expr.isNotEmpty()) {
                    _expression.value = when {
                        expr.endsWith("asin(") || expr.endsWith("acos(") || expr.endsWith("atan(") -> {
                            expr.substring(0, expr.length - 5)
                        }
                        expr.endsWith("sqrt(") -> {
                            expr.substring(0, expr.length - 5)
                        }
                        expr.endsWith("sin(") || expr.endsWith("cos(") || expr.endsWith("tan(") || expr.endsWith("log(") -> {
                            expr.substring(0, expr.length - 4)
                        }
                        expr.endsWith("ln(") -> {
                            expr.substring(0, expr.length - 3)
                        }
                        else -> {
                            expr.substring(0, expr.length - 1)
                        }
                    }
                }
                recalculateRealTime()
            }
            "=" -> {
                evaluateAndSave()
            }
            "+/-" -> {
                invertLastNumber()
                recalculateRealTime()
            }
            else -> {
                // If the user taps a function, ensure '(' is included for convenient entry
                val keyToAdd = when (key) {
                    "sin", "cos", "tan", "sqrt", "ln", "log", "asin", "acos", "atan" -> "$key("
                    else -> key
                }
                _expression.value += keyToAdd
                recalculateRealTime()
            }
        }
    }

    private fun recalculateRealTime() {
        val currentExpr = _expression.value
        if (currentExpr.isEmpty()) {
            _realTimeResult.value = ""
            return
        }
        try {
            val processed = ExpressionEvaluator.sanitizeAndPreprocess(currentExpr)
            val result = ExpressionEvaluator.evaluate(processed, _isDegree.value)
            _realTimeResult.value = ExpressionEvaluator.formattedResult(result)
        } catch (e: Exception) {
            // Do not show full error in real-time, just keep it blank or show preview indicator
            _realTimeResult.value = ""
        }
    }

    private fun evaluateAndSave() {
        val currentExpr = _expression.value
        if (currentExpr.isEmpty()) return

        try {
            val processed = ExpressionEvaluator.sanitizeAndPreprocess(currentExpr)
            val resultValue = ExpressionEvaluator.evaluate(processed, _isDegree.value)
            val formatted = ExpressionEvaluator.formattedResult(resultValue)

            // Save to Database
            if (!formatted.startsWith("Error")) {
                viewModelScope.launch {
                    repository.insert(
                        HistoryEntity(
                            expression = currentExpr,
                            result = formatted
                        )
                    )
                }
            }

            _expression.value = formatted
            _realTimeResult.value = ""
        } catch (e: Exception) {
            _realTimeResult.value = "Error: " + (e.message ?: "Invalid syntax")
        }
    }

    private fun invertLastNumber() {
        val expr = _expression.value
        if (expr.isEmpty()) return

        // Find the block of numbers at the end of the expression
        val regex = Regex("([0-9.]+)$")
        val match = regex.find(expr)
        if (match != null) {
            val numStr = match.value
            val prefix = expr.substring(0, match.range.first)
            if (prefix.endsWith("-")) {
                // If it was negative, change to positive
                // Wait, check if "-" was unary or part of operator expression
                val possibleOperator = prefix.dropLast(1)
                if (possibleOperator.isEmpty() || possibleOperator.last() in listOf('+', '-', '×', '÷', '*', '/', '^', '%', '(')) {
                    _expression.value = possibleOperator + numStr
                } else {
                    _expression.value = prefix.dropLast(1) + "+" + numStr
                }
            } else if (prefix.endsWith("+")) {
                _expression.value = prefix.dropLast(1) + "-" + numStr
            } else {
                _expression.value = prefix + "-" + numStr
            }
        }
    }

    fun loadHistoryItem(item: HistoryEntity) {
        _expression.value = item.expression
        recalculateRealTime()
        _showHistory.value = false
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clear()
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CalculatorViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CalculatorViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
