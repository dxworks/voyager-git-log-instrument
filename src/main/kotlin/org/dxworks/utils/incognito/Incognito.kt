package org.dxworks.utils.incognito

import java.io.File
import java.nio.charset.Charset
import java.nio.charset.MalformedInputException
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import kotlin.system.exitProcess

val authorRegex = Regex("(author:)(.*)")
val emailRegex = Regex("(email:)(.*)(@.*)")

const val consonants = "bcdfghjklmnpqrstvwxyzBCDFGHJKLMNPQRSTVWXYZ"
const val vowels = "aeiouAEIOU"

fun encrypt(car: Char): Char {

    if (car == 'u') return 'a'
    if (car == 'U') return 'A'
    if (car == 'z') return 'b'
    if (car == 'Z') return 'B'

    var indexOfCar: Int = consonants.indexOf(car)

    if (indexOfCar == consonants.length - 1) return consonants[0]

    if (indexOfCar >= 0) return consonants[indexOfCar + 1]

    indexOfCar = vowels.indexOf(car)
    if (indexOfCar == vowels.length - 1) return vowels[0]

    return if (indexOfCar >= 0) vowels[indexOfCar + 1]
    else car
}

private fun encryptLine(name: String): String =
    name.toCharArray().map { encrypt(it) }.toString()


private fun processLogfile(logFile: File, charset: Charset = Charsets.UTF_8) {
    val incognitoFile = logFile.resolveSibling("${logFile.nameWithoutExtension}-incognito.git")

    try {
        logFile.useLines(charset) { lines ->
            incognitoFile.bufferedWriter().use { writer ->
                lines.map {
                    authorRegex.find(it)?.let { match ->
                        match.groupValues[1] + encryptLine(match.groupValues[2])
                    } ?: emailRegex.find(it)?.let { match ->
                        match.groupValues[1] + encryptLine(match.groupValues[2]) + match.groupValues[3]
                    } ?: it
                }.forEach { writer.write(it) }
            }
        }
    } catch (e: MalformedInputException) {
        if (charset == Charsets.UTF_8)
            processLogfile(logFile, StandardCharsets.ISO_8859_1)
    }

    incognitoFile.copyTo(logFile)
    incognitoFile.delete()
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        System.err.println("No input folder provided")
        System.err.println("You must put a Git log file as parameter!")
        exitProcess(-1)
    }
    val fileToProcess = args[0]
    processLogfile(Paths.get(fileToProcess).toFile())
}

