package controllers;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import notifiers.Mails;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.types.User;

import config.ConfigBean;
import play.Logger;
import play.jobs.Job;
import play.libs.Crypto;
import play.libs.OAuth2;
import play.libs.Time;
import play.libs.OAuth2.Response;
import play.mvc.Controller;
import security.BCrypt;
import security.ParamOAuth2;
import service.ServiceLayer;

public class UserManagement extends Controller {
  @Inject
  static ConfigBean configBean;

  @Inject
  static ServiceLayer serviceLayer;

  private interface AuthVisitor {
    boolean visit(OAuth2.Response response);
  }

  public static void facebookRegister(final Long classId) {
    ParamOAuth2 FACEBOOK =
        new ParamOAuth2("https://graph.facebook.com/oauth/authorize",
            "https://graph.facebook.com/oauth/access_token", configBean.getFbAppID(),
            configBean.getFbAppSecret());
    if (OAuth2.isCodeResponse()) {
      OAuth2.Response response = FACEBOOK.retrieveAccessToken(facebookRegisterURL());
      String token = response.accessToken;
      FacebookClient client = new DefaultFacebookClient(token);
      
      final User fbUser = client.fetchObject("me", User.class);

      final Long facebookID = Long.parseLong(fbUser.getId());
      
      Map existingUser = await(new Job() {
        @Override
        public Object doJobWithResult() {
          try {
            Map result = serviceLayer.getDao().getUserByFacebookID(facebookID);
            return result;
          } catch (Exception e) {
            return null;
          }
        }
      }.now());
      
      
      if (existingUser != null) {
        String username = (String)existingUser.get("username");
        if (username != null && !username.isEmpty()){
          session.put("username", existingUser.get("username"));
          Secured.browseVideo(null);
        } 
      }
      
      final Long userId = await(new Job() {
        @Override
        public Object doJobWithResult() {
          try {
            Long id = serviceLayer.insertFacebookUser(fbUser);
            return id;
          } catch (Exception e) {
            return null;
          }
        }
      }.now());
      if (userId == null) {
        Logger.warn("Error loading prefilled facebook user. %s" + fbUser.getEmail());
        userRegistrationPage(null);
      } else {
        session.put("userId", userId + "");
        userRegistrationPage(null);
      }
    } else {
      if (classId != null) {
        session.put("desiredClassId", classId+"");
      }
      FACEBOOK.retrieveVerificationCode(facebookRegisterURL(),
          "scope=email,user_education_history,user_location");
    }
  }

  public static void userRegistrationPage(final Long classId) {
    if (classId != null) {
      session.put("desiredClassId", classId+"");
    }
    final String userIDStr = session.get("userId");
    String photoLarge = "http://testdoc.tenka.com/0824883c-2ce3-4493-a5b8-a79d54d9096a.jpg";
    String photoMedium = "http://testdoc.tenka.com/94b17722-4d5b-4dcc-8dc6-a8bfbe857945.jpg";
    String photoSmall = "http://testdoc.tenka.com/17c5553f-8711-492d-9063-bf259a28bab6.jpg";
    String photoSquare = "http://testdoc.tenka.com/09a1fb50-3bf8-45de-9f5f-5a74037907d1.jpg";
    renderArgs.put("photoUrlLarge", photoLarge);
    renderArgs.put("photoUrlMedium", photoMedium);
    renderArgs.put("photoUrlSmall", photoSmall);
    renderArgs.put("photoUrlSquare", photoSquare);
    if (userIDStr != null && !userIDStr.isEmpty()) {
      final Long userId = Long.parseLong(userIDStr);
      Map userMap = await(new Job() {
        @Override
        public Object doJobWithResult() {
          try {
            Map result = serviceLayer.getDao().getUserByID(userId);
            return result;
          } catch (Exception e) {
            return null;
          }

        }
      }.now());
      
      if (userMap == null) {
        Logger.warn("User registration could not load prefilled user. %s", userId + "");
      } else {
        
        if (userMap.get("photo_url_large")!=null && !((String)userMap.get("photo_url_large")).isEmpty()) {
          photoLarge = (String)userMap.get("photo_url_large");
        }
        if (userMap.get("photo_url_medium")!=null && !((String)userMap.get("photo_url_medium")).isEmpty()) {
          photoMedium = (String)userMap.get("photo_url_medium");
        }
        if (userMap.get("photo_url_small")!=null && !((String)userMap.get("photo_url_small")).isEmpty()) {
          photoSmall = (String)userMap.get("photo_url_small");
        }
        if (userMap.get("photo_url_square")!=null && !((String)userMap.get("photo_url_square")).isEmpty()) {
          photoSquare = (String)userMap.get("photo_url_square");
        }
        
        renderArgs.put("firstName", userMap.get("fname"));
        renderArgs.put("lastName", userMap.get("lname"));
        renderArgs.put("userId", userMap.get("id"));
        renderArgs.put("userName", userMap.get("username"));
        renderArgs.put("userEmail", userMap.get("canonnical_email"));
        renderArgs.put("photoUrlLarge", photoLarge);
        renderArgs.put("photoUrlMedium", photoMedium);
        renderArgs.put("photoUrlSmall", photoSmall);
        renderArgs.put("photoUrlSquare", photoSquare);
        renderArgs.put("classId", classId);
      }
    }
    render();
  }

  public static void teacherRegistrationPage() {
    final String userIDStr = session.get("userId");
    if (userIDStr != null && !userIDStr.isEmpty()) {
      final Long userId = Long.parseLong(userIDStr);
      session.put("userId", userId + "");
      Map userMap = await(new Job() {
        @Override
        public Object doJobWithResult() {
          try {
            Map result = serviceLayer.getDao().getUserByID(userId);
            return result;
          } catch (Exception e) {
            return null;
          }

        }
      }.now());
      if (userMap == null) {
        Logger.warn("Teacher registration could not load prefilled user. %s", userId + "");
      } else {
        session.put("username", userMap.get("username"));
        renderArgs.put("userId", userMap.get("id"));
        renderArgs.put("firstName", userMap.get("fname"));
        renderArgs.put("lastName", userMap.get("lname"));
        renderArgs.put("location", userMap.get("location"));
      }
    }
    render();
  }

  public static void registerTeacher(final String firstName, final String lastName,
      final Long userId, final String location, final String degree, final String school) {
    validation.clear();
    validation.required(firstName);
    validation.required(lastName);
    validation.required(location);
    validation.required(school);
    validation.required(degree);
    validation.required(userId);
    //System.err.println("degree:" + school);
    if (validation.hasErrors()) {
      params.flash();
      validation.keep();
      teacherRegistrationPage();
    }

    Long savedId = await(new Job() {
      @Override
      public Object doJobWithResult() {
        Long id =
            serviceLayer.setInstructorInfo(firstName, lastName, userId, location, degree, school);
        return id;
      }
    }.now());
    Secured.browseVideo(null);
  }

  public static void skipTeacherInfo() {
    Secured.browseVideo(null);
  }


  public static void registerUser(final Long userId, final String userEmail, final String userName,
      final String password, final String photoUrlLarge, final String photoUrlMedium,
      final String photoUrlSmall, final String photoUrlSquare, final String firstName,
      final String lastName) {
    validation.clear();
    validation.required(userEmail);
    validation.required(userName);
    validation.required(password);
    validation.minSize(password, 6);
    validation.email(userEmail);

    List users = await(new Job() {
      @Override
      public Object doJobWithResult() {
        try {
          List result = serviceLayer.getDao().getAllUsersIdsWithUsername(userName);
          return result;
        } catch (Exception e) {
          return null;
        }

      }
    }.now());
    int maxcount = 0;
    
    if (userId != null){
      maxcount = 1;
    }

    if (users != null && users.size() > maxcount) {
      validation.addError("userName", "Your username already exists please pick a different one");
    }
    if (validation.hasErrors()) {
      //params.flash();
      renderArgs.put("userId", userId);
      renderArgs.put("userName", userName);
      renderArgs.put("userEmail", userEmail);
      renderArgs.put("photoUrlLarge", photoUrlLarge);
      renderArgs.put("photoUrlMedium", photoUrlMedium);
      renderArgs.put("photoUrlSmall", photoUrlSmall);
      renderArgs.put("photoUrlSquare", photoUrlSquare);
      validation.keep();
      userRegistrationPage(null);
    }

    Long savedId = await(new Job() {
      @Override
      public Object doJobWithResult() {
        Long id =
            serviceLayer.registerUser(userId, userEmail, userName, password, photoUrlLarge,
                photoUrlMedium, photoUrlSmall, photoUrlSquare, firstName, lastName);
        return id;
      }
    }.now());
    if (savedId == null) {
      validation
          .addError("userName",
              "A temporary technical issue occurred while saving your info. Please try submitting again");
      params.flash();
      validation.keep();
      userRegistrationPage(null);
    } else {
      session.put("username", userName);
      teacherRegistrationPage();
    }
  }

  public static void recoverPasswordPage() {
    render();
  }
  
  public static void resetPasswordRequest(final String email) {
   // System.err.println("EMAIL IS:"+email);
    Map user = await(new Job() {
      @Override
      public Object doJobWithResult() {
        Map result = serviceLayer.getDao().getUserByEmail(email);
        return result;
      }
    }.now());
    if (user != null){
      final Long userID = (Long)user.get("id");
      final String rid = UUID.randomUUID().toString();
      await(new Job() {
        @Override
        public void doJob() {
          serviceLayer.getDao().setUserRecoveryID(rid, userID);
        }
      }.now());
      String link = configBean.getDeployedUrl() +"/pwReset?id="+rid;
      Mails.lostPassword(email, link);
    }
    render();
  }
  
  public static void resetPasswordPage(final String id) {
    Map user = await(new Job() {
      @Override
      public Object doJobWithResult() {
        Map result = serviceLayer.getDao().getUserByRecoveryId(id);
        return result;
      }
    }.now());
//    if (user == null){
//      notFound();
//    }
//    renderArgs.put("userId",user.get("id"));
//    renderArgs.put("recoverId", id);
    render();
  }
  
  public static void resetPasswordComplete(final Long userId, final String password, final String repassword, final String recoverId) {
    if (userId == null){
      error();
    }
    validation.required(password);
    validation.required(repassword);
    validation.minSize(password, 6);
    
    if (!password.trim().equals(repassword)) {
      validation.addError("password", "Your passwords do not match");
    }
    
    if (validation.hasErrors()) {
      params.flash();
      validation.keep();
      resetPasswordPage(recoverId);
    }
    Map user = await(new Job() {
      @Override
      public Object doJobWithResult() {
        Map result = serviceLayer.getDao().getUserByID(userId);
        return result;
      }
    }.now());
    if (user == null){
      notFound();
    }
    String pwHash = BCrypt.hashpw(password, BCrypt.gensalt());
    user.put("password", pwHash);
    user.put("recovery_id", "");
    await(new Job() {
      @Override
      public void doJob() {
        serviceLayer.getDao().setUserRecoveryID(null, userId);
      }
    }.now());
    render();
  }

  
  public static String facebookRegisterURL() {
    return play.mvc.Router.getFullUrl("UserManagement.facebookRegister");
  }
}
