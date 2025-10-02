package ru.queuejw.space.desktop

import ru.queuejw.space.game.*
import kotlin.random.Random

class DesktopNamer : INamer {
    private val planetDescriptors = Bag(arrayOf(
        "earthy", "swamp", "frozen", "grassy", "arid", "crowded", "ancient", "lively", "homey", "modern",
        "boring", "compact", "expensive", "polluted", "rusty", "sandy", "undulating", "verdant", "tessellated",
        "hollow", "scalding", "hemispherical", "oblong", "oblate", "vacuum", "high-pressure", "low-pressure",
        "plastic", "metallic", "burned-out", "bucolic"
    ))

    private val lifeDescriptors = Bag(arrayOf(
        "aggressive", "passive-aggressive", "shy", "timid", "nasty", "brutish", "short", "absent", "teen-aged",
        "confused", "transparent", "cubic", "quadratic", "higher-order", "huge", "tall", "wary", "loud",
        "yodeling", "purring", "slender", "cats", "adorable", "eclectic", "electric", "microscopic",
        "trunkless", "myriad", "cantankerous", "gargantuan", "contagious", "fungal", "cattywampus",
        "spatchcocked", "rotisserie", "farm-to-table", "organic", "synthetic", "unfocused", "focused",
        "capitalist", "communal", "bossy", "malicious", "compliant", "psychic", "oblivious", "passive", "bonsai"
    ))

    private val anyDescriptors = Bag(arrayOf(
        "silly", "dangerous", "vast", "invisible", "superfluous", "superconducting", "superior", "alien",
        "phantom", "friendly", "peaceful", "lonely", "uncomfortable", "charming", "fractal", "imaginary",
        "forgotten", "tardy", "gassy", "fungible", "bespoke", "artisanal", "exceptional", "puffy", "rusty",
        "fresh", "crusty", "glossy", "lovely", "processed", "macabre", "reticulated", "shocking", "void",
        "undefined", "gothic", "beige", "mid", "milquetoast", "melancholy", "unnerving", "cheery", "vibrant",
        "heliotrope", "psychedelic", "nondescript", "indescribable", "tubular", "toroidal", "voxellated",
        "low-poly", "low-carb", "100% cotton", "synthetic", "boot-cut", "bell-bottom", "bumpy", "fluffy",
        "sous-vide", "tepid", "upcycled", "bedazzled", "ancient", "inexplicable", "sparkling", "still",
        "lemon-scented", "eccentric", "tilted", "pungent", "pine-scented", "corduroy", "overengineered",
        "bioengineered", "impossible"
    ))

    private val atmoDescriptors = Bag(arrayOf(
        "toxic", "breathable", "radioactive", "clear", "calm", "peaceful", "vacuum", "stormy", "freezing",
        "burning", "humid", "tropical", "cloudy", "obscured", "damp", "dank", "clammy", "frozen",
        "contaminated", "temperate", "moist", "minty", "relaxed", "skunky", "breezy", "soup"
    ))

    private val planetTypes = Bag(arrayOf(
        "planet", "planetoid", "moon", "moonlet", "centaur", "asteroid", "space garbage", "detritus",
        "satellite", "core", "giant", "body", "slab", "rock", "husk", "planemo", "object",
        "planetesimal", "exoplanet", "ploonet"
    ))

    private val constellations = Bag(arrayOf(
        "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo", "Libra", "Scorpio", "Sagittarius",
        "Capricorn", "Aquarius", "Pisces", "Andromeda", "Cygnus", "Draco", "Alcor", "Calamari",
        "Cuckoo", "Neko", "Monoceros", "Norma", "Abnorma", "Morel", "Redlands", "Cupcake", "Donut",
        "Eclair", "Froyo", "Gingerbread", "Honeycomb", "Icecreamsandwich", "Jellybean", "Kitkat",
        "Lollipop", "Marshmallow", "Nougat", "Oreo", "Pie", "Quincetart", "Redvelvetcake", "Snowcone",
        "Tiramisu", "Upsidedowncake", "Vanillaicecream", "Android", "Binder", "Campanile", "Dread"
    ))

    private val constellationsRare = Bag(arrayOf(
        "Jandycane", "Zombiegingerbread", "Astro", "Bender", "Flan", "Untitled-1", "Expedit",
        "Petit Four", "Worcester", "Xylophone", "Yellowpeep", "Zebraball", "Hutton", "Klang",
        "Frogblast", "Exo", "Keylimepie", "Nat", "Nrp"
    ))

    private val suffixes = Bag(arrayOf(
        "Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta", "Theta", "Iota", "Kappa",
        "Lambda", "Mu", "Nu", "Xi", "Omicron", "Pi", "Rho", "Sigma", "Tau", "Upsilon", "Phi",
        "Chi", "Psi", "Omega", "Prime", "Secundo", "Major", "Minor", "Diminished", "Augmented",
        "Ultima", "Penultima", "Mid", "Proxima", "Novis", "Plus"
    ))

    private val suffixesRare = Bag(arrayOf(
        "Serif", "Sans", "Oblique", "Grotesque", "Handtooled", "III \"Trey\"", "Alfredo", "2.0",
        "(Final)", "(Final (Final))", "(Draft)", "Con Carne"
    ))

    private val activities = Bag(arrayOf(
        "refueling", "sightseeing", "vacationing", "luncheoning", "recharging", "taking up space",
        "reticulating space splines", "using facilities", "spelunking", "repairing", "herding {fauna}",
        "taming {fauna}", "breeding {fauna}", "singing lullabies to {fauna}", "singing lullabies to {flora}",
        "singing lullabies to the {planet}", "gardening {flora}", "collecting {flora}", "surveying the {planet}",
        "mapping the {planet}", "breathing {atmo}", "reprocessing {atmo}", "bottling {atmo}"
    ))

    private val floraGenericPlurals = Bag(arrayOf(
        "flora", "plants", "flowers", "trees", "mosses", "specimens", "life", "cells"
    ))

    private val faunaGenericPlurals = Bag(arrayOf(
        "fauna", "animals", "locals", "creatures", "critters", "wildlife", "specimens", "life", "cells"
    ))

    private val atmoGenericPlurals = Bag(arrayOf(
        "air", "atmosphere", "clouds", "atmo", "gases"
    ))

    private val planetTable = RandomTable(0.75f to planetDescriptors, 0.25f to anyDescriptors)
    private val lifeTable = RandomTable(0.75f to lifeDescriptors, 0.25f to anyDescriptors)
    private val constellationsTable = RandomTable(RARE_PROB to constellationsRare, 1f - RARE_PROB to constellations)
    private val suffixesTable = RandomTable(RARE_PROB to suffixesRare, 1f - RARE_PROB to suffixes)
    private val atmoTable = RandomTable(0.75f to atmoDescriptors, 0.25f to anyDescriptors)

    private val delimiterTable = RandomTable(
        15f to " ", 3f to "-", 1f to "_", 1f to "/", 1f to ".", 1f to "*", 1f to "^", 1f to "#",
        0.1f to "(^*!%@##!!"
    )

    override fun describePlanet(rng: Random): String =
        planetTable.roll(rng).pull(rng) + " " + planetTypes.pull(rng)

    override fun describeLife(rng: Random): String = lifeTable.roll(rng).pull(rng)

    override fun nameSystem(rng: Random): String {
        val parts = StringBuilder()
        parts.append(constellationsTable.roll(rng).pull(rng))
        if (rng.nextFloat() <= SUFFIX_PROB) {
            parts.append(delimiterTable.roll(rng))
            parts.append(suffixesTable.roll(rng).pull(rng))
            if (rng.nextFloat() <= RARE_PROB) parts.append(' ').append(suffixesRare.pull(rng))
        }
        if (rng.nextFloat() <= LETTER_PROB) {
            parts.append(delimiterTable.roll(rng))
            parts.append('A' + rng.nextInt(0, 26))
            if (rng.nextFloat() <= RARE_PROB) parts.append(delimiterTable.roll(rng))
        }
        if (rng.nextFloat() <= NUMBER_PROB) {
            parts.append(delimiterTable.roll(rng))
            parts.append(rng.nextInt(2, 5039))
        }
        return parts.toString()
    }

    override fun describeAtmo(rng: Random): String = atmoTable.roll(rng).pull(rng)

    override fun describeActivity(rng: Random, target: PlanetInfo?): String =
        activities.pull(rng).replace(Regex("""\{(flora|fauna|planet|atmo)\}""")) {
            when (it.groupValues[1]) {
                "flora" -> (target?.flora ?: "SOME") + " " + floraPlural(rng)
                "fauna" -> (target?.fauna ?: "SOME") + " " + faunaPlural(rng)
                "atmo" -> (target?.atmosphere ?: "SOME") + " " + atmoPlural(rng)
                "planet" -> (target?.description ?: "SOME BODY")
                else -> "unknown template tag: ${it.groupValues[0]}"
            }
        }

    private fun floraPlural(rng: Random) = floraGenericPlurals.pull(rng)
    private fun faunaPlural(rng: Random) = faunaGenericPlurals.pull(rng)
    private fun atmoPlural(rng: Random) = atmoGenericPlurals.pull(rng)
}

