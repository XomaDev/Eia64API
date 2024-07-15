package space.themelon.eia64

import space.themelon.eia64.runtime.Executor
import java.io.File
import kotlin.concurrent.thread

object Initiator {
    @JvmStatic
    fun main(args: Array<String>) {
        val userDirectory = System.getProperty("user.dir")
        Executor.STD_LIB = File(userDirectory, "eialib/stdlib/").absolutePath

        thread {
            EiaShell.main()
        }
        thread {
            EiaPlayground.main()
        }
        thread {
            FileEia.main()
        }
    }
}