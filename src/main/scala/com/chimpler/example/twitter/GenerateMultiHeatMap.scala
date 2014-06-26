package com.chimpler.example.twitter

import java.awt.image.BufferedImage
import java.awt.Color
import javax.imageio.ImageIO
import java.io.{FileInputStream, File, BufferedReader, FileReader}
import scala.collection.mutable

object GenerateMultiHeatMap extends App {
  val backgroundImageFileName = "cartesian2d.jpg"

  val imageWidth = 1000
  val imageHeight = 500

  if (args.length < 4 || args.length > 7) {
    sys.error("Arguments: <tweet_file> <output_image_file> <max_heat> <keyword1> <keyword2> <keyword3> <keyword4>")
  }

  val tweetFileName = args(0)
  val outputImageFileName = args(1)
  val maxHeat = args(2).toDouble
  val keywords = args.slice(3, args.length) map (_.toLowerCase())

  val image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)
  val graphics = image.createGraphics()

  // draw map background
  val mapBackground = ImageIO.read(GenerateHeatMap.getClass.getClassLoader.getResourceAsStream(backgroundImageFileName))
  graphics.drawImage(mapBackground, 0, 0, imageWidth, imageHeight, Color.WHITE, null)

  val colorizers = Array(
    Colorizer.getColorizer("green"),
    Colorizer.getColorizer("red"),
    Colorizer.getColorizer("blue"),
    Colorizer.getColorizer("yellow")
  )

  val sumHeatMap = Array.ofDim[Int](imageWidth, imageHeight, 3)

  var c = 0
  for(keyword <- keywords) {
    val tweetGeos = Utils.readTweetFile(tweetFileName, Array(keyword))
    println(s"Found ${tweetGeos.size} tweets")

    val colorizer = colorizers(c)


    // iterate overall the pixels of the image
    // to determine the heat of the pixel
    val imageHeatMap = Utils.computeHeatMap(tweetGeos, imageWidth, imageHeight)

    val pixelCount = imageWidth * imageHeight
    var i = 0
    for (x <- 0 until imageWidth; y <- 0 until imageHeight) {
      val weight = imageHeatMap(x)(y) / maxHeat
      val color = colorizer.getColor(weight)
      sumHeatMap(x)(y)(0) = Math.max(sumHeatMap(x)(y)(0), color.getRed)
      sumHeatMap(x)(y)(1) = Math.max(sumHeatMap(x)(y)(1), color.getGreen)
      sumHeatMap(x)(y)(2) = Math.max(sumHeatMap(x)(y)(2), color.getBlue)
      i += 1
      if (i % 10000 == 0) {
        val percent = (i * 100) / pixelCount
        println(s"Done $percent%")
      }
    }

    c += 1
  }

  val heatMapImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)
  for (x <- 0 until imageWidth; y <- 0 until imageHeight) {
    val argb = (200 << 24) |
      (sumHeatMap(x)(y)(0)) << 16 |
      (sumHeatMap(x)(y)(1)) << 8 |
      (sumHeatMap(x)(y)(2))

    heatMapImage.setRGB(x, y, argb)
  }

  graphics.drawImage(heatMapImage, 0, 0, imageWidth, imageHeight, null, null)

  ImageIO.write(image, "png", new File(outputImageFileName))
}
