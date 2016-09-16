# agent-access-control

[![Build Status](https://travis-ci.org/hmrc/agent-access-control.svg?branch=master)](https://travis-ci.org/hmrc/agent-access-control) [ ![Download](https://api.bintray.com/packages/hmrc/releases/agent-access-control/images/download.svg) ](https://bintray.com/hmrc/releases/agent-access-control/_latestVersion)

Delegated auth rules for [play-authorisation](https://github.com/hmrc/play-authorisation) library to allow access
to agents to their clients' data. Currently it supports Self-Assessment tax regime (IR-SA).

### Example usage for SA

###### Controller
```scala
package uk.gov.hmrc.test.controllers
object TestController extends BaseController {

  def handleGet(saUtr: String) = Action {
    Ok("you are in")
  }
}
```

###### app.routes
```
GET    /sa/:saUtr    uk.gov.hmrc.test.controllers.TestController.handleGet(saUtr)
```

Note that your endpoint must have the client's identifier, i.e., SA UTR, in the URL. This is a requirement
by `play-authorisation`, review the source/docs of `AuthorisationFilter` for more details about how it gets parsed.

###### application.conf
```
controllers {
  uk.gov.hmrc.test.controllers.TestController = {
    authParams {
      confidenceLevel = 50
      delegatedAuthRule = sa-auth
    }
  }
}
```

`play-authorisation` library will get the tax regime and the client identifier in that regime from the URL path using
a pattern. (default: `/([\w]+)/([^/]+)/?.*`, overridable with the _pattern_ conf key; tax regime is also overridable
with the _account_ conf key) In the example it is going to be sa and the sa utr. Auth service will check if the given
user is logged in, and if not and the logged-in user is an agent, it will delegate to this service because the
_delegatedAuthRule_ is set to `sa-auth`.

N.B.: all of the above is about how to use this with the current version of `play-authorisation`, 3.3.0.

### Endpoints

##### GET /agent-access-control/sa-auth/agent/:agentCode/client/:saUtr

Headers: need to contain a valid `Authorizastion` header.

Possible responses:

code | scenario
---- | ---
200 | _saUtr_ is allocated to _agentCode_ for Self-Assessment (GG check) AND the logged in user's agency has a valid authorisation to act on behalf of _saUtr_ for dealing with Self-Assessment (HODs check).
401 | The conditions above do not meet.
502 | In case of any error responses in downstream services.
504 | In case of a timeout while querying downstream services.

##### GET /ping/ping

Always `200` with empty body.

##### GET /admin/metrics

Displays metrics as JSON.

##### GET /admin/details

Displays `META-INF/MANIFEST.MF` as JSON.

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")