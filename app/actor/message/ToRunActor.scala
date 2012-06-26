package org.w3.vs.actor.message


// Could be Run/Cancel to be consistent with the ui?
case object Refresh
case object Stop

case object BeProactive
case object BeLazy

case object GetRun
case object NoMorePendingAssertion

case object GetOrgEnumerator
case object GetJobEnumerator
