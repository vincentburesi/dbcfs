package fr.ct402.dbcfs.factorio.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Class for map-settings.json serialisation. See example file for more informations.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class MapSettings (
        var difficulty_settings: DifficultySettings = DifficultySettings(),
        var pollution: Pollution = Pollution(),
        var enemy_evolution: EnemyEvolution = EnemyEvolution(),
        var enemy_expansion: EnemyExpansion = EnemyExpansion(),
        var unit_group: UnitGroup = UnitGroup(),
        var steering: Steering = Steering(),
        var path_finder: PathFinder = PathFinder(),
        var max_failed_behavior_count: Int = 3
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    class DifficultySettings (
            var recipe_difficulty: Int = 0,
            var technology_difficulty: Int = 0,
            var technology_price_multiplier: Int = 1,
            var research_queue_setting: String = "after-victory"
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Pollution (
            var enabled: Boolean = true,
            var diffusion_ratio: Double = 0.02,
            var min_to_diffuse: Int = 15,
            var ageing: Int = 1,
            var expected_max_per_chunk: Int = 150,
            var min_to_show_per_chunk: Int = 50,
            var min_pollution_to_damage_trees: Int = 60,
            var pollution_with_max_forest_damage: Int = 150,
            var pollution_per_tree_damage: Int = 50,
            var pollution_restored_per_tree_damage: Int = 10,
            var max_pollution_to_restore_tree: Int = 20,
            var enemy_attack_pollution_consumption_modifier: Int = 1
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    class EnemyEvolution (
            var enabled: Boolean = true,
            var time_factor: Double = 0.000004,
            var destroy_factor: Double = 0.002,
            var pollution_factor: Double = 0.0000009
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    class EnemyExpansion (
            var enabled: Boolean = true,
            var min_base_spacing: Int = 3,
            var max_expansion_distance: Int = 7,
            var friendly_base_influence_radius: Int = 2,
            var enemy_building_influence_radius: Int = 2,
            var building_coefficient: Double = 0.1,
            var other_base_coefficient: Double = 2.0,
            var neighbouring_chunk_coefficient: Double = 0.5,
            var neighbouring_base_chunk_coefficient: Double = 0.4,
            var max_colliding_tiles_coefficient: Double = 0.9,
            var settler_group_min_size: Int = 5,
            var settler_group_max_size: Int = 20,
            var min_expansion_cooldown: Int = 14400,
            var max_expansion_cooldown: Int = 216000
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    class UnitGroup (
            var min_group_gathering_time: Int = 3600,
            var max_group_gathering_time: Int = 36000,
            var max_wait_time_for_late_members: Int = 7200,
            var max_group_radius: Double = 30.0,
            var min_group_radius: Double = 5.0,
            var max_member_speedup_when_behind: Double = 1.4,
            var max_member_slowdown_when_ahead: Double = 0.6,
            var max_group_slowdown_factor: Double = 0.3,
            var max_group_member_fallback_factor: Int = 3,
            var member_disown_distance: Int = 10,
            var tick_tolerance_when_member_arrives: Int = 60,
            var max_gathering_unit_groups: Int = 30,
            var max_unit_group_size: Int = 200
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Steering (
            var default: SpecificSteering = SpecificSteering(),
            var moving: SpecificSteering = SpecificSteering(3.0, 0.01, 3.0)
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    class SpecificSteering (
            var radius: Double = 1.2,
            var separation_force: Double = 0.005,
            var separation_factor: Double = 1.2,
            var force_unit_fuzzy_goto_behavior: Boolean = false
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    class PathFinder (
            var fwd2bwd_ratio: Int = 5,
            var goal_pressure_ratio: Int = 2,
            var max_steps_worked_per_tick: Int = 100,
            var max_work_done_per_tick: Int = 8000,
            var use_path_cache: Boolean = true,
            var short_cache_size: Int = 5,
            var long_cache_size: Int = 25,
            var short_cache_min_cacheable_distance: Int = 10,
            var short_cache_min_algo_steps_to_cache: Int = 50,
            var long_cache_min_cacheable_distance: Int = 30,
            var cache_max_connect_to_cache_steps_multiplier: Int = 100,
            var cache_accept_path_start_distance_ratio: Double = 0.2,
            var cache_accept_path_end_distance_ratio: Double = 0.15,
            var negative_cache_accept_path_start_distance_ratio: Double = 0.3,
            var negative_cache_accept_path_end_distance_ratio: Double = 0.3,
            var cache_path_start_distance_rating_multiplier: Int = 10,
            var cache_path_end_distance_rating_multiplier: Int = 20,
            var stale_enemy_with_same_destination_collision_penalty: Int = 30,
            var ignore_moving_enemy_collision_distance: Int = 5,
            var enemy_with_different_destination_collision_penalty: Int = 30,
            var general_entity_collision_penalty: Int = 10,
            var general_entity_subsequent_collision_penalty: Int = 3,
            var extended_collision_penalty: Int = 3,
            var max_clients_to_accept_any_new_request: Int = 10,
            var max_clients_to_accept_short_new_request: Int = 100,
            var direct_distance_to_consider_short_request: Int = 100,
            var short_request_max_steps: Int = 1000,
            var short_request_ratio: Double = 0.5,
            var min_steps_to_check_path_find_termination: Int = 2000,
            var start_to_goal_cost_multiplier_to_terminate_path_find: Double = 500.0,
            var overload_levels: Array<Int> = arrayOf(0, 100, 500),
            var overload_multipliers: Array<Int> = arrayOf(2, 3, 4)
    )
}