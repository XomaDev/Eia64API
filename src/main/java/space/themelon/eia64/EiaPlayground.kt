package space.themelon.eia64

import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.UserAuthNoneFactory
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.server.shell.ShellFactory
import java.nio.file.Paths

object EiaPlayground {

    fun main() {
        SshServer.setUpDefaultServer().apply {
            port = 2121
            keyPairProvider = SimpleGeneratorHostKeyProvider(Paths.get("eiaplayground.ser"))
            userAuthFactories = listOf(UserAuthNoneFactory.INSTANCE)
            shellFactory = ShellFactory {
                EiaCommand { input, output, exitCallback ->
                    EiaSession(input, output, exitCallback)
                }
            }
            start()
        }
    }
}