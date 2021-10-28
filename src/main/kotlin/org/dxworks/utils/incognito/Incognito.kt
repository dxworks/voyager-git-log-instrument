package org.dxworks.utils.incognito

import java.io.File
import java.nio.charset.Charset
import java.nio.charset.MalformedInputException
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import kotlin.system.exitProcess

val authorRegex = Regex("(author:)(.*)")
val emailRegex = Regex("(email:)(.*)(@.*)")
const val CHAR_MAP_ENV_VARIABLE = "INCOGNITO_CHARMAP_FILE"
val charmapFilePath: String? = System.getenv(CHAR_MAP_ENV_VARIABLE)
var charTransformer: CharTransformer =
    if (charmapFilePath != null && File(charmapFilePath).exists())
        CharTransformer(charmapFilePath)
    else CharTransformer()

private fun encryptString(name: String): String =
    name.toCharArray().map { charTransformer.mapChar(it) }.joinToString("")


private fun processLogfile(logFile: File, charset: Charset = Charsets.UTF_8) {
    val incognitoFile = logFile.resolveSibling("${logFile.nameWithoutExtension}-incognito.git")

    try {
        logFile.useLines(charset) { lines ->
            incognitoFile.bufferedWriter().use { writer ->
                lines.map {
                    authorRegex.find(it)?.let { match ->
                        match.groupValues[1] + encryptString(match.groupValues[2])
                    } ?: emailRegex.find(it)?.let { match ->
                        match.groupValues[1] + encryptString(match.groupValues[2]) + match.groupValues[3]
                    } ?: it
                }.forEach { writer.write("$it\n") }
            }
        }
    } catch (e: MalformedInputException) {
        if (charset == Charsets.UTF_8)
            processLogfile(logFile, StandardCharsets.ISO_8859_1)
    }

    incognitoFile.copyTo(logFile, true)
    incognitoFile.delete()
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        System.err.println("No log file provided")
        System.err.println("You must provide a Git log file as parameter!")
        exitProcess(-1)
    }
    val fileToProcess = args[0]
    processLogfile(Paths.get(fileToProcess).toFile())
}

