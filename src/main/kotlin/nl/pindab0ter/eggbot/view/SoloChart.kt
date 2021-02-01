package nl.pindab0ter.eggbot.view

import io.data2viz.axis.Orient
import io.data2viz.axis.axis
import io.data2viz.color.Colors
import io.data2viz.geom.Point
import io.data2viz.geom.size
import io.data2viz.scale.Scales
import io.data2viz.viz.JFxVizRenderer
import io.data2viz.viz.Margins
import io.data2viz.viz.viz
import javafx.embed.swing.SwingFXUtils
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.image.WritableImage
import kotlinx.coroutines.runBlocking
import nl.pindab0ter.eggbot.model.simulation.SoloContractState
import java.io.File
import javax.imageio.ImageIO
import kotlin.io.path.createTempFile
import kotlin.math.E
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.round

fun createChart(
    stateSeries: List<SoloContractState>,
): File = runBlocking {
    val canvasSize = size(960, 500)
    val margins = Margins(40.5, 30.5, 50.5, 50.5)

    val xScale = Scales.Continuous.linear {
        domain = listOf(.0, stateSeries.last().timeElapsed.standardMinutes.toDouble())
        range = listOf(.0, canvasSize.width - margins.hMargins)
    }

    val yScale = Scales.Continuous.log(E) {
        domain = listOf(exp(.0), stateSeries.last().runningEggsLaid.toDouble())
        range = listOf(canvasSize.height - margins.vMargins, .0)
    }

    val points = stateSeries.map { state ->
        Point(state.timeElapsed.standardMinutes.toDouble(), state.runningEggsLaid.toDouble())
    }

    val canvas = Canvas(canvasSize.width, canvasSize.height)
    val notQuiteWhite = Colors.rgb(220, 221, 222)
    val darkButNotBlack = Colors.rgb(153, 170, 181)
    val notQuiteBlack = Colors.rgb(47, 49, 54)

    val viz = viz {
        size = canvasSize
        fill = Colors.rgb(47, 49, 54)

        // Background
        rect {
            size = canvasSize
            fill = notQuiteBlack
        }

        group {
            transform {
                translate(x = margins.left, y = margins.top)
            }

            group {
                transform {
                    translate(x = -10.0)
                }
                axis(Orient.LEFT, yScale) {
                    axisStroke = notQuiteWhite
                    tickStroke = notQuiteWhite
                    fontColor = notQuiteWhite
                    tickFormat = { "e${round(ln(it)).toInt()}" }
                }
            }

            group {
                transform {
                    translate(y = canvasSize.height - margins.vMargins + 10.0)
                }
                axis(Orient.BOTTOM, xScale) {
                    axisStroke = notQuiteWhite
                    tickStroke = notQuiteWhite
                    fontColor = notQuiteWhite
                }
            }

            group {
                path {
                    fill = null
                    stroke = Colors.Web.linen
                    strokeWidth = 2.0

                    moveTo(xScale(points.first().x), yScale(points.first().y))
                    for (point in points) {
                        lineTo(xScale(point.x), yScale(point.y))
                    }
                }
            }
        }
    }

    JFxVizRenderer(canvas, viz)
    viz.render()
    val writableImage = WritableImage(canvasSize.width.toInt(), canvasSize.height.toInt())
    canvas.snapshot(SnapshotParameters(), writableImage)
    val renderedImage = SwingFXUtils.fromFXImage(writableImage, null)
    val file = createTempFile(prefix = "chart", suffix = ".png").toFile()
    ImageIO.write(renderedImage, "png", file)
    return@runBlocking file
}
