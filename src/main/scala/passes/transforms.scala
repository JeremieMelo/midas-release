package strober
package passes

import firrtl._
import firrtl.Annotations._
import scala.collection.mutable.{HashMap, LinkedHashSet, ArrayBuffer}
import scala.util.DynamicVariable

private class TransformContext {
  type ChainMap = HashMap[String, ArrayBuffer[ir.Statement]]
  val childInsts = HashMap[String, LinkedHashSet[String]]()
  val childMods = HashMap[String, LinkedHashSet[String]]()
  val instToMod = HashMap[(String, String), String]()
  val chains = (ChainType.values.toList map (_ -> new ChainMap)).toMap
}

case class DaisyChainAnnotation(t: String) extends Annotation with Loose with Unstable {
  val target = CircuitName(t)
  val tID = TransID(1) 
  def duplicate(n: Named) = this.copy(t=n.name)
}

private[strober] object StroberTransforms extends Transform with SimpleRun {
  private val contextVar = new DynamicVariable[Option[TransformContext]](None)
  private[passes] def context = contextVar.value.getOrElse (new TransformContext)
  def execute(circuit: ir.Circuit, map: AnnotationMap) = {
    (contextVar withValue Some(new TransformContext)){
      val fame1 = run(circuit, Seq(
        Analyses,
        Fame1Transform))
      map get TransID(1) match {
        case Some(p) => p get CircuitName(circuit.main) match {
          case Some(DaisyChainAnnotation(_)) =>
            run(fame1.circuit, Seq(
              AddDaisyChains,
              DumpChains))
          case _ => fame1
        }
        case _ => fame1
      }
    }
  }
}