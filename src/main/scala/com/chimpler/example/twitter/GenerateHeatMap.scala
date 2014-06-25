package com.chimpler.example.twitter

import java.awt.image.BufferedImage
import java.awt.Color
import javax.imageio.ImageIO
import java.io.{FileInputStream, File, BufferedReader, FileReader}
import scala.collection.mutable

object GenerateHeatMap extends App {
  val backgroundImageFileName = "cartesian2d.jpg"

  val imageWidth = 1000
  val imageHeight = 500

  if (args.length < 4) {
    sys.error("Arguments: <tweet_file> <output_image_file> <color_scheme> <max_heat> <keywords>")
  }

  val tweetFileName = args(0)
  val outputImageFileName = args(1)
  val colorScheme = args(2)
  val maxHeat = args(3).toDouble
  val keywordFilter = args.slice(4, args.length) map (_.toLowerCase())

  val tweetGeos = Utils.readTweetFile(tweetFileName, keywordFilter)
  println(s"Found ${tweetGeos.size} tweets")

  // iterate overall the pixels of the image
  // to determine the heat of the pixel
  val imageHeatMap = Utils.computeHeatMap(tweetGeos, imageWidth, imageHeight)

  val colorizer = Colorizer.getColorizer(colorScheme)

  val heatMapImage =
    new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)
  val pixelCount = imageWidth * imageHeight
  var i = 0
  for (x <- 0 until imageWidth; y <- 0 until imageHeight) {
    val weight = imageHeatMap(x)(y) / maxHeat
    val color = colorizer.getColor(weight)
    heatMapImage.setRGB(x, y, color.getRGB)
    i += 1
    if (i % 10000 == 0) {
      val percent = (i * 100) / pixelCount
      println(s"Done $percent%")
    }
  }

  val image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)
  val graphics = image.createGraphics()

  // draw map background
  val mapBackground = ImageIO.read(GenerateHeatMap.getClass.getClassLoader.getResourceAsStream(backgroundImageFileName))
  graphics.drawImage(mapBackground, 0, 0, imageWidth, imageHeight, Color.WHITE, null)

  graphics.drawImage(heatMapImage, 0, 0, imageWidth, imageHeight, null, null)

  graphics.setColor(new Color(255, 255, 255, 40))
  for (tweetGeo <- tweetGeos) {
    val (x, y) = Utils.toImageCoordinates(tweetGeo._1, tweetGeo._2, imageWidth, imageHeight)
    graphics.fillOval(x, y, 1, 1)
  }

  ImageIO.write(image, "png", new File(outputImageFileName))
}
