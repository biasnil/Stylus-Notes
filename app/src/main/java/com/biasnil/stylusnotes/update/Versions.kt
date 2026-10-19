package com.biasnil.stylusnotes.update

/**
 * Compares version names like "1.0", "v1.2.3" or "1.3.0-beta". Pure Kotlin so it runs in JVM tests.
 * Numbers are compared part by part ("1.10" is newer than "1.9"); missing parts count as 0.
 * A version with a suffix ("-beta", "-rc1") is older than the same version without one.
 */
object Versions {

    data class Parsed(val parts: List<Int>, val suffix: String)

    fun parse(name: String): Parsed {
        val t = name.trim().removePrefix("v").removePrefix("V")
        val numeric = t.takeWhile { it.isDigit() || it == '.' }
        val parts = numeric.split('.').filter { it.isNotEmpty() }.map { it.toIntOrNull() ?: 0 }
        val suffix = t.drop(numeric.length).trimStart('-', '_', ' ', '+')
        return Parsed(parts.ifEmpty { listOf(0) }, suffix)
    }

    /** Negative if [a] is older than [b], 0 if equal, positive if newer. */
    fun compare(a: String, b: String): Int {
        val pa = parse(a)
        val pb = parse(b)
        val n = maxOf(pa.parts.size, pb.parts.size)
        for (i in 0 until n) {
            val x = pa.parts.getOrElse(i) { 0 }
            val y = pb.parts.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return when {
            pa.suffix == pb.suffix -> 0
            pa.suffix.isEmpty() -> 1
            pb.suffix.isEmpty() -> -1
            else -> pa.suffix.compareTo(pb.suffix, ignoreCase = true)
        }
    }

    fun isNewer(latest: String, current: String): Boolean = compare(latest, current) > 0
}
