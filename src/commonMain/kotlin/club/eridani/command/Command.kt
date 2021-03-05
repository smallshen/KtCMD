package club.eridani.command

@DslMarker annotation class CommandBuilderDSL
@DslMarker annotation class ExecutorBuilderDSL
@DslMarker annotation class ParserDSL

abstract class Command<ARGTYPE, C : Command<ARGTYPE, C, RUNNER>, RUNNER : CommandRunner<ARGTYPE, C, RUNNER>>(
    val name: String,
    var description: String = "No Description",
    val runner: RUNNER,
) {
    fun suggest(args: Argument<ARGTYPE>) : List<ARGTYPE> { return listOf()  }

    val subcommands = mutableListOf<C>()

    private val executors = mutableListOf<(Argument<ARGTYPE>) -> Unit>()

    fun execute(args: Argument<ARGTYPE>) {
        if (args.isEmpty()) {
            executors.forEach { it(args) }
            return
        }
        if (subcommands.isNotEmpty()) {
            val subCommand = subcommands.find { it.name.equals(args[0].toString(), true) }
            if (subCommand == null) {
                runner.outputMessage("Avalible SubCommands:\n${
                    subcommands
                        .joinToString("\n") { it.name + " - " + it.description }
                }")
            } else subCommand.execute(Argument(args.toMutableList().apply { removeFirstOrNull() }))
        } else executors.forEach { it(args) }
    }

    @ExecutorBuilderDSL fun execute(func: Argument<ARGTYPE>.() -> Unit) = executors.add(func)

    @CommandBuilderDSL
    fun subCommand(name: String, description: String = "No Description", applier: C.() -> Unit) =
        runner.createCommand(name, description, applier).apply { this@Command.subcommands.add(this) }

    @CommandBuilderDSL
    operator fun String.invoke(applier: C.() -> Unit) = subCommand(this, applier = applier)
}

class Argument<ARG>(val content: List<ARG>, var index: Int = 0) : List<ARG> by content {
    @ParserDSL fun <R> arg(size: Int = 1, parser: Argument<ARG>.() -> R): R? =
        runCatching { parser(Argument(this.subList(index, index + size))).also { index += size } }.getOrNull()


    @ParserDSL fun <R> vararg(size: Int = this.size, parser: Argument<ARG>.() -> R): List<R> =
        runCatching { Argument(this.subList(index, size)).map { parser(Argument(listOf(it))) } }
            .getOrElse { listOf() }


}

fun <T> T?.default(value: T) = this ?: value
fun <T> T?.required(message: String = "Missing Value") = this ?: error(message)

fun Argument<String>.int() = arg { first().toInt() }
fun Argument<String>.long() = arg { first().toLong() }
fun Argument<String>.float() = arg { first().toFloat() }
fun Argument<String>.double() = arg { first().toDouble() }
fun Argument<String>.text() = arg { first() }
fun Argument<String>.jointext() = vararg { text().required() }.joinToString(" ")

interface CommandRunner<ARGTYPE, C : Command<ARGTYPE, C, RUNNER>, RUNNER : CommandRunner<ARGTYPE, C, RUNNER>> {
    val commands: MutableList<C>
    fun run(input: List<ARGTYPE>)
    fun outputMessage(message: String) = println(message)
    operator fun String.invoke(applier: C.() -> Unit): C =
        createCommand(this, applier = applier).apply { commands.add(this) }

    fun createCommand(name: String, description: String = "No Description", applier: C.() -> Unit): C

    fun suggest(input: List<ARGTYPE>): List<String> {
        var cmds: List<C>? = commands
        repeat(input.size) { index ->
            cmds = cmds?.find { it.name.startsWith(input.getOrNull(index).toString(), true) }?.subcommands
        }

        return cmds?.map { it.name } ?: listOf()
    }
}

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

