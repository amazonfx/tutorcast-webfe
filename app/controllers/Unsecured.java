package controllers;

import play.*;
import play.jobs.Job;
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.XML;
import play.libs.XPath;
import play.modules.spring.Spring;
import play.mvc.*;
import service.ServiceLayer;

import groovy.json.JsonBuilder;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.codec.binary.Base64;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyXmlSerializer;
import org.htmlcleaner.TagNode;
import org.jets3t.service.S3Service;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.acl.GroupGrantee;
import org.jets3t.service.acl.Permission;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.resource.FSEntityResolver;
import org.xhtmlrenderer.resource.XMLResource;
import org.xhtmlrenderer.swing.Java2DRenderer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;

import config.ConfigBean;

import dao.JDBCDao;


public class Unsecured extends Controller {

  @Inject
  static ConfigBean configBean;

  @Inject
  static ServiceLayer serviceLayer;

  public static void index(String desiredID) {
    
    render();
  }
  
  public static void test() {
    render();
  }
  
  public static void classLanding(final Long classId) {
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
    if (classMap == null){
      notFound();
    }
    final Long teacherID = (Long)classMap.get("teacher_id");
    Map teacher = await(new Job() {
      @Override
      public Object doJobWithResult() {
        try {
          Map result = serviceLayer.getDao().getUserByID(teacherID);
          return result;
        } catch (Exception e) {
          return null;
        }

      }
    }.now());
    
    List<Map> tagList= await(new Job() {
      @Override
      public Object doJobWithResult() {
        try {
          List result = serviceLayer.getDao().getTagsForClass(classId);
          return result;
        } catch (Exception e) {
          return null;
        }

      }
    }.now());
    String firstname = teacher.get("fname")+"";
    String lastname = teacher.get("lname") +"";
    String fullname = firstname + " " +lastname;
    
    Long startTime = (Long)classMap.get("start_time");
    Locale locale = play.i18n.Lang.getLocale();
    Calendar calendar = Calendar.getInstance(locale);  
    TimeZone timezone = calendar.getTimeZone();
    SimpleDateFormat dtFormat = new SimpleDateFormat("hh:mm aa z, MM/dd/yyyy ");
    
    dtFormat.setTimeZone(timezone);
    String formattedTime = dtFormat.format(startTime*1000);
    String description =(String)classMap.get("description");
    if (description == null || description.isEmpty()){
      description = "No description given";
    }
    List<String> tags = new ArrayList<>();
    for (Map t:tagList){
      tags.add((String)t.get("tag"));
    }
    renderArgs.put("classId", classMap.get("id"));
    renderArgs.put("className", classMap.get("class_name"));
    renderArgs.put("description", description);
    renderArgs.put("profilePic", teacher.get("photo_url_square"));
    renderArgs.put("classPhotoUrl", classMap.get("photo_url_large"));
    renderArgs.put("teacherName", fullname);
    renderArgs.put("tags", tags);
    renderArgs.put("startTime", formattedTime);
    render();
  }

  public static void downloadClassArchives(final Long classId) {
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
    final String archiveId = (String) classMap.get("archive_id");
    if (archiveId == null) {
      Logger.warn("downloadClassArchives endpoint returning error for classID:"+classId);
      error();
    }
    
    Boolean status = await(new Job() {
      @Override
      public Object doJobWithResult() {
        try {
          Boolean success = serviceLayer.downloadArchive(classId, archiveId);
          return success;
        } catch (Exception e) {
          Logger.error(e, "Exception occurred in download applicatin archive endpoint for classID: %s, archiveID: %s", classId+"", archiveId+"" );
          return false;
        }
      }
    }.now());
    
    if (status){
      renderJSON("{\"success\":true}");
    } else {
      renderJSON("{\"success\":false}");
    }
  }

  public static void setClassArchiveId(final String archiveId, final Long classId) {
    Boolean status = await(new Job() {
      @Override
      public Object doJobWithResult() {
        try {
          serviceLayer.getDao().setClassArchiveId(archiveId, classId);
          return true;
        } catch (Exception e) {
          Logger.error(e, "Exception occurred in setClassArchiveId endpoint for classID: %s, archiveID: %s", classId+"", archiveId+"" );
          return false;
        }

      }
    }.now());
    if (status) {
      ok();
    } else {
      Logger.warn("setClassArchiveId enpoint returning error have receiving status false");
      error();
    }
  }

  
  public static void setClassVideoOffset(final Long videoOffset, final Long classId) {
    Boolean status = await(new Job() {
      @Override
      public Object doJobWithResult() {
        try {
          serviceLayer.getDao().setClassVideoOffset(videoOffset, classId);
          return true;
        } catch (Exception e) {
          Logger.error(e, "Exception occurred in setClassVideoOffset endpoint for classID: %s, videoOffset: %l", classId+"", videoOffset+"" );
          return false;
        }

      }
    }.now());
    if (status) {
      ok();
    } else {
      Logger.warn("setClassArchiveId enpoint returning error have receiving status false");
      error();
    }
  }
  

  public static void saveTextPdf(final String doc, final String pdfLayer, final String pathLayer) {
    final String resultUrl = await(new Job() {
      @Override
      public Object doJobWithResult() throws Exception {
        File pdf = serviceLayer.mergeBoardContent(doc, pdfLayer, pathLayer);
        if (pdf == null) {
          return null;
        } else {
          Boolean status = serviceLayer.uploadToS3(pdf, pdf.getName(), "application/pdf");
          if (!status) {
            pdf.delete();
            return null;
          } else {
            pdf.delete();
            String url = "/s3/" + pdf.getName();
            return url;
          }
        }
      }
    }.now());

    if (resultUrl == null) {
      Logger.warn("saveTextPDF returning error because classID is null");
      renderJSON("{\"success\":false}");
    } else {
      renderJSON("{\"url\": \"" + resultUrl + "\", \"success\":true}");
    }
  }

  public static void getS3File(String fileName) {
    String url = configBean.getS3DocUrl() +"/"+ fileName;
    Promise<HttpResponse> s3Promise = WS.url(url).timeout("60s").getAsync();
    HttpResponse s3Response = await(s3Promise);
    if (s3Response.success()) {
      // response.contentType = s3Response.getContentType();
      response.setHeader("Content-Type", "application/force-download");
      response.setHeader("Content-Type", s3Response.getContentType());
      response.setHeader("Content-Type", "application/download");

      renderBinary(s3Response.getStream());
    } else {
      Logger.warn("Attempt to download S3 file failed for url:"+url);
      notFound("not found");
    }
  }
  
  public static void getClassEnded(final Long classId) {
    Map classMap = await(new Job() {
      @Override
      public Object doJobWithResult() {
        try {
          Map result = serviceLayer.getDao().getClassByID(classId);
          return result;
        } catch (Exception e) {
          Logger.error(e, "Exception occurred getting class end status for classID:"+classId);
          return false;
        }
      }
    }.now());
    if (classMap == null) {
      Logger.warn("get class ended returning error because classID is null");
      renderJSON("{\"success\":false}");
    } else {
      Boolean ended = (Boolean)classMap.get("is_ended");
      renderJSON("{\"success\":true,\"ended\":"+ended+" }");
    }
  }
  
  public static void setClassEnded(final Long classId) {
    Boolean status = await(new Job() {
      @Override
      public Object doJobWithResult(){
        try {
          serviceLayer.getDao().setClassEnded(classId);
          return true;
        } catch (Exception e) {
          Logger.error(e, "Exception occurred setting class end status for classID:"+classId);
          return false;
        }
      }
    }.now());
    if (!status){
      Logger.warn("setClassEnded endpoint returning error for classID:"+classId);
      error();
    } else {
      ok();
    }
  }
  
  public static void getClassArchive(final Long classId) {
    List<Map> archiveList = await(new Job() {
      @Override
      public Object doJobWithResult(){
        try {
          List result = serviceLayer.getDao().getClassArchiveByID(classId, configBean.getCloudfrontStreaming(), configBean.getS3DocUrl());
          return result;
        } catch (Exception e) {
          Logger.error(e, "Exception occurred while getting class archive for classID:"+classId);
          return null;
        }
      }
    }.now());
    if (archiveList == null){
      Logger.warn("getClassArchive endpoint returning empty JSON for classID:"+classId);
      renderJSON("{}");
    } else {
      renderJSON(archiveList);
    }
  }

  public static void uploadFileS3(final File file, final Long classID) {
    try {
      Boolean status = await(new Job() {
        @Override
        public Object doJobWithResult() throws Exception {
          Boolean fileStatus = serviceLayer.uploadToS3(file, file.getName(), "application/pdf");
          String link = "/s3/" + file.getName();
          serviceLayer.addClassMaterial(link, classID, file.getName());
          return fileStatus;
        }
      }.now());
     
     String name = file.getName();
     String link = "/s3/" + file.getName();
      if (status) {
        String url = "/s3/" + file.getName();
        renderJSON("{\"url\": \"" + url + "\", \"name\":\""+name+"\", \"success\":true}");
      } else {
        renderJSON("{\"success\":false}");
      }
    } catch (Exception e) {
      Logger.error(e, "Exception occurred while uploading file to S3");
      response.status = 400;
    }
  }
  
  
  public static void uploadImageS3(final File file) {
    if (file == null) {
      error();
      return;
    }
      Map status = await(new Job() {
        @Override
        public Object doJobWithResult(){
          File large = null;
          File medium = null;
          File small = null;
          File square = null;
          
          try {
            HashMap result = new HashMap();
            large = serviceLayer.resizeImage(file, 200, false);
            Boolean largeStatus = serviceLayer.uploadToS3(large, large.getName(), "image/jpeg");
            medium = serviceLayer.resizeImage(file, 100, false);
            Boolean mediumStatus = serviceLayer.uploadToS3(medium, medium.getName(), "image/jpeg");
            small = serviceLayer.resizeImage(file, 50, false);
            Boolean smallStatus = serviceLayer.uploadToS3(small, small.getName(), "image/jpeg");
            square = serviceLayer.resizeImage(file, 50, true);
            Boolean squareStatus = serviceLayer.uploadToS3(square, square.getName(), "image/jpeg");
            if (largeStatus){
              result.put("large", configBean.getS3DocUrl()+"/"+large.getName());
            }
            if (mediumStatus){
              result.put("medium", configBean.getS3DocUrl()+"/"+medium.getName());
            }
            if (smallStatus){
              result.put("small", configBean.getS3DocUrl()+"/"+small.getName());
            }
            if (squareStatus){
              result.put("square", configBean.getS3DocUrl()+"/"+square.getName());
            }  
            return result;
          } catch (Exception e) {
            Logger.error(e, "Exception occurred in controller while uploading images");
            return null;
          } finally {
            try {
              large.delete();
              medium.delete();
              small.delete();
              square.delete();
            } catch (Exception e) {
              Logger.error(e, "Exception occurred in controller while deleteing temp files in uploading images");
            }
          }
        }
      }.now());
     
      if (status!=null) {
        renderJSON(status);
      } else {
        error();
      }
    } 
}
