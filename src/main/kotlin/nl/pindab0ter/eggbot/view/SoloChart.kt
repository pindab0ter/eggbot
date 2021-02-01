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
    val margins = Margins(40.5, 30.5, 50.5, 50.5)
    val chartWidth = 960.0 - margins.hMargins
    val chartHeight = 500.0 - margins.vMargins

    val xScale = Scales.Continuous.linear {
        domain = listOf(.0, stateSeries.last().timeElapsed.standardMinutes.toDouble())
        range = listOf(.0, chartWidth)
    }

    val yScale = Scales.Continuous.log(E) {
        domain = listOf(exp(.0), stateSeries.last().runningEggsLaid.toDouble())
        range = listOf(chartHeight, .0)
    }

    val points = stateSeries.map { state ->
        Point(state.timeElapsed.standardMinutes.toDouble(), state.runningEggsLaid.toDouble())
    }

    val canvas = Canvas(960.0, 500.0)

    val viz = viz {
        size = size(960, 500)

        group {
            transform {
                translate(x = margins.left, y = margins.top)
            }

            group {
                transform {
                    translate(x = -10.0)
                }
                axis(Orient.LEFT, yScale) {
                    tickFormat = { "e${round(ln(it)).toInt()}" }
                }
            }

            group {
                transform {
                    translate(y = chartHeight + 10.0)
                }
                axis(Orient.BOTTOM, xScale)
            }

            group {
                path {
                    fill = null
                    stroke = Colors.Web.steelblue
                    strokeWidth = 1.5

                    moveTo(xScale(points[0].x), yScale(points[0].y))
                    for (point in points) {
                        lineTo(xScale(point.x), yScale(point.y))
                    }
                }
            }
        }
    }

    JFxVizRenderer(canvas, viz)
    viz.render()
    val writableImage = WritableImage(960, 500)
    canvas.snapshot(SnapshotParameters(), writableImage)
    val renderedImage = SwingFXUtils.fromFXImage(writableImage, null)
    val file = createTempFile(prefix = "chart", suffix = ".png").toFile()
    ImageIO.write(renderedImage, "png", file)
    return@runBlocking file
}
