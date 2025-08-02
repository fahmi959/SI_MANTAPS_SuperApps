package com.presensisiswainformatikabyfahmi

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.presensisiswainformatikabyfahmi.databinding.ActivityKalkulatorBinding
import net.objecthunter.exp4j.ExpressionBuilder
import net.objecthunter.exp4j.function.Function

class KalkulatorActivity : AppCompatActivity() {

    private var lastAnswer: Double = 0.0
    private lateinit var binding: ActivityKalkulatorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKalkulatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ganti label tombol
        binding.sinBtn.text = "sin°"
        binding.cosBtn.text = "cos°"
        binding.tanBtn.text = "tan°"


        val numberButtons = listOf(
            binding.btn0, binding.btn1, binding.btn2,
            binding.btn3, binding.btn4, binding.btn5,
            binding.btn6, binding.btn7, binding.btn8,
            binding.btn9
        )


        fun appendToExpression(newText: String) {
            val current = binding.calTxt.text.toString()
            if (current == "0" || current.isEmpty()) {
                binding.calTxt.text = newText
            } else {
                binding.calTxt.append(newText)
            }
        }


        numberButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                appendToExpression(index.toString())
            }
        }

        // Operator dan fungsi umum
        binding.plusBtn.setOnClickListener { appendToExpression("+") }
        binding.minusBtn.setOnClickListener { appendToExpression("-") }
        binding.multBtn.setOnClickListener { appendToExpression("*") }
        binding.divBtn.setOnClickListener { appendToExpression("/") }
        binding.dotBtn.setOnClickListener { appendToExpression(",") }
        binding.brac1.setOnClickListener { appendToExpression("(") }
        binding.brac2.setOnClickListener { appendToExpression(")") }
        binding.sqrtBtn.setOnClickListener { appendToExpression("akar(") }
        binding.sqrt3Btn.setOnClickListener { appendToExpression("akar^3(") }
        binding.sqrtnBtn.setOnClickListener { appendToExpression("ⁿ√x(") }
        binding.piBtn.setOnClickListener { appendToExpression("pi") }
        binding.powBtn.setOnClickListener { appendToExpression("^") }
        binding.expBtn.setOnClickListener { appendToExpression("e^") }
        binding.percentBtn.setOnClickListener { appendToExpression("/100") }
        binding.logBtn.setOnClickListener { appendToExpression("log10(") }
        binding.lnBtn.setOnClickListener { appendToExpression("logaritma_natural(") }
        binding.komaBtn.setOnClickListener { appendToExpression(";") }


        // Fungsi trigonometri
        binding.sinBtn.setOnClickListener { appendToExpression("sin(") }
        binding.cosBtn.setOnClickListener { appendToExpression("cos(") }
        binding.tanBtn.setOnClickListener { appendToExpression("tan(") }

        // Fungsi arc-trigonometri
        binding.asinBtn.setOnClickListener { appendToExpression("arc_sin(") }
        binding.acosBtn.setOnClickListener { appendToExpression("arc_cos(") }
        binding.atanBtn.setOnClickListener { appendToExpression("arc_tan(") }

        // Tombol AC dan DEL
        binding.acBtn.setOnClickListener {
            binding.calTxt.text = "0"
            binding.resultTxt.text = "0"
        }

        binding.delBtn.setOnClickListener {
            val currentText = binding.calTxt.text.toString()
            if (currentText.isNotEmpty()) {
                binding.calTxt.text = currentText.dropLast(1)
            }
        }

        // Tombol sama dengan
        binding.equalBtn.setOnClickListener {
            val expression = binding.calTxt.text.toString()

                .replace(',', '.') // buat ubah koma ke titik


//                .replace("akar\\(".toRegex(), "sqrt(")
                .replace("akar\\^3\\(".toRegex(), "sqrt3(")
                .replace("ⁿ√x\\(".toRegex(), "nroot(")
                .replace("logaritma_natural\\(".toRegex(), "ln(")

                .replace(';', ',') // buat ⁿ√x

            try {
                val sind = object : Function("sin", 1) {
                    override fun apply(vararg args: Double): Double =
                        Math.sin(Math.toRadians(args[0]))
                }

                val cosd = object : Function("cos", 1) {
                    override fun apply(vararg args: Double): Double =
                        Math.cos(Math.toRadians(args[0]))
                }

                val tand = object : Function("tan", 1) {
                    override fun apply(vararg args: Double): Double =
                        Math.tan(Math.toRadians(args[0]))
                }

                val sqrt = object : Function("akar", 1) {
                    override fun apply(vararg args: Double): Double =
                        Math.sqrt(args[0])
                }

                val sqrtn = object : Function("nroot", 2) {
                    override fun apply(vararg args: Double): Double {
                        val n = args[0]
                        val x = args[1]
                        if (n == 0.0) throw IllegalArgumentException("Akar pangkat tidak boleh 0")
                        return Math.pow(x, 1.0 / n)
                    }
                }

                val sqrt3 = object : Function("sqrt3", 1) {
                    override fun apply(vararg args: Double): Double =
                        Math.cbrt(args[0])
                }

                val exp = object : Function("exp", 1) {
                    override fun apply(vararg args: Double): Double =
                        Math.exp(args[0])
                }

                val percent = object : Function("percent", 1) {
                    override fun apply(vararg args: Double): Double =
                        args[0] / 100
                }

                val asin = object : Function("arc_sin", 1) {
                    override fun apply(vararg args: Double): Double =
                        if (args[0] in -1.0..1.0) Math.toDegrees(Math.asin(args[0]))
                        else throw IllegalArgumentException("asin input out of range")
                }

                val acos = object : Function("arc_cos", 1) {
                    override fun apply(vararg args: Double): Double =
                        if (args[0] in -1.0..1.0) Math.toDegrees(Math.acos(args[0]))
                        else throw IllegalArgumentException("acos input out of range")
                }

                val atan = object : Function("arc_tan", 1) {
                    override fun apply(vararg args: Double): Double =
                        Math.toDegrees(Math.atan(args[0]))
                }

                val ln = object : Function("ln", 1) {
                    override fun apply(vararg args: Double): Double =
                        if (args[0] > 0) Math.log(args[0])
                        else throw IllegalArgumentException("ln hanya untuk nilai > 0")
                }

                val result = ExpressionBuilder(expression)
                    .functions(sind, cosd, tand, sqrt, sqrt3, sqrtn, exp, percent, asin, acos, atan, ln)
                    .variables("pi", "Ans")
                    .build()
                    .setVariable("pi", Math.PI)
                    .setVariable("Ans", lastAnswer)
                    .evaluate()

                val displayResult = when {
                    expression.contains("arc_sin") || expression.contains("arc_cos") || expression.contains("arc_tan") -> {
                        val deg = if (result % 1.0 == 0.0) result.toLong() else result
                        "$deg°"
                    }
                    result % 1.0 == 0.0 -> result.toLong().toString()
                    else -> result.toString()
                }

                binding.resultTxt.text = displayResult.replace('.', ',')
                lastAnswer = result

            } catch (e: Exception) {
                binding.resultTxt.text = "Error"
            }
        }
    }

}
