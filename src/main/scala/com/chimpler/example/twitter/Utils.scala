package com.chimpler.example.twitter

import java.io.{FileReader, BufferedReader}
import scala.collection.mutable

object Utils {

  def toImageCoordinates(latitude: Double, longitude: Double, imageWidth: Int, imageHeight: Int): (Int, Int) = {
    (
      (imageWidth * (0.5 + longitude / 360)).toInt,
      (imageHeight * (0.5 - latitude / 180)).toInt
      )
  }


  def toWorldCoordinates(x: Int, y: Int, imageWidth: Int, imageHeight: Int): (Double, Double) = {
    (
      180 * (0.5 - y.toDouble / imageHeight),
      360 * (x.toDouble / imageWidth - 0.5)
      )
  }

  def readTweetFile(tweetFileName: String, keywords: Array[String]): mutable.Buffer[(Double, Double)] = {
    val reader = new BufferedReader(new FileReader(tweetFileName))

    var line: String = null

    val tweetGeos = mutable.Buffer.empty[(Double, Double)]
    do {
      line = reader.readLine()
      if (line != null) {
        try {
          val tokens = line.split(",", 3)

          if (tokens.length >= 2) {
            val text = tokens(2)
            // check if the text contains one of the keyword
            if (keywords.isEmpty || (keywords exists (text.toLowerCase().contains))) {
              val latitude = tokens(0).trim.toDouble
              val longitude = tokens(1).trim.toDouble
              tweetGeos += ((latitude, longitude))
            }
          }
        }
      }
    } while (line != null)
    tweetGeos
  }

  private def computeHeat(x: Int, y: Int, tweetGeos: mutable.Buffer[(Double, Double)], imageWidth:Int, imageHeight: Int): Double = {
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

  def computeHeatMap(tweetGeos: mutable.Buffer[(Double, Double)], imageWidth:Int, imageHeight: Int) = {
    val imageHeatMap = Array.ofDim[Double](imageWidth, imageHeight)
    for(x <- 0 until imageWidth ; y<-0 until imageHeight) {
      val heat = computeHeat(x, y, tweetGeos, imageWidth, imageHeight)
      imageHeatMap(x)(y) = heat
    }
    imageHeatMap
  }
}
