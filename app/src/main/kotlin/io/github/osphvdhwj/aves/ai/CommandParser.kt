package io.github.osphvdhwj.aves.ai

data class ParsedCommand(
    val raw: String,
    val prefix: String,
    val verb: String,
    val freeText: String,
)

object CommandParser {
    const val DEFAULT_VERB = "find"

    fun parse(input: String): ParsedCommand {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return ParsedCommand("", "", DEFAULT_VERB, "")
        }

        if (trimmed.startsWith("/")) {
            return parsePrefixed(trimmed, "/", trimmed.substring(1))
        }
        if (trimmed.startsWith("@")) {
            return parsePrefixed(trimmed, "@", trimmed.substring(1))
        }
        // plain text: whole input is the free text, default verb
        return ParsedCommand(trimmed, "", DEFAULT_VERB, trimmed)
    }

    private fun parsePrefixed(raw: String, prefix: String, body: String): ParsedCommand {
        val sp = body.indexOfFirst { it.isWhitespace() }
        if (sp < 0) {
            val v = body.ifEmpty { DEFAULT_VERB }
            return ParsedCommand(raw, prefix, v, "")
        }
        val verb = body.substring(0, sp)
        val freeText = body.substring(sp + 1).trim()
        return ParsedCommand(raw, prefix, verb.ifEmpty { DEFAULT_VERB }, freeText)
    }
}
