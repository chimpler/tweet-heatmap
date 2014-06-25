package com.chimpler.example.twitter

import java.awt.image.BufferedImage
import java.awt.Color
import javax.imageio.ImageIO
import java.io.File

object GenerateMap extends App {
  val backgroundImageFileName = "cartesian2d.jpg"
  val imageWidth = 1000
  val imageHeight = 500

  if (args.length < 2) {
    sys.error("Arguments: <tweet_file> <output_image_file> <keywords>")
  }
  val tweetFileName = args(0)
  val outputImageFileName = args(1)
  val keywordFilter = args.slice(2, args.length) map (_.toLowerCase())

  val tweetGeos = Utils.readTweetFile(tweetFileName, keywordFilter)
  println(s"Found ${tweetGeos.size} tweets")

  // create image
  val image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)
  val graphics = image.createGraphics()

  // draw map background
  val mapBackground = ImageIO.read(GenerateMap.getClass.getClassLoader.getResourceAsStream(backgroundImageFileName))
  graphics.drawImage(mapBackground, 0, 0, imageWidth, imageHeight, Color.WHITE, null)
  graphics.setColor(new Color(0, 0, 0, 200))
  graphics.fillRect(0, 0, imageWidth, imageHeight)

  graphics.setColor(new Color(0, 255, 0, 50))
  for (tweetGeo <- tweetGeos) {
    val (x, y) = Utils.toImageCoordinates(tweetGeo._1, tweetGeo._2, imageWidth, imageHeight)
    graphics.fillOval(x - 1, y - 1, 2, 2)
  }

  ImageIO.write(image, "png", new File(outputImageFileName))
}
