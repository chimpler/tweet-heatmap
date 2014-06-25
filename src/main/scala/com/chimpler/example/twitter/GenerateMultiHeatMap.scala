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

  private def computeHeat(x: Int, y: Int, tweetGeos: mutable.Buffer[(Double, Double)]): Double = {
    var heat = 0.0d
    val intensity = 1.0d
    val maxDistance = imageWidth / 5
    for (tweetGeo <- tweetGeos) {
      // we compute the cartesian distance of 2 geo points
      // this formula does not compute the real distance but a approximate distance
      val (tweetX, tweetY) = Utils.toImageCoordinates(tweetGeo._1, tweetGeo._2, imageWidth, imageHeight)
      //if the tweets are too far from the pixel, we can skip them to save some CPU
      if (Math.abs(tweetX - x) < maxDistance && Math.abs(tweetY - y) < maxDistance) {
        val distanceSquare = ((tweetX - x) * (tweetX - x)) + ((tweetY - y) * (tweetY - y))
        if (distanceSquare == 0) {
          heat += intensity
        } else {
          heat += intensity / distanceSquare
        }
      }
    }
    heat
  }

  private def normalizeColorComponent(c: Int):Int = {
    if (c < 0) {
      0
    } else if (c > 255) {
      255
    } else {
      c
    }
  }

  private def getRedHeatColor(weight: Double): Color = {
    val r = (255 * weight).toInt
    val g = 0
    val b = 0

    new Color(
      normalizeColorComponent(r),
      g,
      b,
      200
    )
  }

  private def getMultiColorHeatColor(weight: Double): Color = {
    val s1 = 0.25d
    val s2 = 0.50d
    val s3 = 0.75d
    val s4 = 1.0d

    var r:Int = 0
    var g:Int = 0
    var b:Int = 0
    if (weight < s1) {
      // black to blue
      r = 0
      g = 0
      b = (255 * weight / s1).toInt
    } else if (weight < s2) {
      // blue to green
      r = 0
      g = (255 * (weight - s1) / (s2 - s1)).toInt
      b = (255 * (s2 - weight) / (s2 - s1)).toInt
    } else if (weight < s3) {
      // green to yellow
      r = (255 * (s3 - weight) / (s3 - s2)).toInt
      g = 255
      b = 0
    } else {
      // yellow to red
      r = (255 * (weight - s3) / (s4 - s3)).toInt
      g = (255 * (s4 - weight) / (s4 - s3)).toInt
      b = 0
    }

    new Color(
      normalizeColorComponent(r),
      normalizeColorComponent(g),
      normalizeColorComponent(b),
      200
    )
  }

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
