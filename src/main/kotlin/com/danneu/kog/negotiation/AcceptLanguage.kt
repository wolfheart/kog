package com.danneu.kog.negotiation

import kotlin.comparisons.compareBy

// A **goofy** wrapper around accept-language prefix/suffix pairs like en, en-GB, en-US.
//
// The implementation got a bit gnarly since I was reverse-engineering how it should work from
// other server test cases and letting TDD drive my impl like a black box in some parts.
//
// https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
//
// TODO: Incomplete, experimental.
// TODO: Finish language code list http://www.lingoes.net/en/translator/langcode.htm
// TODO: Maybe should keep it open-ended like any language being able to use any locale.
// TODO: Maybe should just simplify it into Pair("en", null), Pair("en", "US") style stuff.


// This class is just a pair of language and its q-value.
//
// TODO: Maybe AcceptLanguage should be Pair<Lang, QValue>. Lang vs AcceptLanguage is confusing as top-level classes.
class AcceptLanguage(val lang: Lang, val q: Double = 1.0) {
    override fun toString() = "AcceptLanguage[lang=$lang, q=$q]"
    override fun equals(other: Any?) = other is AcceptLanguage && this.lang == other.lang && this.q == other.q

    // Generated by IDEA
    override fun hashCode() = 31 * lang.hashCode() + q.hashCode()

    companion object {
        val regex = Regex("""^\s*([^\s\-;]+)(?:-([^\s;]+))?\s*(?:;(.*))?$""")

        // TODO: Test malformed header

        fun acceptable(clientLang: Lang, availableLang: Lang, excludedLangs: Set<Lang>): Boolean {
            if (availableLang in excludedLangs) return false
            // clientLang is * so everything is acceptable
            if (clientLang == Lang.Wildcard) return true
            // if clientLang is "en" and "en-US" is available, it is acceptable
            if (clientLang.locale == Locale.Wildcard && clientLang.name == availableLang.name) return true
            // if clientLang is "en-US" and "en-US" is available
            if (clientLang.name == availableLang.name) return true

            return false
        }

        /** Parses a single segment pair
         */
        fun parse(string: String): AcceptLanguage? {
            val parts = regex.find(string)?.groupValues?.drop(1) ?: return null
            // FIXME: temp hack since the refactor: rejoining parts with hyphen because that's what regex gives me
            val lang = Lang.fromString("${parts[0]}-${parts[1]}") ?: return null
            val q = QValue.parse(parts[2]) ?: 1.0
            return AcceptLanguage(lang, q)
        }


        /** Parses comma-delimited string of types
         */
        fun parseHeader(header: String): List<AcceptLanguage> {
            return header.split(",").map(String::trim).mapNotNull(this::parse)
        }

        fun prioritize(xs: List<AcceptLanguage>): List<AcceptLanguage> {
            return xs.sortedWith(compareBy(
                { -it.q },
                // Wildcard comes last
                { when (it.lang) {
                    Lang.Wildcard ->
                        1
                    else ->
                        0
                }}
            ))

        }
    }
}


