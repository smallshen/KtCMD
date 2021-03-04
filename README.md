结构

Argument Type 通常为 String, 一般交互基本都是字符串。特殊情况如 Discord, QQ(eg: Mirai -> SingleMessage) 这种不单单有 String

```kotlin
class Command<ArgumentType, CommandImplementation, CommandRunner>
```

```kotlin
class CommandRunner<ArgumentType, CommandImplementation, RunnerImplementation>
```

自定义Parser

```kotlin
val a: ResultType = arg(size 默认 1) { lambda -> ResultType }
```

可通过扩展

```kotlin
fun Argument<ArgumentType>.customParser() = arg(size) { lambda -> ResultType }
```

自定义 top level runner dsl

```kotlin
@CommandBuilderDSL
fun commands(runner: 自己的Runner.() -> Unit) = 自己的Runner().apply(runner)
```

Examples:

see [BuiltInStringCommand](./src/commonMain/kotlin/club/eridani/command/Command.kt)

```kotlin
open class StringCommand(name: String, description: String = "No Description", runner: StringCommandRunner) :
    Command<String, StringCommand, StringCommandRunner>(name, description, runner)

open class StringCommandRunner : CommandRunner<String, StringCommand, StringCommandRunner> {
    override val commands: MutableList<StringCommand> = mutableListOf()

    override fun run(input: List<String>) {
        if (input.none { it.isNotEmpty() }) return else {
            val command = commands.find { it.name.equals(input[0], true) }
            command ?: outputMessage("Command \"${input[0]}\" Not Found\nAvalible Commands:\n" +
                    commands.joinToString("\n") { it.name + " - " + it.description })
            command ?: return
            runCatching {
                command.execute(Argument(input.toMutableList().apply { removeFirstOrNull() }))
            }.exceptionOrNull()
                ?.also { outputMessage("Error on running Command: ${it.message}") }
        }
    }

    override fun createCommand(name: String, description: String, applier: StringCommand.() -> Unit) =
        StringCommand(name, description, this).apply(applier)

}
```

```kotlin
fun main() {
    val commands =
        commands {
            "set" {
                execute {
                    val name = text().required()
                    val number = int().required()
                    val pos = pos().required()
                    val ids = vararg { int().required() }
                    println(name)
                    println(number)
                    println(pos)
                    println(ids)
                }
            }

            "add" {
                //SubCommands
                "what" {
                    execute {
                        val name = jointext()
                        println(name)
                    }
                }

                "oopps" {
                    execute {
                        println("oops")
                    }
                }
            }
        }
    commands.run(with(readLine()!!) { if (trim().isEmpty()) return else split(" ") })
}

data class Pos(val x: Int, val y: Int, val z: Int)

@CommandBuilderDSL
fun commands(runner: StringCommandRunner.() -> Unit) = StringCommandRunner().apply(runner)

fun Argument<String>.pos() = arg(3) { val (x, y, z) = map { it.toInt() }; Pos(x, y, z) }
```