package example_module

import me.groovymc.api.ScriptAPI
import groovy.transform.BaseScript
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

@BaseScript ScriptAPI base

// --- EVENT HANDLING SYSTEM ---
// The 'onEvent' method is universal and supports ALL Bukkit events.
// You can listen to any event available in the 'org.bukkit.event' package.
// Examples include:
// - EntityDamageByEntityEvent (Combat)
// - AsyncPlayerChatEvent (Chat)
// - InventoryClickEvent (GUI interaction)
// - VehicleMoveEvent, WeatherChangeEvent, and many more.

// --- EVENT: PLAYER JOIN ---
onEvent(PlayerJoinEvent) { e ->
    // Create a new Potion Effect.
    // Type: DARKNESS, Duration: 60 ticks (3 seconds), Amplifier: 5 (Strength level 6)
    def effect = new PotionEffect(PotionEffectType.DARKNESS, 60, 5)
    def p = e.player

    // Apply the potion effect to the player.
    p.addPotionEffect(effect)

    // Play a sound effect at the player's location.
    p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1)

    // Spawn visual particles at the player's location.
    p.spawnParticle(Particle.SMOKE, p.location, 0, 0, 0, 0)

    message(p, "Welcome back ${p.name}")
}

// --- EVENT: BLOCK BREAK ---
// Listening to another standard Bukkit event.
onEvent(BlockBreakEvent) { e ->
    // Check if the block being broken is an ore.
    if (e.block.type.toString().contains("ORE")) {
        def p = e.player

        // Play a specific 'ding' sound.
        p.playSound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1)

        message(p, "You are mining.")
    }
}