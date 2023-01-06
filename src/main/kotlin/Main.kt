/**
 * @author Kacper Piasta, 249105
 *
 * difficulty level: 3
 */

@file:Suppress("EnumEntryName")

import MovementDirection.*
import State.*
import Symbol.*
import TransitionTablePrinter.print
import java.io.Closeable
import java.util.Scanner

typealias Entry = Triple<Symbol, State, MovementDirection>
typealias Row = Map.Entry<Pair<State, Symbol>, Entry>
typealias Table = Map<Pair<State, Symbol>, Entry>

/**
 * Enumeration class representation of head's direction of movement
 */
enum class MovementDirection(private val value: String) {
    L("L"),
    R("R"),
    N("-");

    override fun toString() = value
}

/**
 * Exception thrown if read character is not accepted by TM
 *
 * (in other words: if alphabet doesn't contain symbol)
 */
private class SymbolNotAcceptedException(value: Char) : Exception("TM doesn't accept symbol: $value")

/**
 * Enumeration class representation of symbols
 *
 * Used for type-safety to prevent read of non-accepted character
 */
enum class Symbol(private val value: String) {
    `0`("0"), `1`("1"), theta("Θ"), blank("-");

    companion object {
        /**
         * Gets enumeration object by character
         */
        @JvmStatic
        fun of(value: Char): Symbol {
            return when (value) {
                '0' -> `0`
                '1' -> `1`
                '#' -> theta
                else -> throw SymbolNotAcceptedException(value)
            }
        }
    }

    override fun toString() = value
}

/**
 * Enumeration class representation of states
 */
enum class State(private val value: String) {
    q0("q0"), q1("q1"), q2("q2"), idle("-");

    override fun toString() = value
}

/**
 * Utility class to print transition table
 */
object TransitionTablePrinter {
    private const val HORIZONTAL_BORDER_KNOT = "+"
    private const val HORIZONTAL_BORDER_PATTERN = "-"
    private const val VERTICAL_BORDER_PATTERN = "|"

    /**
     * Pretty-prints transition table of an automaton (2D array)
     */
    fun Array<Array<String>>.print() = takeIf { isNotEmpty() }?.let {
        val numberOfColumns = maxOfOrNull(Array<String>::size) ?: 0
        val maxColumnWidth = flatten().maxOfOrNull(String::length) ?: 0
        val horizontalBorder = createHorizontalBorder(numberOfColumns, maxColumnWidth)
        println(horizontalBorder)
        forEach { row ->
            println(row.asString(maxColumnWidth))
            println(horizontalBorder)
        }
    } ?: Unit

    /**
     * Converts row to pretty-printed string
     */
    private fun Array<String>.asString(width: Int) = VERTICAL_BORDER_PATTERN.plus(joinToString("") {
        padCell(it, width)
    })

    /**
     * Creates horizontal border for a row
     */
    private fun createHorizontalBorder(numberOfColumns: Int, width: Int) =
        HORIZONTAL_BORDER_KNOT + HORIZONTAL_BORDER_PATTERN
            .repeat(width)
            .plus(HORIZONTAL_BORDER_KNOT)
            .repeat(numberOfColumns)

    /**
     * Pads cell left to particular length
     */
    private fun padCell(text: String, length: Int) = text.padStart(length).plus(VERTICAL_BORDER_PATTERN)
}

/**
 * Actual implementation of TM
 */
class TuringMachine : Closeable {
    /**
     * Companion object containing static fields
     */
    private companion object {
        /**
         * Converts transition table to 2D array
         */
        @JvmStatic
        fun Table.matrix(): Array<Array<String>> {
            fun Entry.asString() = toList().joinToString(", ")

            fun List<Row>.mapRows(): Array<String> {
                val header = listOf(first().key.first.toString())
                val values = map(Row::value).map(Entry::asString)
                return header.plus(values).toTypedArray()
            }

            val header = (listOf("δ") + keys
                .map(Pair<State, Symbol>::second)
                .map(Symbol::toString)
                .distinct())
                .toTypedArray()
            val rows = entries
                .chunked(3)
                .map(List<Row>::mapRows)
                .toTypedArray()
            return arrayOf(header).plus(rows)
        }

        /**
         * Transition table representation as a key-value map
         *
         * Key – pair of unique states with corresponding transition symbol
         *
         * Value – post-transition triple of symbol, next state and head's direction of movement
         */
        // @formatter:off
        @JvmField
        val transitionTable = mapOf(
            Pair(q0, `0`) to Triple(`0`, q1, L), Pair(q0, `1`) to Triple(`1`, q1, L), Pair(q0, theta) to Triple(blank, idle, N),
            Pair(q1, `0`) to Triple(blank, idle, N), Pair(q1, `1`) to Triple(blank, idle, N), Pair(q1, theta) to Triple(`1`, q2, L),
            Pair(q2, `0`) to Triple(blank, idle, N), Pair(q2, `1`) to Triple(blank, idle, N), Pair(q2, theta) to Triple(blank, idle, N)
        )
        // @formatter:on

        /**
         * Set of accepted states
         */
        val acceptingStates = setOf(q2)
    }

    /**
     * Representation of values written on tape
     */
    private var _tapeValues = mutableListOf<Symbol>()

    /**
     * Representation of traversed states
     */
    private var _states = mutableListOf(q0)

    /**
     * Prints initial TM state
     */
    init {
        println("Transition table:")
        transitionTable.matrix().print()
        printCurrentState()
    }

    /**
     * Consumes symbols and performs TM transition
     */
    fun consume(symbol: Symbol) {
        /**
         * Get next state from transition table
         */
        fun List<State>.nextState() = transitionTable[Pair(last(), symbol)]

        println("Reading symbol: $symbol")

        /** perform transition, print current value on tape, state and head direction of movement **/
        _states.nextState()?.let {
            _tapeValues += it.first
            _states += it.second
            printCurrentState()
            println("Value written on tape: ${it.first}")
            println("Head direction of movement: ${it.third}")
        }
    }

    /**
     * Prints final results on completion
     */
    override fun close() {
        printStateChangePath()
        printCurrentState(true)
        if (_states.last().isAccepting()) printFinalValue()
    }

    /**
     * Prints current TM state
     */
    private fun printCurrentState(onClose: Boolean = false) {
        val currentState = _states.last()
        val statusLabel = if (onClose) "Final" else "Current"
        val resultLabel = if (onClose and currentState.isAccepting()) "(accepting)" else ""
        println("$statusLabel TM state: $currentState $resultLabel")
    }

    /**
     * Prints state change path
     */
    private fun printStateChangePath() = println("State change path: ${_states.joinToString("→")}")

    /**
     * Prints final TM value
     */
    private fun printFinalValue() = println("Final value: ${_tapeValues.reversed().joinToString("")}")

    /**
     * Check if TM is in accepting state
     */
    private fun State.isAccepting() = acceptingStates.contains(this)
}

fun main() = TuringMachine().use {
    with(it) {
        Scanner(System.`in`).use { scanner ->
            /** infinite loop **/
            while (true) {
                print("Type value terminated by # character (theta equivalent): ")
                try {
                    /** get user input and consume symbol by TM **/
                    scanner.nextLine()
                        .toCharArray()
                        .filterNot(Char::isWhitespace)
                        .ifEmpty { null }
                        ?.onEach { char -> consume(Symbol.of(char)) }
                        ?: continue
                    break
                } catch (ex: SymbolNotAcceptedException) {
                    /** print message if user tries to input symbol not defined in alphabet, TM doesn't accept it **/
                    println(ex.message)
                }
            }
        }
    }
}
