package dev.nheox.mintal.data.species

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.nheox.mintal.core.model.GrowthHabit
import dev.nheox.mintal.core.model.LeafHue
import dev.nheox.mintal.core.model.LeafMargin
import dev.nheox.mintal.core.model.LeafShape
import dev.nheox.mintal.core.model.Species
import dev.nheox.mintal.core.model.SpeciesArt
import dev.nheox.mintal.core.model.StemHue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The twelve mints of spec §6.1, read from `assets/species.json`. Shipping the catalogue
 * as an asset rather than table rows is what lets it grow in a later version without a
 * Room migration (spec §7).
 */
@Singleton
class SpeciesCatalog @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    @Volatile
    private var cached: List<Species>? = null

    suspend fun all(): List<Species> {
        cached?.let { return it }
        val parsed = withContext(Dispatchers.IO) { parse(readAsset()) }
        cached = parsed
        return parsed
    }

    suspend fun byId(id: String): Species? = all().firstOrNull { it.id == id }

    /** The one mint available from the start (spec §6.1). */
    suspend fun starter(): Species = all().minByOrNull { it.unlockMinutes } ?: error("empty catalogue")

    private fun readAsset(): String =
        context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }

    private fun parse(raw: String): List<Species> {
        val root = JSONObject(raw)
        val array = root.getJSONArray("species")
        val result = ArrayList<Species>(array.length())
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val art = item.getJSONObject("art")
            result += Species(
                id = item.getString("id"),
                binomial = item.getString("binomial"),
                commonName = item.getString("commonName"),
                unlockMinutes = item.getInt("unlockMinutes"),
                hybrid = item.optBoolean("hybrid", false),
                note = item.getString("note"),
                art = SpeciesArt(
                    leafShape = art.getString("leafShape").toEnum(LeafShape.OVATE),
                    margin = art.getString("margin").toEnum(LeafMargin.SERRATE),
                    habit = art.getString("habit").toEnum(GrowthHabit.UPRIGHT),
                    leafScale = art.optDouble("leafScale", 1.0).toFloat(),
                    leafHue = art.getString("leafHue").toEnum(LeafHue.LEAF_MID),
                    stemHue = art.getString("stemHue").toEnum(StemHue.STEM_GREEN),
                ),
            )
        }
        return result.sortedBy { it.unlockMinutes }
    }

    private inline fun <reified T : Enum<T>> String.toEnum(fallback: T): T =
        runCatching { enumValueOf<T>(this) }.getOrDefault(fallback)

    private companion object {
        const val ASSET_NAME = "species.json"
    }
}
