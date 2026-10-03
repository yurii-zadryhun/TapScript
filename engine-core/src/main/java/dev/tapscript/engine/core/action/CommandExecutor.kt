package dev.tapscript.engine.core.action

import dev.tapscript.engine.api.command.AutomationCommand
import dev.tapscript.engine.api.model.AutomationProfile
import dev.tapscript.engine.api.model.PixelSize
import dev.tapscript.engine.api.ports.AutomationLogger
import dev.tapscript.engine.api.ports.GestureDispatcher
import kotlinx.coroutines.delay

class CommandExecutor(
    private val gestureDispatcher: GestureDispatcher,
    private val actionResolver: ActionResolver,
    private val logger: AutomationLogger,
) {
    suspend fun execute(
        commands: List<AutomationCommand>,
        profile: AutomationProfile,
        screenSize: PixelSize,
        onCommand: (String) -> Unit = {},
    ) {
        for (command in commands) {
            when (command) {
                is AutomationCommand.Tap -> {
                    val description = "tap:${command.targetId}"
                    onCommand(description)
                    logger.info("Executing $description")
                    actionResolver.resolveTap(profile, command.targetId, screenSize)
                        .fold(
                            onSuccess = { point -> gestureDispatcher.tap(point).getOrThrow() },
                            onFailure = { throw it },
                        )
                }

                is AutomationCommand.Swipe -> {
                    val description = "swipe:${command.targetId}"
                    onCommand(description)
                    logger.info("Executing $description")
                    actionResolver.resolveSwipe(profile, command.targetId, screenSize)
                        .fold(
                            onSuccess = { swipe ->
                                gestureDispatcher.swipe(swipe.start, swipe.end, swipe.durationMs).getOrThrow()
                            },
                            onFailure = { throw it },
                        )
                }

                is AutomationCommand.Wait -> {
                    val safeDuration = command.durationMs.coerceIn(0, 60_000)
                    val description = "wait:${safeDuration}ms"
                    onCommand(description)
                    logger.debug("Executing $description")
                    delay(safeDuration)
                }

                is AutomationCommand.Log -> {
                    onCommand("log")
                    logger.info("[script] ${command.message}")
                }
            }
        }
    }
}
