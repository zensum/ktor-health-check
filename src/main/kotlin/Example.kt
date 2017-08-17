import ktor_health_check.Health
import org.jetbrains.ktor.application.install
import org.jetbrains.ktor.host.embeddedServer
import org.jetbrains.ktor.netty.Netty
import org.jetbrains.ktor.response.respondText
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.routing.routing

fun main(args: Array<String>) {
    embeddedServer(Netty, 8123) {
        install(Health)
        routing {
            get("/") {
                call.respondText { "yee b0i" }
            }
            get("/healthz") {
                call.respondText { "me 2 tankz" }
            }
        }
    }.start(wait = true)
}