package org.w3.vs.model

import org.w3.vs._
import org.w3.util._
import org.w3.vs.actor._
import scalaz._
import Scalaz._
import akka.dispatch._
import org.w3.vs.exception._
import org.w3.banana._

object User {

  def apply(vo: UserVO)(implicit conf: VSConfiguration): User =
    User(vo.id, vo.name, vo.email, vo.password, vo.organizationId)

  def getUserVO(id: UserId)(implicit conf: VSConfiguration): FutureVal[Exception, UserVO] = {
    import conf.binders._
    implicit val context = conf.webExecutionContext
    val uri = UserUri(id)
    FutureVal.applyTo(conf.store.getNamedGraph(uri)) flatMapValidation { graph => 
      val pointed = PointedGraph(uri, graph)
      UserVOBinder.fromPointedGraph(pointed)
    }
  }
  
  def get(id: UserId)(implicit conf: VSConfiguration): FutureVal[Exception, User] =
    getUserVO(id) map { User(_) }

  //def getForOrganization(id: OrganizationId)(implicit conf: VSConfiguration): FutureVal[Exception, Iterable[User]] = sys.error("") 
  
  def authenticate(email: String, password: String)(implicit conf: VSConfiguration): FutureVal[Exception, User] = {
    implicit def ec = conf.webExecutionContext
    FutureVal.successful(play.api.Global.tgambet)
    
    // I tried the following but it doesn't seem to work. Note the failMap {_ => Unauthenticated}
    /*implicit val context = conf.webExecutionContext
    import conf._
    import conf.ops._
    import conf.projections._
    import conf.binders.{ xsd => _, _ }
    import conf.diesel._
    val query = """
CONSTRUCT {
  ?s ?p ?o
} WHERE {
  graph ?g {
    ?s ont:email "#email"^^xsd:string .
    ?s ont:password "#password"^^xsd:string .
    ?s ?p ?o
  }
}
""".replaceAll("#email", email).replaceAll("#password", password)
    val construct = SparqlOps.ConstructQuery(query, xsd, ont)
    FutureVal.applyTo(store.executeConstruct(construct)) flatMapValidation { graph =>
      graph.getAllInstancesOf(ont.User).exactlyOneUri flatMap { uri =>
        val pointed = PointedGraph(uri, graph)
        UserVOBinder.fromPointedGraph(pointed)
      } map { User(_) }
    } failMap {_ => Unauthenticated} */
  }
  
  def getByEmail(email: String)(implicit conf: VSConfiguration): FutureVal[Exception, User] = {
    implicit val context = conf.webExecutionContext
    import conf._
    import conf.ops._
    import conf.projections._
    import conf.binders.{ xsd => _, _ }
    import conf.diesel._
    val query = """
CONSTRUCT {
  ?s ?p ?o
} WHERE {
  graph ?g {
    ?s ont:email "#email"^^xsd:string .
    ?s ?p ?o
  }
}
""".replaceAll("#email", email)
    val construct = SparqlOps.ConstructQuery(query, xsd, ont)
    FutureVal.applyTo(store.executeConstruct(construct)) flatMapValidation { graph =>
      graph.getAllInstancesOf(ont.User).exactlyOneUri flatMap { uri =>
        val pointed = PointedGraph(uri, graph)
        UserVOBinder.fromPointedGraph(pointed)
      } map { User(_) }
    }
  }

  def saveUserVO(vo: UserVO)(implicit conf: VSConfiguration): FutureVal[Exception, Unit] = {
    import conf.binders._
    implicit val context = conf.webExecutionContext
    val graph = UserVOBinder.toPointedGraph(vo).graph
    val result = conf.store.addNamedGraph(UserUri(vo.id), graph)
    FutureVal.toFutureValException(FutureVal.applyTo(result))
  }
  
  def save(user: User)(implicit conf: VSConfiguration): FutureVal[Exception, Unit] =
    saveUserVO(user.toValueObject)

}



case class User (
    id: UserId = UserId(),
    name: String,
    email: String,
    password: String,
    organizationId: OrganizationId)(implicit conf: VSConfiguration) {
  
  def getOrganization: FutureVal[Exception, Organization] = Organization.get(organizationId)
  
  def getJobs: FutureVal[Exception, Iterable[Job]] = Job.getFor(this)
  
  // getJob with id only if owned by user. should probably be a db request directly.
  def getJob(id: JobId): FutureVal[Exception, Job] = {
    getJobs map {
      jobs => jobs.filter(_.id === id).headOption
    } discard {
      case None => UnknownJob
    } map { _.get }
  }
  
  def save(): FutureVal[Exception, Unit] = User.save(this)

  def toValueObject: UserVO = UserVO(id, name, email, password, organizationId)
}
