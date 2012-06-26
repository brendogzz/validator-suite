package org.w3.vs.model

import org.w3.vs._
import org.w3.util._
import akka.dispatch._
import org.w3.banana._

object Strategy {
  
  def getStrategyVO(id: StrategyId)(implicit conf: VSConfiguration): FutureVal[Exception, StrategyVO] = {
    import conf.binders._
    implicit val context = conf.webExecutionContext
    val uri = StrategyUri(id)
    FutureVal(conf.store.getNamedGraph(uri)) flatMapValidation { graph => 
      val pointed = PointedGraph(uri, graph)
      StrategyVOBinder.fromPointedGraph(pointed)
    }
  }

  def get(id: StrategyId)(implicit conf: VSConfiguration): FutureVal[Exception, Strategy] =
    getStrategyVO(id) map { Strategy(_) }

  def saveStrategyVO(vo: StrategyVO)(implicit conf: VSConfiguration): FutureVal[Exception, Unit] = {
    import conf.binders._
    implicit val context = conf.webExecutionContext
    val graph = StrategyVOBinder.toPointedGraph(vo).graph
    val result = conf.store.addNamedGraph(StrategyUri(vo.id), graph)
    FutureVal(result)
  }

  def save(strategy: Strategy)(implicit conf: VSConfiguration): FutureVal[Exception, Unit] =
    saveStrategyVO(strategy.toValueObject)
  
  def delete(strategy: Strategy)(implicit conf: VSConfiguration): FutureVal[Exception, Unit] =
    sys.error("")
    
  def apply(vo: StrategyVO)(implicit conf: VSConfiguration): Strategy = {
    import vo._
    Strategy(id, entrypoint, linkCheck, maxResources, filter, assertorSelector)
  }

}

case class Strategy (
    id: StrategyId = StrategyId(),
    entrypoint: URL,
    linkCheck: Boolean,
    maxResources: Int,
    filter: Filter = Filter.includeEverything,
    assertorSelector: AssertorSelector = AssertorSelector.simple)(implicit conf: VSConfiguration) {
  
  val mainAuthority: Authority = entrypoint.authority
  
  val authorityToObserve: Authority = mainAuthority
  
  def getActionFor(url: URL): HttpAction =
    if (filter.passThrough(url)) {
      if (url.authority == entrypoint.authority)
        GET
      else if (linkCheck)
        HEAD
      else
        IGNORE
    } else {
      IGNORE
    }

  def noAssertor(): Strategy = this.copy(assertorSelector = AssertorSelector.noAssertor)
  
  def save() = Strategy.save(this)
  
  def delete() = Strategy.delete(this)
  
  def toValueObject: StrategyVO = StrategyVO(id, entrypoint, linkCheck, maxResources, filter, assertorSelector)
  
}
