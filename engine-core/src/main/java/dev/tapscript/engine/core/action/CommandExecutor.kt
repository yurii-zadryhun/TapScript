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
                    onCommand("tap:${command.targetId}")
                    actionResolver.resolveTap(profile, command.targetId, screenSize)
                        .fold(
                            onSuccess = { point -> gestureDispatcher.tap(point).getOrThrow() },
                            onFailure = { throw it },
                        )
                }

                is AutomationCommand.Swipe -> {
                    onCommand("swipe:${command.targetId}")
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
                    onCommand("wait:${safeDuration}ms")
                    delay(safeDuration)
                }

                is AutomationCommand.Log -> {
                    onCommand("log")
                    logger.info(command.message)
                }
            }
        }
    }
}
