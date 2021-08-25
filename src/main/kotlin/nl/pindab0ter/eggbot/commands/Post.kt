package nl.pindab0ter.eggbot.commands

import com.kotlindiscord.kord.extensions.commands.converters.impl.coalescedString
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType.EPHEMERAL
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType.PUBLIC
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommand
import dev.kord.common.annotation.KordPreview
import dev.kord.core.behavior.UserBehavior
import dev.kord.rest.builder.message.create.embed

private data class Post(
    val title: String,
    val author: UserBehavior,
    val body: String,
)

private val posts = mutableListOf<Post>()

private fun getPostByTitle(query: String) = posts.firstOrNull { post ->
    post.title == query
}

class GetArguments : Arguments() {
    val title by string("title", "The title of the post.")
}

class CreateArguments : Arguments() {
    val title by string("title", "The title of the post.")
    val body by coalescedString("body", "The text content to be placed within the post's body.")
}

@KordPreview
val postCommand: suspend SlashCommand<out Arguments>.() -> Unit = {
    name = "post"
    description = "create a post."

    subCommand(::GetArguments) {
        name = "get"
        description = "Get a post by title"
        autoAck = PUBLIC

        action {
            val post = getPostByTitle(arguments.title)

            publicFollowUp {
                content =
                    if (post == null) "No post found with that title"
                    else """
                            **${post.title}** (by ${post.author.asUserOrNull()?.username})

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

            ephemeralFollowUp { content = "New post created." }
        }
    }

    subCommand {
        name = "index"
        description = "Get all post titles."
        autoAck = PUBLIC

        action {
            publicFollowUp {
                embed {
                    title = "Posts"
                    field {
                        value =
                            if (posts.isEmpty()) "No posts found."
                            else posts.joinToString("\n", transform = Post::title)
                    }
                }
            }
        }
    }
}
