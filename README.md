# ktor-health-check
[![license](https://img.shields.io/github/license/zensum/ktor-health-check.svg)]() [![](https://jitpack.io/v/zensum/ktor-health-check.svg)](https://jitpack.io/#zensum/ktor-health-check)

Simple, opinionated ktor health and readiness checks made for Kubernetes.

```kotlin
import ktor_health_check.Health

fun main(args: Array<String>) {
    embeddedServer(Netty, 80) {
        // Install the middleware...
        install(Health)
    }.start(wait = true)
}
```

... and boom, the your application now exposes a /healthz and /readyz
endpoint for use with for example Kubernetes. For a simple application
this is all you configuration you will ever need. In a more
complicated application we might want to our readycheck to start
failing if the database goes down.

```kotlin
fun main(args: Array<String>) {
    embeddedServer(Netty, 80) {
        install(Health) {
            readyCheck("database") { myDatabase.ping() }
        }
    }.start(wait = true)
}
```

And now getting `/readyz` returns:
```
HTTP/1.1 200 OK
Content-Type: application/json; charset=UTF-8
Content-Length: 17

{"database":true}
```

Lets add another check

```kotlin
install(Health) {
            readyCheck("database") { myDatabase.ping() }
            readyCheck("redis") { redis.ping() }
}
```

Now lets say someone tripped on the cord for our Redis server.

```
HTTP/1.1 500 Internal Server Error
Content-Type: application/json; charset=UTF-8
Content-Length: 31

{"database":true,"redis":false}
```

The database check is still returning true, but redis has turned
false. If any single check is down, as is the case here, the result of
the entire request becomes 500, indicating that the service isn't
operational.

For some use-cases you may want to expose checks on URLs other than
`/healthz` and `/readyz`. In that case we need to use `customCheck`

```kotlin
customCheck("/smoketest", "database") { database.test() }
```
And the smoketest should now be avaliable on `/smoketest`
 