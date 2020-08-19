package fr.ct402.dbcfs.factorio.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Class for server-settings.json serialisation. See example file for more informations.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class ServerSettings (
    var name: String,
    var description: String = "",
    var tags: Array<String> = arrayOf(),
    var max_players: Int = 0,
    var visibility: Visibility = Visibility(true, true),
    var username: String = "",
    var password: String = "",
    var token: String = "",
    var game_password: String = "",
    var require_user_verification: Boolean = true,
    var max_upload_in_kilobytes_per_second: Int = 0,
    var max_upload_slots: Int = 5,
    var minimum_latency_in_ticks: Int = 0,
    var ignore_player_limit_for_returning_players: Boolean = false,
    var allow_commands: String = "admins-only",
    var autosave_interval: Int = 10,
    var autosave_slots: Int = 5,
    var afk_autokick_interval: Int = 0,
    var auto_pause: Boolean = true,
    var only_admins_can_pause_the_game: Boolean = true,
    var autosave_only_on_server: Boolean = true,
    var non_blocking_saving: Boolean = false,
    var minimum_segment_size: Int = 25,
    var minimum_segment_size_peer_count: Int = 20,
    var maximum_segment_size: Int = 100,
    var maximum_segment_size_peer_count: Int = 10
    ) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    class Visibility (
            var public: Boolean,
            var lan: Boolean
    )
}