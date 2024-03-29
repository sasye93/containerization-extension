/**
  * Temp location class, serves as a wrapper for generated images (kind of abstract representation of peer images).
  * @author Simon Schönwälder
  * @version 1.0
  */
package loci.container.build.types

import java.io.File
import java.nio.file.{Path, Paths}

import loci.container.build.IO.ContainerEntryPoint
import loci.container.build.Options

case class TempLocation(classSymbol : String, private val tempPath : Path, entryPoint : ContainerEntryPoint){
  def getImageName : String = loci.container.Tools.getIpString(classSymbol)
  def getRepoImageName : String = Options.dockerUsername + "/" + Options.dockerRepository.toLowerCase + ":" + this.getImageName
  def getAppropriateImageName : String = if(Options.published) this.getRepoImageName else this.getImageName
  def getServiceName : String = getImageName.split("_").last
  def getTempUri : java.net.URI = tempPath.toUri
  def getTempPath : Path = Paths.get(getTempUri)
  def getTempPathString : String = getTempPath.toString
  def getTempFile : File = tempPath.toFile
}
case object TempLocation{

}