package notifiers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import javax.inject.Inject;

import config.ConfigBean;

import play.jobs.Job;
import play.mvc.Mailer;
import service.ServiceLayer;

public class Mails extends Mailer {
  
  @Inject
  static ConfigBean configBean;

  @Inject
  static ServiceLayer serviceLayer;
  
  public static void lostPassword(String email, String link) {
    setFrom("noreply <noreply@tutorcast.com>");
    setSubject("Your password reset link");
    addRecipient(email);
    send(link);
 }
  
  public static void inviteTeacher(String email, Long classId) {
    Map classMap = serviceLayer.getDao().getClassByID(classId);
    Long teacherId = (Long)classMap.get("teacher_id");
    Map teacherMap = serviceLayer.getDao().getUserByID(teacherId);
    String link = configBean.getDeployedUrl()+"/classLanding/"+classId;
    Long startTime = (Long)classMap.get("start_time");  
    Date start = new Date(startTime*1000);
    String timezone = (String)classMap.get("timezone");
    SimpleDateFormat dtFormat = new SimpleDateFormat("hh:mm aa MM-dd-yyyy");
    
    dtFormat.setTimeZone(TimeZone.getTimeZone(timezone));
    String formattedTime = dtFormat.format(start) +" "+timezone;
    String className = (String)classMap.get("class_name");
    String firstname = (String)teacherMap.get("fname");
    String lastname =(String)teacherMap.get("lname");
    String fullname = firstname+" "+lastname;
    
    String dashboardLink = configBean.getDeployedUrl()+"/dashboard";
    setFrom("noreply <noreply@tutorcast.com>");
    setSubject("Your class was scheduled successfully");
    addRecipient(email);
    send(link, formattedTime, fullname, className, dashboardLink);
  }
  
  public static void inviteRegisteredUser(String email, Long classId) {
    Map classMap = serviceLayer.getDao().getClassByID(classId);
    Long teacherId = (Long)classMap.get("teacher_id");
    Map teacherMap = serviceLayer.getDao().getUserByID(teacherId);
    String link = configBean.getDeployedUrl()+"/classLanding/"+classId;
    Long startTime = (Long)classMap.get("start_time");  
    Date start = new Date(startTime*1000);
    String timezone = (String)classMap.get("timezone");
    SimpleDateFormat dtFormat = new SimpleDateFormat("hh:mm aa MM-dd-yyyy");
    dtFormat.setTimeZone(TimeZone.getTimeZone(timezone));
    
    String formattedTime = dtFormat.format(start) +" "+timezone;
    String className = (String)classMap.get("class_name");
    String firstname = (String)teacherMap.get("fname");
    String lastname =(String)teacherMap.get("lname");
    String fullname = firstname+" "+lastname;
    
    setFrom("noreply <noreply@tutorcast.com>");
    
    setSubject(fullname+" has invited you to a class on Tutorcast");
    addRecipient(email);
    send(link, formattedTime, firstname, className);
  }
  
  public static void inviteUnregistered(String email, Long classId) {
    Map classMap = serviceLayer.getDao().getClassByID(classId);
    Long teacherId = (Long)classMap.get("teacher_id");
    Map teacherMap = serviceLayer.getDao().getUserByID(teacherId);
    String link = configBean.getDeployedUrl()+"/classLanding/"+classId;
    Long startTime = (Long)classMap.get("start_time");  
    Date start = new Date(startTime*1000);
    String timezone = (String)classMap.get("timezone");
    SimpleDateFormat dtFormat = new SimpleDateFormat("hh:mm aa MM-dd-yyyy");
    dtFormat.setTimeZone(TimeZone.getTimeZone(timezone));
    String formattedTime = dtFormat.format(start) +" "+timezone;
    String className = (String)classMap.get("class_name");
    String firstname = (String)teacherMap.get("fname");
    String lastname =(String)teacherMap.get("lname");
    String fullname = firstname+" "+lastname;
    
    setFrom("noreply <noreply@tutorcast.com>");
    setSubject(fullname+" has invited you to a class on Tutorcast");
    addRecipient(email);
    send(link, formattedTime, fullname, className);
    
  }
  
  public static void sendReceipt(String email, Long classId) {
    Map classMap = serviceLayer.getDao().getClassByID(classId);
    Long teacherId = (Long)classMap.get("teacher_id");
    Map teacherMap = serviceLayer.getDao().getUserByID(teacherId);

    String cost = classMap.get("cost")+"";
    String className = (String)classMap.get("class_name");
    String firstname = (String)teacherMap.get("fname");
    String lastname =(String)teacherMap.get("lname");
    String fullname = firstname+" "+lastname;
    
    setFrom("noreply <noreply@tutorcast.com>");
    setSubject("Receipt for your Tutorcast session");
    addRecipient(email);
    send(fullname, className, cost);
    
  }
}
