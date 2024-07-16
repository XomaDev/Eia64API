package space.themelon.eia64.io

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import space.themelon.eia64.runtime.Executor
import java.io.IOException
import java.net.Socket

class AutoCloseExecutor(
    exitCallback: () -> Unit,
) {

    companion object {
        // The Maximum session time of Executor cannot exceed 5 minutes.
        // Not enforcing so could affect resources and increase the load
        const val MAX_EXECUTION_TIME = 5 * 50 * 1000
    }

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        scope.launch {
            delay(MAX_EXECUTION_TIME.toLong())
            exitCallback()
        }
    }
}