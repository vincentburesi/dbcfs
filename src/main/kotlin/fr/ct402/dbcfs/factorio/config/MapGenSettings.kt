package fr.ct402.dbcfs.factorio.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Class for map-gen-settings.json serialisation. See example file for more informations.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class MapGenSettings (
var terrain_segmentation: Int = 1,
var water: Int = 1,
var width: Int = 0,
var height: Int = 0,
var starting_area: Int = 1,
var peaceful_mode: Boolean = false,
var autoplace_controls: AutoplaceControls = AutoplaceControls(),
var cliff_settings: CliffSettings = CliffSettings(),
var property_expression_names: PropertyExpressionNames = PropertyExpressionNames(),
var starting_points: Array<Coord> = arrayOf(Coord()),
var seed: Int? = null
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    class AutoplaceControls(
            var coal: AutoplaceSetting = AutoplaceSetting(),
            var stone: AutoplaceSetting = AutoplaceSetting(),
            @JsonProperty("copper-ore")
            var copperOre: AutoplaceSetting = AutoplaceSetting(),
            @JsonProperty("iron-ore")
            var ironOre: AutoplaceSetting = AutoplaceSetting(),
            @JsonProperty("uranium-ore")
            var uraniumOre: AutoplaceSetting = AutoplaceSetting(),
            @JsonProperty("crude-oil")
            var crudeOil: AutoplaceSetting = AutoplaceSetting(),
            var trees: AutoplaceSetting = AutoplaceSetting(),
            @JsonProperty("enemy-base")
            var enemyBase: AutoplaceSetting = AutoplaceSetting()
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    class AutoplaceSetting(
            var frequency: Int = 1,
            var size: Int = 1,
            var richness: Int = 1
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    class CliffSettings(
            var name: String = "cliff",
            var cliff_elevation_0: Int = 10,
            var cliff_elevation_interval: Int = 10,
            var richness: Int = 1
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    class PropertyExpressionNames(
            var elevation: String = "",
            @JsonProperty("control-setting:aux:bias")
            var controlSettingAuxBias: String = "0.300000",
            @JsonProperty("control-setting:aux:frequency:multiplier")
            var controlSettingAuxFrequencyMultiplier: String = "1.333333",
            @JsonProperty("control-setting:moisture:bias")
            var controlSettingMoistureBias: String = "0.100000",
            @JsonProperty("control-setting:moisture:frequency:multiplier")
            var controlSettingMoistureFrequencyMultiplier: String = "0.500000"
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Coord(
            var x: Int = 1000,
            var y: Int = 2000
    )
}
