package nl.pindab0ter.eggbot.kord

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.converters.coalescedString
import com.kotlindiscord.kord.extensions.commands.converters.string
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType.EPHEMERAL
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType.PUBLIC
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.annotation.KordPreview
import dev.kord.core.behavior.UserBehavior

@KordPreview
class FarmersExtension(bot: ExtensibleBot) : Extension(bot) {
    override val name: String = "FarmersExtension"

    class GetArguments : Arguments() {
        val title by string("title", "The title of the post.")
    }

    class CreateArguments : Arguments() {
        val title by string("title", "The title of the post.")
        val body by coalescedString("body", "The text content to be placed within the post's body.")
    }

    data class Post(
        val title: String,
        val author: UserBehavior,
        val body: String
    )

    val posts = mutableListOf<Post>()

    fun getPostByTitle(query: String) = posts.first { post ->
        post.title == query
    }

    override suspend fun setup() {
        slashCommand {
            name = "ping"
            description = "Ping!"

            action {
                ephemeralFollowUp("Pong!")
            }
        }


        slashCommand {
            name = "post"
            description = "create a post."


            // action {
            //     with(arguments) {
            //         logger().debug { "**$title** (by ${event.interaction.user.mention})\n\n$body" }
            //         // channel.createMessage("**$title** (by ${event.interaction.user.mention})\n\n$body")
            //         ephemeralFollowUp("Calculating…")
            //         publicFollowUp {
            //             content = "**$title** (by ${getUser().mention})\n\n$body"
            //         }
            //     }
            // }

            subCommand(::GetArguments) {
                name = "get"
                description = "Get a post by title"
                autoAck = PUBLIC

                action {
                    val post = getPostByTitle(arguments.title)

                    publicFollowUp {
                        content = """
                            **${post.title}** (by ${post.author.mention})

                            ${post.body}
                            """.trimIndent()
                    }
                }
            }

            subCommand(::CreateArguments) {
                name = "create"
                description = "Create a new post"
                autoAck = EPHEMERAL

                action {
                    val post = Post(arguments.title, event.interaction.user, arguments.body)
                    posts.add(post)

                    ephemeralFollowUp("New post created.")
                }
            }
        }
    }
}