package controllers;

import java.util.Date;
import java.util.Map;

import javax.inject.Inject;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.types.User;

import config.ConfigBean;

import play.Logger;
import play.Play;
import play.data.validation.Required;
import play.jobs.Job;
import play.libs.Crypto;
import play.libs.OAuth2;
import play.libs.Time;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;
import security.BCrypt;
import security.ParamOAuth2;
import service.ServiceLayer;

public class SecurityController extends Controller {
  @Inject
  static ConfigBean configBean;

  @Inject
  static ServiceLayer serviceLayer;

  @Before(unless = {"login", "authenticate", "facebookAuthenticate","logout"})
  static void checkAccess() throws Throwable {
    if (!session.contains("username")) {
      flash.put("url", "GET".equals(request.method) ? request.url : Play.ctxPath + "/");
      login(null);
    }
  }

  public static void login(final Long classId) throws Throwable {
    if (classId != null) {
      session.put("desiredClassId", classId+"");
    }
    Http.Cookie remember = request.cookies.get("rememberme");
    if (remember != null) {
      int firstIndex = remember.value.indexOf("-");
      int lastIndex = remember.value.lastIndexOf("-");
      if (lastIndex > firstIndex) {
        String sign = remember.value.substring(0, firstIndex);
        String restOfCookie = remember.value.substring(firstIndex + 1);
        String username = remember.value.substring(firstIndex + 1, lastIndex);
        String time = remember.value.substring(lastIndex + 1);
        Date expirationDate = new Date(Long.parseLong(time));
        Date now = new Date();
        if (expirationDate == null || expirationDate.before(now)) {
          logout();
        }
        if (Crypto.sign(restOfCookie).equals(sign)) {
          session.put("username", username);
          redirectToOriginalURL();
        }
      }
    }
    flash.keep("url");
    renderArgs.put("classId", classId);
    render();
  }


  public static void facebookAuthenticate() throws Throwable {
    ParamOAuth2 FACEBOOK =
        new ParamOAuth2("https://graph.facebook.com/oauth/authorize",
            "https://graph.facebook.com/oauth/access_token", configBean.getFbAppID(),
            configBean.getFbAppSecret());
    if (OAuth2.isCodeResponse()) {
      try {
        OAuth2.Response fbResponse = FACEBOOK.retrieveAccessToken(facebookAuthenticateURL());
        String token = fbResponse.accessToken;
        FacebookClient client = new DefaultFacebookClient(token);
        final User fbUser = client.fetchObject("me", User.class);
        final Long facebookID = Long.parseLong(fbUser.getId());
        Boolean allowed = false;
        Map user = await(new Job() {
          @Override
          public Object doJobWithResult() {
            Map result = serviceLayer.getDao().getUserByFacebookID(facebookID);
            return result;
          }
        }.now());
        if (user != null) {
          allowed = true;
        }
        if (!allowed) {
          Unsecured.index(null);
        }
        String username = (String) user.get("username");
        session.put("username", username);
        Date expiration = new Date();
        String duration = "30d";
        expiration.setTime(expiration.getTime() + Time.parseDuration(duration));
        response.setCookie("rememberme", Crypto.sign(username + "-" + expiration.getTime()) + "-"
            + username + "-" + expiration.getTime(), duration);

        redirectToOriginalURL();
      } catch (Exception e) {
        Logger.error(e, "Exception encountered during facebook authentication");
        Unsecured.index(null);
      }
    } else {
      FACEBOOK.retrieveVerificationCode(facebookAuthenticateURL(),
          "scope=email,user_education_history,user_location");
    }
  }

  public static void authenticate(@Required final String username, String password, boolean remember)
      throws Throwable {
    // Check tokens
    Boolean allowed = false;
    Map user = await(new Job() {
      @Override
      public Object doJobWithResult() {
        Map result = serviceLayer.getDao().getUserByEmail(username);
        return result;
      }
    }.now());
    if (user != null) {
      String pwHash = (String) user.get("password");
      if (BCrypt.checkpw(password, pwHash)) {
        Long userId = (Long) (user.get("id"));
        session.put("userId", userId + "");
        allowed = true;
      }
    } 
    if (validation.hasErrors() || !allowed) {
      flash.keep("url");
      flash.error("Login failed check your username and password");
      params.flash();
      login(null);
    }
    session.put("username", user.get("username"));
    // Remember if needed
    if (remember) {
      Date expiration = new Date();
      String duration = "30d"; // maybe make this override-able
      expiration.setTime(expiration.getTime() + Time.parseDuration(duration));
      response.setCookie("rememberme", Crypto.sign(username + "-" + expiration.getTime()) + "-"
          + username + "-" + expiration.getTime(), duration);

    }
    redirectToOriginalURL();
  }


  public static void logout() throws Throwable {
    session.clear();
    response.removeCookie("rememberme");
    flash.success("secure.logout");
    Unsecured.index(null);
  }

  static void redirectToOriginalURL() throws Throwable {
    String url = flash.get("url");
    if (url == null) {
      url = Play.ctxPath + "/browseVideo";
    }
    redirect(url);
  }

  public static String facebookAuthenticateURL() {
    return play.mvc.Router.getFullUrl("SecurityController.facebookAuthenticate");
  }
}
