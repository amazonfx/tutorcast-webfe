package controllers;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import notifiers.Mails;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import config.ConfigBean;
import play.Logger;
import play.jobs.Job;
import play.libs.OpenID.UserInfo;
import play.mvc.Controller;
import play.mvc.With;
import service.ServiceLayer;


@With(SecurityController.class)
public class Secured extends Controller {
  @Inject
  static ConfigBean configBean;

  @Inject
  static ServiceLayer serviceLayer;

  public static void browseVideo(String className) {
    final String username = session.get("username");
    final Map user = await(new Job() {
      @Override
      public Object doJobWithResult() {
        try {
          Map result = serviceLayer.getDao().getUserByUsername(username);
          return result;
        } catch (Exception e) {
          return null;
        }
      }
    }.now());
    if (user == null) {
      error();
    }
    
    String desiredClass = session.get("desiredClassId");
    if (desiredClass != null) {
      
      final Long dcId = Long.parseLong(desiredClass);
      final Map classMap = await(new Job() {
        @Override
        public Object doJobWithResult() {
          try {
            Map result = serviceLayer.getDao().getClassByID(dcId);
            return result;
          } catch (Exception e) {
            return null;
          }
        }
      }.now());
      
      final Map teacher = await(new Job() {
        @Override
        public Object doJobWithResult() {
          try {
            Map result = serviceLayer.getDao().getUserByID((Long)classMap.get("teacher_id"));
            return result;
          } catch (Exception e) {
            return null;
          }
        }
      }.now());
      
      await(new Job() {
        @Override
        public void doJob() {
          try {
            Float cost = (Float)classMap.get("cost");
            Boolean paid = false;
            if (cost <= 0){
              paid = true;
            }
            String email = serviceLayer.getUserEmail(user);
            HashMap cu = new HashMap<>();
            cu.put("class_id", dcId);
            cu.put("user_id", user.get("id"));
            cu.put("paid", paid);
            cu.put("cost", cost);
            cu.put("is_teacher", false);
            cu.put("user_email", email);
            
            serviceLayer.getDao().insertObj("ClassUser", cu);
            
            serviceLayer.getDao().deletePendingClassUser(email);
          } catch (Exception e) {
          }
        }
      }.now());
      Float cost = (Float)classMap.get("cost");
      Boolean paid = false;
      if (cost <= 0){
        paid = true;
      }
      String cname = (String) classMap.get("class_name");
      renderArgs.put("classRegistered", cname);
      renderArgs.put("classCost", cost);
      renderArgs.put("classPaid", paid);
      renderArgs.put("classId", classMap.get("id"));
      renderArgs.put("teacherName", teacher.get("fname")+" "+teacher.get("lname"));
      renderArgs.put("classPhotoUrl", classMap.get("photo_url_medium"));
      
      Long startTime = serviceLayer.getJavaTime((Long)classMap.get("start_time"));
      Long now = new Date().getTime();
      Long endTime = now + (60*80*1000);
      Boolean isEnded = (Boolean)classMap.get("is_ended");
      System.err.println("startTime:"+startTime);
      System.err.println("isENDED:"+isEnded);
      if (!isEnded && now >=startTime && now <=endTime){
        renderArgs.put("classInProgress", true);
        System.err.println("CLASS IN PROGRESS true");
      } else {
        renderArgs.put("classInProgress", false);
        System.err.println("CLASS IN PROGRESS False");
      }
      session.remove("desiredClassId");
    }
        
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd-yyyy");
    String today = simpleDateFormat.format(new Date());

    String photoUrlLarge = (String) user.get("photo_url_large");
    String photoUrlMedium = (String) user.get("photo_url_medium");
    String photoUrlSmall = (String) user.get("photo_url_small");
    String photoUrlSquare = (String) user.get("photo_url_square");
    if (className != null){
      renderArgs.put("classCreated", className);
    }
    renderArgs.put("photoUrlLarge", photoUrlLarge);
    renderArgs.put("photoUrlMedium", photoUrlMedium);
    renderArgs.put("photoUrlSmall", photoUrlSmall);
    renderArgs.put("photoUrlSquare", photoUrlSquare);
    renderArgs.put("today", today);
    
    render();
  }

  public static void createClass(final String className, final String charge, final Float cost,
      final String when, final String dateField, final String timeField, final String timezone,
      final List<String> who, final String photoUrlLarge, final String photoUrlMedium,
      final String photoUrlSmall, final String photoUrlSquare, final Boolean isPublic,
      final String desc, final String tags) {
    final String username = session.get("username");
    Map user = await(new Job() {
      @Override
      public Object doJobWithResult() {
        try {
          Map result = serviceLayer.getDao().getUserByUsername(username);
          return result;
        } catch (Exception e) {
          return null;
        }
      }
    }.now());

    if (user == null) {
      error();
    }
    final Long teacherID = (Long) user.get("id");
    Long classId = await(new Job() {
      @Override
      public Object doJobWithResult() {
        Long id =
            serviceLayer.createClass(className, charge, cost, when, dateField, timeField, timezone,
                who, photoUrlLarge, photoUrlMedium, photoUrlSmall, photoUrlSquare, isPublic, desc,
                tags, teacherID);
        return id;
      }
    }.now());
    if (classId == null) {
      error();
    }
    
    if (when.trim().equalsIgnoreCase("now")){
      redirect("/classroom/live/"+classId);
    } else {
      renderArgs.put("classCreated", className);
      browseVideo(className);
    } 
  }
  public static void setClassTags(final String tags, final Long classId) {

    List classTags = await(new Job() {
      @Override
      public Object doJobWithResult() throws Exception {
        serviceLayer.getDao().deleteClassTags(classId);
        String [] tagList = tags.split(",");
        for (int i = 0; i< tagList.length; ++i){
          serviceLayer.addClassTag(classId, tagList[i]);
        }
        List tags = serviceLayer.getDao().getTagsForClass(classId);
        return tags;
      }
    }.now());
    renderJSON(classTags);
  }
  
  public static void dashboard() {
    final String username = session.get("username");
    
    Map userMap = await(new Job() {
      @Override
      public Object doJobWithResult() throws Exception {
        Map userResult = serviceLayer.getDao().getUserByUsername(username);
        return userResult;
      }
    }.now());
    
    final Long userId = (Long)userMap.get("id");
    String degree = await(new Job() {
      @Override
      public Object doJobWithResult() throws Exception {
        String result = serviceLayer.getDegree(userId);
        return result;
      }
    }.now());
    renderArgs.put("degree", degree);
    renderArgs.put("userId", userId);
    renderArgs.put("userName", userMap.get("username"));
    renderArgs.put("aboutMe", userMap.get("about_me"));
    renderArgs.put("userFullName", userMap.get("fname") +" "+userMap.get("lname"));
    renderArgs.put("profilePic", userMap.get("photo_url_large"));
    renderArgs.put("userEmail", serviceLayer.getUserEmail(userMap));
    renderArgs.put("phone", userMap.get("phone"));
    renderArgs.put("location", userMap.get("location"));
    renderArgs.put("title", userMap.get("title"));
    render();
  }
  
  public static void getUserCredential(final Long userId) {

    List credentials = await(new Job() {
      @Override
      public Object doJobWithResult() throws Exception {
        List result = serviceLayer.getDao().getCredentialForUser(userId);
        return result;
      }
    }.now());
    renderJSON(credentials);
  }
  
  public static void getClassesForUser(final Long userId) {

    List classes = await(new Job() {
      @Override
      public Object doJobWithResult() throws Exception {
        List result = serviceLayer.getDao().getClassByUserID(userId);
        return result;
      }
    }.now());
    renderJSON(classes);
  }
  
  public static void getMaterialsForClass(final Long classId) {

    List materials = await(new Job() {
      @Override
      public Object doJobWithResult() throws Exception {
        List result = serviceLayer.getDao().getMaterialForClass(classId);
        return result;
      }
    }.now());
    renderJSON(materials);
  }
  
  public static void classroom(final String mode, final Long classId) {
    final String username = session.get("username");
    
    Map userMap = await(new Job() {
      @Override
      public Object doJobWithResult() throws Exception {
        Map userResult = serviceLayer.getDao().getUserByUsername(username);
        return userResult;
      }
    }.now());
    
    if (userMap == null) {
      Logger.warn("classroom enpoint returning notfound because user could not be found");
      notFound();
      return;
    }
    final Long userId = (Long)userMap.get("id");
    Map classMap = await(new Job() {
      @Override
      public Object doJobWithResult() throws Exception {
        Map classResult = serviceLayer.getClassIfAttending(classId, userId, mode);
        return classResult;
      }
    }.now());
    if (classMap == null) {
      Logger.warn("classroom enpoint returning notfound because user classId combination could not be found");
      notFound();
      return;
    } else {
      final String sessionId = await(new Job() {
        @Override
        public Object doJobWithResult() throws Exception {
          String sId = serviceLayer.getTokSessionId(classId);
          return sId;
        }
      }.now());
      
      if (sessionId == null) {
        Logger.warn("classroom enpoint returning error because tokbox session not be obtained for classID:"+classId);
        error("Video Initialization Error");
        return;
      }

      String tokToken = await(new Job() {
        @Override
        public Object doJobWithResult() {
          String token = serviceLayer.getTokToken(sessionId, userId);
          return token;
        }
      }.now());
          
      if (tokToken == null) {
        Logger.warn("classroom enpoint returning error because tokbox token not be obtained for classID: %s, userID, %s", classId+"", userId+"");
        error("Video Initialization Error");
        return;
      }
      
      List<Map> materialList = await(new Job() {
        @Override
        public List doJobWithResult() {
          try {
            List materials = serviceLayer.getDao().getMaterialForClass(classId);
            if (materials == null) {
              return new ArrayList();
            } else return materials;
          } catch (Exception e) {
            Logger.error(e, "Exception encountered while getting class material. classID: %s, userID, %s", classId+"", userId+"");
            return new ArrayList();
          }
        }
      }.now());
      JsonArray materialArray = new JsonArray();
      
      for (Map v: materialList){
        JsonObject o = new JsonObject();
        o.addProperty("name",(String)v.get("material_name"));
        o.addProperty("link", (String)v.get("link"));
        materialArray.add(o);
      }
      
      List<Map> pendingUserList = await(new Job() {
        @Override
        public List doJobWithResult() {
          try {
            List pending = serviceLayer.getDao().getAllPendingUsersForClass(classId);
            if (pending  == null) {
              return new ArrayList();
            } else return pending ;
          } catch (Exception e) {
            Logger.error(e, "Exception encountered while getting pending users. classID: "+classId);
            return new ArrayList();
          }
        }
      }.now());
      List<Map> registeredUserList = await(new Job() {
        @Override
        public List doJobWithResult() {
          try {
            List registered = serviceLayer.getDao().getAllUsersForClass(classId);
            if (registered  == null) {
              return new ArrayList();
            } else return registered ;
          } catch (Exception e) {
            Logger.error(e, "Exception encountered while getting registered users. classID: "+classId);
            return new ArrayList();
          }
        }
      }.now());
      
      JsonArray pendingUserArray = new JsonArray();
      
      for (Map v: pendingUserList){
        JsonObject o = new JsonObject();
        o.addProperty("email",(String)v.get("email"));
        o.addProperty("paid", (Boolean)v.get("paid"));
        o.addProperty("userId", (Long)v.get("user_id"));
        pendingUserArray.add(o);
      }
      
      String email = serviceLayer.getUserEmail(userMap);
      Boolean isReplay = mode.trim().equalsIgnoreCase("replay");
      Long teacherId = (Long)classMap.get("teacher_id");
      
      Boolean paid = true;
      JsonArray registeredUserArray = new JsonArray();
      for (Map v: registeredUserList){
        JsonObject o = new JsonObject();
        o.addProperty("email",(String)v.get("user_email"));
        o.addProperty("paid", (Boolean)v.get("paid"));
        Long uid = (Long)v.get("user_id");
        if (uid.equals(userId)) {
          Boolean paidRaw = (Boolean)v.get("paid");
          Float cost = (Float)v.get("cost");
          if (cost <=0){
            paid = true;
          } else {
            paid = paidRaw;
          }
          

        }
        o.addProperty("userId",uid );
        registeredUserArray.add(o);
      }
      
      if ((isReplay!=null && isReplay)) {
        paid = true;
      }

      renderArgs.put("isTeacher", teacherId.equals(userId));
      renderArgs.put("stripeKey", configBean.getStripePublicKey());
      renderArgs.put("roomId", classMap.get("id"));
      renderArgs.put("videoOffset", classMap.get("video_offset"));
      renderArgs.put("paid", paid);
      renderArgs.put("userId", userId);
      renderArgs.put("firstName", userMap.get("fname"));
      renderArgs.put("profilePic", userMap.get("photo_url_square"));
      renderArgs.put("isReplay", isReplay);
      renderArgs.put("orbitHost", serviceLayer.getOrbitHost());
      renderArgs.put("orbitPort", serviceLayer.getOrbitPort());
      renderArgs.put("roomName", classMap.get("class_name"));
      renderArgs.put("userEmail", email);
      renderArgs.put("cost", classMap.get("cost"));
      renderArgs.put("teacherID", teacherId);
      renderArgs.put("tokSession", sessionId);
      renderArgs.put("tokToken", tokToken);
      renderArgs.put("archiveId", classMap.get("archive_id"));
      renderArgs.put("materialList", materialArray.toString());
      renderArgs.put("registeredUserList", registeredUserArray.toString());
      renderArgs.put("pendingUserList", pendingUserArray.toString());
      render();
    }
  }
  
  public static void inviteToClass (List<String> who, final Long classId, final String teacherEmail){
    final String username = session.get("username");
    Map user = await(new Job() {
      @Override
      public Object doJobWithResult() {
        try {
          Map result = serviceLayer.getDao().getUserByUsername(username);
          return result;
        } catch (Exception e) {
          return null;
        }
      }
    }.now());

    if (user == null) {
      error();
    }

    final HashSet<String> whoSet = new HashSet<>();
    for (String w:who){
      whoSet.add(w);
    }
    
    Boolean status = await(new Job() {
      @Override
      public Object doJobWithResult() {
          Boolean status = serviceLayer.inviteToClass(whoSet, teacherEmail, classId);
          return status;
      }   
    }.now());
    
    if (!status){
      error();
    }
    renderJSON(whoSet);
  }
  
  public static void getRecentClasses(final Integer pageSize, final Integer pageNumber){
    final List<Map> result = await(new Job() {
      @Override
      public Object doJobWithResult() {
          List classes = serviceLayer.getDao().getRecentClasses(pageSize, pageNumber);
          return classes;
      }   
    }.now());
    Map tags = await(new Job() {
      @Override
      public Object doJobWithResult() {
        HashMap tagList = new HashMap<>();
        for (Map r:result) {
          try {
            HashMap t = new HashMap<>();
            Long classId = (Long)r.get("classId");
            List tags = serviceLayer.getDao().getTagsForClass(classId);
            tagList.put((Long)r.get("classId"), tags);
          } catch (Exception e) {
              e.printStackTrace();
              
          }
        }
        return tagList;
      }   
    }.now());
    
    Map max = await(new Job() {
      @Override
      public Object doJobWithResult() {
        Map max = serviceLayer.getDao().getMaxClassId();
        return max;
      }   
    }.now());
    Long maxId = (Long)max.get("max");
    Long totalPage = maxId / pageSize + 1;
    if (totalPage > 6){
      totalPage = 6L;
    }
    HashMap resList = new HashMap<>();
    resList.put("classes", result);
    resList.put("tags", tags);
    resList.put("totalPages", totalPage);
    resList.put("currentPage", pageNumber);
    renderJSON(resList);
  }
  
  public static void getTaughtClasses(final Integer pageSize, final Integer pageNumber, final Long userId){
    final List<Map> result = await(new Job() {
      @Override
      public Object doJobWithResult() {
          List classes = serviceLayer.getDao().getTaughtClasses( pageSize, pageNumber, userId);
          return classes;
      }   
    }.now());
    
    
    Map max = await(new Job() {
      @Override
      public Object doJobWithResult() {
        Map max = serviceLayer.getDao().getMaxClassId();
        return max;
      }   
    }.now());
    Boolean ended = false;
    if (result.size() < pageSize) {
      ended = true;
    }
    HashMap resList = new HashMap<>();
    resList.put("classes", result);
    resList.put("ended", ended);
    resList.put("currentPage", pageNumber);
    renderJSON(resList);
  }
  
  public static void getAttendedClasses(final Integer pageSize, final Integer pageNumber, final Long userId){
    final List<Map> result = await(new Job() {
      @Override
      public Object doJobWithResult() {
          List classes = serviceLayer.getDao().getAttendedClasses( pageSize, pageNumber, userId);
          return classes;
      }   
    }.now());
    
    
    Boolean ended = false;
    if (result.size() < pageSize) {
      ended = true;
    }
    HashMap resList = new HashMap<>();
    resList.put("classes", result);
    resList.put("ended", ended);
    resList.put("currentPage", pageNumber);
    renderJSON(resList);
  }
  
  public static void getScheduledClasses( final Long userId){
    final List<Map> result = await(new Job() {
      @Override
      public Object doJobWithResult() {
          List classes = serviceLayer.getDao().getScheduledClasses(userId);
          return classes;
      }   
    }.now());
   // System.err.println("returned:"+result.size());
    HashMap resList = new HashMap<>();
    resList.put("events", result);
    renderJSON(resList);
  }
  
  public static void payJson (final Long cardnumber, final String cvc, final String expMonth, final String expYear, final Long classId){
    final String username = session.get("username");
    Map user = await(new Job() {
      @Override
      public Object doJobWithResult() {
        try {
          Map result = serviceLayer.getDao().getUserByUsername(username);
          return result;
        } catch (Exception e) {
          return null;
        }
      }
    }.now());
    final Long userId = (Long)user.get("id");
    Map classMap = await(new Job() {
      @Override
      public Object doJobWithResult() {
        try {
          Map result = serviceLayer.getDao().getClassByID(classId);
          return result;
        } catch (Exception e) {
          return null;
        }
      }
    }.now());
    final Float amount = (Float)classMap.get("cost");
    String status = await(new Job() {
      @Override
      public Object doJobWithResult() {
          String status = serviceLayer.processCard(new Float(amount*100).intValue(), cardnumber, cvc, expMonth, expYear);
          return status;
      }   
    }.now());
    await(new Job() {
      @Override
      public void doJob(){
        for (int i = 0; i < 4; ++i){
          Boolean status = serviceLayer.updatePaidClass(userId, classId);
          if (status){
            return;
          }
        }
      }   
    }.now());
    String email = (String)user.get("canonnical_email");
    if (email == null || email.isEmpty()){
      email = (String)user.get("user_email");
    }
    Mails.sendReceipt(email, classId);
    if (status.startsWith("success")){
      renderJSON("{\"success\":true,\"amount\":"+amount+" }");
    } else if (status!=null){
      renderJSON("{\"success\":false,\"reason\":\""+status+"\" }");
    } else {
      error();
    }
  }
}
