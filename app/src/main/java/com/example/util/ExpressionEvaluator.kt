package com.example.util

import kotlin.math.*

object ExpressionEvaluator {

    fun evaluate(str: String, isDegree: Boolean = false): Double {
        return object {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < str.length) str[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) throw RuntimeException("Unexpected character: " + ch.toChar())
                return x
            }

            // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor | term `%` factor
            // factor = `+` factor | `-` factor | power
            // power = base `^` factor
            // base = number | constant | function | `(` expression `)` | base`!` (factorial)

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm() // addition
                    else if (eat('-'.code)) x -= parseTerm() // subtraction
                    else break
                }
                return x
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor() // multiplication
                    else if (eat('/'.code)) {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        x /= divisor // division
                    }
                    else if (eat('%'.code)) {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Modulo by zero")
                        x %= divisor // modulo
                    }
                    else break
                }
                return x
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor() // unary plus
                if (eat('-'.code)) return -parseFactor() // unary minus

                var x: Double
                val startPos = this.pos
                if (eat('('.code)) { // parentheses
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) { // numbers
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                    val numStr = str.substring(startPos, this.pos)
                    x = numStr.toDoubleOrNull() ?: throw RuntimeException("Invalid number: $numStr")
                } else if ((ch >= 'a'.code && ch <= 'z'.code) || (ch >= 'A'.code && ch <= 'Z'.code) || ch == 'π'.code) {
                    // functions or constants
                    while ((ch >= 'a'.code && ch <= 'z'.code) || (ch >= 'A'.code && ch <= 'Z'.code) || ch == 'π'.code) nextChar()
                    val name = str.substring(startPos, this.pos)
                    if (name == "π" || name == "pi") {
                        x = Math.PI
                    } else if (name == "e") {
                        x = Math.E
                    } else if (eat('('.code)) {
                        val arg = parseExpression()
                        eat(')'.code)
                        x = when (name) {
                            "sin" -> if (isDegree) sin(Math.toRadians(arg)) else sin(arg)
                            "cos" -> if (isDegree) cos(Math.toRadians(arg)) else cos(arg)
                            "tan" -> if (isDegree) tan(Math.toRadians(arg)) else tan(arg)
                            "asin" -> if (isDegree) Math.toDegrees(asin(arg)) else asin(arg)
                            "acos" -> if (isDegree) Math.toDegrees(acos(arg)) else acos(arg)
                            "atan" -> if (isDegree) Math.toDegrees(atan(arg)) else atan(arg)
                            "sqrt" -> {
                                if (arg < 0) throw ArithmeticException("Square root of negative number")
                                sqrt(arg)
                            }
                            "log" -> log10(arg)
                            "ln" -> ln(arg)
                            else -> throw RuntimeException("Unknown function: $name")
                        }
                    } else {
                        throw RuntimeException("Unknown constant or function: $name")
                    }
                } else {
                    throw RuntimeException("Unexpected character: " + ch.toChar())
                }

                // exponentiation
                if (eat('^'.code)) {
                    x = x.pow(parseFactor())
                }

                // factorial check
                if (eat('!'.code)) {
                    x = factorial(x)
                }

                return x
            }

            private fun factorial(n: Double): Double {
                if (n < 0.0 || n != floor(n)) throw IllegalArgumentException("Factorial is only defined for non-negative integers")
                if (n > 170.0) return Double.POSITIVE_INFINITY // overflow
                var result = 1.0
                for (i in 1..n.toInt()) {
                    result *= i
                }
                return result
            }
        }.parse()
    }

    /**
     * Sanitizes external human text (supports custom symbols like × and ÷, adds implicit multiplication if needed)
     */
    fun sanitizeAndPreprocess(expression: String): String {
        val cleaned = expression
            .replace(" ", "")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("PI", "π")
            .replace("pi", "π")

        // Handle implicit multiplication (e.g. "2(3+1)" -> "2*(3+1)", "2π" -> "2*π", ")(" -> ")*(")
        val builder = java.lang.StringBuilder()
        for (i in 0 until cleaned.length) {
            val curr = cleaned[i]
            builder.append(curr)
            if (i < cleaned.length - 1) {
                val next = cleaned[i + 1]
                
                // Determine if we need implicit multiplication
                val isCurrNumericOrPiOrEOrCloseParenOrFact = curr.isDigit() || curr == '.' || curr == 'π' || curr == 'e' || curr == ')' || curr == '!'
                val isNextPiOrEOrOpenParenOrLetter = next == 'π' || next == 'e' || next == '(' || next.isLetter()
                
                // Do not insert * before '(' if we are inside a function name (e.g., "sin(")
                // But if the previous characters form "sin", we shouldn't insert. Let's make sure:
                // If curr is a letter (e.g. 'n' in "sin(x)"), and next is '(', does it insert?
                // Wait! "sin(" -> curr is 'n', next is '('. If curr is a letter and next is '(', it is the start of a function call.
                // But what if we have "x(y)"? If 'x' is a variable, we want "x*(y)".
                // Indeed, standard function names in our app are sin, cos, tan, asin, acos, atan, sqrt, log, ln.
                // We should make sure we don't insert '*' for function calls.
                var isFunctionCall = false
                if (curr.isLetter() && next == '(') {
                    val wordStart = findWordStart(cleaned, i)
                    val word = cleaned.substring(wordStart, i + 1)
                    if (word in listOf("sin", "cos", "tan", "asin", "acos", "atan", "sqrt", "log", "ln")) {
                        isFunctionCall = true
                    }
                }

                if (isCurrNumericOrPiOrEOrCloseParenOrFact && isNextPiOrEOrOpenParenOrLetter && !isFunctionCall) {
                    builder.append('*')
                }
            }
        }
        return builder.toString()
    }

    private fun findWordStart(str: String, endIdx: Int): Int {
        var start = endIdx
        while (start > 0 && str[start - 1].isLetter()) {
            start--
        }
        return start
    }

    fun formattedResult(res: Double): String {
        if (res.isNaN()) return "Error: NaN"
        if (res.isInfinite()) return if (res > 0) "Error: Infinity" else "Error: -Infinity"
        
        // If it's a whole number, drop the decimal point
        val longVal = res.toLong()
        if (res == longVal.toDouble() && res <= 9e15 && res >= -9e15) {
            return longVal.toString()
        }
        
        // formats with up to 10 decimal places, stripping trailing zeros
        val formatted = String.format(java.util.Locale.US, "%.10f", res)
        if (formatted.contains(".")) {
            var trimmed = formatted.replace(Regex("0+$"), "")
            if (trimmed.endsWith(".")) {
                trimmed = trimmed.substring(0, trimmed.length - 1)
            }
            return trimmed
        }
        return formatted
    }
}
