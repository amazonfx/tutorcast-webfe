package security;

import play.libs.OAuth2;
import play.libs.WS;
import play.mvc.results.Redirect;

//Default OAuth2 Helper does not support parameters, which is required for facebook permissions
public class ParamOAuth2 extends OAuth2 {
  public String authorizationURL;
  public String accessTokenURL;
  public String clientid;
  public String secret;
  public ParamOAuth2(String authorizationURL, String accessTokenURL, String clientid, String secret) {
    super(authorizationURL, accessTokenURL, clientid, secret);
    this.accessTokenURL = accessTokenURL;
    this.authorizationURL = authorizationURL;
    this.clientid = clientid;
    this.secret = secret;
  }

  public void retrieveVerificationCode(String callbackURL, String params) {
    StringBuilder url = new StringBuilder(authorizationURL);
    url.append("?client_id=").append(clientid).append("&redirect_uri=").append(callbackURL);
    url.append("&").append(params);
    throw new Redirect(url.toString());
  }


}
