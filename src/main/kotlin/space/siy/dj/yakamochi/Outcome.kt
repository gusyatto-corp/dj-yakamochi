package space.siy.dj.yakamochi

import java.io.PrintStream
import java.io.PrintWriter
import java.io.StringWriter

/**
 * @author SIY1121
 */

sealed class Outcome<out T, out U> {
    data class Success<out T>(val result: T): Outcome<T, Nothing>()
    data class Error<out U>(val reason: U, val cause: Throwable?): Outcome<Nothing, U>()
}

fun Throwable.stackTraceString(): String {
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    printStackTrace(pw)
    return sw.toString()
}