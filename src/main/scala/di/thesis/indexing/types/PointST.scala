package di.thesis.indexing.types

case class PointST (longitude:Double, latitude:Double, timestamp:Long) {

  def getTimestamp:Long={
    this.timestamp
  }

  def getLongitude:Double={
    this.longitude
  }

  def getLatitude:Double={
    this.latitude
  }
}
