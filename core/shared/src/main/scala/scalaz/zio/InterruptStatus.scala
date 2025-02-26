/*
 * Copyright 2017-2019 John A. De Goes and the ZIO Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package scalaz.zio

/**
 * The `InterruptStatus` of a fiber determines whether or not it can be
 * interrupted. The status can change over time in different regions.
 */
sealed abstract class InterruptStatus extends Serializable with Product {
  final def isInterruptible: Boolean   = this match { case InterruptStatus.Interruptible => true; case _ => false }
  final def isUninterruptible: Boolean = !isInterruptible

  private[zio] final def toBoolean: Boolean = isInterruptible
}
object InterruptStatus {
  def interruptible: InterruptStatus   = Interruptible
  def uninterruptible: InterruptStatus = Uninterruptible

  /**
   * Indicates the fiber can be interrupted right now.
   */
  case object Interruptible extends InterruptStatus

  /**
   * Indicates the fiber cannot be interrupted right now.
   */
  case object Uninterruptible extends InterruptStatus

  private[zio] def fromBoolean(b: Boolean): InterruptStatus = if (b) Interruptible else Uninterruptible
}
