package nl.pindab0ter.eggbot.extensions

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.coalescingString
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.interaction.suggestString
import dev.kord.rest.builder.message.create.embed
import mu.KotlinLogging

/**
 * This class is for testing purposes only.
 */
class PostExtension : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = javaClass.simpleName

    private data class Post(
        val title: String,
        val author: UserBehavior,
        val body: String,
    )

    private val posts = mutableListOf<Post>()

    override suspend fun setup() {

        ephemeralSlashCommand {
            name = "post"
            description = "create a post."

            class GetArguments : Arguments() {
                val title by string {
                    name = "title"
                    description = "The title of the post."
                    autoComplete {
                        suggestString {
                            posts.forEach { post -> choice(post.title, post.title) }
                        }
                    }
                }
            }

            ephemeralSubCommand(::GetArguments) {
                name = "get"
                description = "Get a post by title"

                action {
                    val post = posts.firstOrNull { post ->
                        post.title == arguments.title
                    }

                    respond {
                        content = if (post == null) {
                            "No post found with that title"
                        } else {
                            """
                            **${post.title}** (by ${post.author.asUserOrNull()?.username})
        
                            ${post.body}
                            """.trimIndent()
                        }
                    }
                }
            }

            class CreateArguments : Arguments() {
                val title by string {
                    name = "title"
                    description = "The title of the post."
                }
                val body by coalescingString {
                    name = "body"
                    description = "The text content to be placed within the post's body."
                }
            }

            ephemeralSubCommand(::CreateArguments) {
                name = "create"
                description = "Create a new post"

                action {
                    val post = Post(
                        title = arguments.title,
                        author = event.interaction.user,
                        body = arguments.body
                    )

                    posts.add(post)

                    respond { content = "New post created." }
                }
            }

            ephemeralSubCommand {
                name = "index"
                description = "Get all post titles."

                action {
                    respond {
                        embed {
                            title = "Posts"
                            field {
                                value = if (posts.isEmpty()) {
                                    "No posts found."
                                } else {
                                    posts.joinToString("\n", transform = Post::title)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
