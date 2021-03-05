import club.eridani.command.*

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

    val cmd = "add"
//    println()
    println(commands.suggest(cmd.split(" ").filter { it.isNotEmpty() }))
//    commands.run(if (cmd.trim().isEmpty()) return else cmd.split(" "))
}

data class Pos(val x: Int, val y: Int, val z: Int)

@CommandBuilderDSL
fun commands(runner: StringCommandRunner.() -> Unit) = StringCommandRunner().apply(runner)

fun Argument<String>.pos() = arg(3) { val (x, y, z) = map { it.toInt() }; Pos(x, y, z) }