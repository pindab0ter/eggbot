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
import nl.pindab0ter.eggbot.helpers.formatIllions
import nl.pindab0ter.eggbot.model.simulation.SoloContractState
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import javax.imageio.ImageIO
import kotlin.io.path.createTempFile
import kotlin.math.E
import kotlin.math.ceil

fun createChart(
    stateSeries: List<SoloContractState>,
): File = runBlocking {
    val canvasSize = size(960, 500)
    val margins = Margins(40.5, 30.5, 50.5, 80.5)

    val xScale = Scales.Continuous.linear {
        domain = listOf(.0, ceil(stateSeries.last().timeElapsed.standardMinutes.toDouble()))
        range = listOf(.0, canvasSize.width - margins.hMargins)
    }

    val yScale = Scales.Continuous.log(E) {
        domain = listOf(stateSeries.first().currentEggsLaid.toDouble(), maxOf(
            stateSeries.last().goals.last().amount.toDouble(),
            stateSeries.last().runningEggsLaid.toDouble()
        ))
        range = listOf(canvasSize.height - margins.vMargins, .0)
    }

    val points = stateSeries.map { state ->
        Point(state.timeElapsed.standardMinutes.toDouble(), state.runningEggsLaid.toDouble())
    }

    val canvas = Canvas(canvasSize.width, canvasSize.height)
    val notQuiteWhite = Colors.rgb(220, 221, 222)
    val darkButNotBlack = Colors.rgb(153, 170, 181)
    val notQuiteBlack = Colors.rgb(47, 49, 54)
    val blurple = Colors.rgb(114, 137, 218)

    val viz = viz {
        size = canvasSize
        fill = Colors.rgb(47, 49, 54)

        // Background
        rect {
            size = canvasSize
            fill = notQuiteBlack
        }

        // TODO: Add lines where bottlenecks are hit

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
                    tickFormat = { it.toBigDecimal().formatIllions() }
                }
            }

            group {
                transform {
                    translate(y = canvasSize.height - margins.vMargins + 10.0)
                }
                val formatter = DecimalFormat(".#", DecimalFormatSymbols.getInstance(Locale.ENGLISH))
                axis(Orient.BOTTOM, xScale) {
                    axisStroke = notQuiteWhite
                    tickStroke = notQuiteWhite
                    fontColor = notQuiteWhite
                    tickFormat = {
                        when (it) {
                            0.0 -> "Now"
                            else -> "${formatter.format(it / 60.0 / 24.0)} days"
                        }
                    }
                }
            }

            stateSeries.first().goals.reversed().forEachIndexed { index, goal ->
                group {
                    path {
                        fill = null
                        stroke = Colors.Web.green
                        strokeWidth = 2.0
                        dashedLine = doubleArrayOf(index * 5.0)

                        moveTo(0.0, yScale(goal.amount.toDouble()))
                        lineTo(canvasSize.width - margins.hMargins, yScale(goal.amount.toDouble()))
                    }
                }
            }

            group {
                path {
                    fill = null
                    stroke = Colors.Web.red
                    strokeWidth = 2.0

                    val deadline = xScale(stateSeries.first().timeRemaining.standardMinutes.toDouble())

                    moveTo(deadline, 0.0)
                    lineTo(deadline, canvasSize.height - margins.vMargins)
                }
            }

            group {
                path {
                    fill = null
                    stroke = blurple
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
