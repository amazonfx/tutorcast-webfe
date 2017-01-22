package service;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import notifiers.Mails;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.WordUtils;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyXmlSerializer;
import org.htmlcleaner.TagNode;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.ServiceException;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.acl.GroupGrantee;
import org.jets3t.service.acl.Permission;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xhtmlrenderer.resource.FSEntityResolver;
import org.xhtmlrenderer.swing.Java2DRenderer;

import play.Logger;
import play.libs.WS;
import play.libs.XML;
import play.libs.XPath;
import play.libs.F.Promise;
import play.libs.WS.HttpResponse;
import security.BCrypt;

import com.google.gson.JsonObject;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;
import com.opentok.api.API_Config;
import com.opentok.api.OpenTokSDK;
import com.opentok.api.constants.RoleConstants;
import com.opentok.exception.OpenTokException;
import com.restfb.types.NamedFacebookType;
import com.restfb.types.User.Education;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;

import config.ConfigBean;

import dao.DBHelper;
import dao.JDBCDao;

public class ServiceLayer {

  JDBCDao dao;
  ConfigBean config;

  public ConfigBean getConfig() {
    return config;
  }

  public void setConfig(ConfigBean config) {
    this.config = config;
  }

  public JDBCDao getDao() {
    return dao;
  }

  public void setDao(JDBCDao dao) {
    this.dao = dao;
  }

  public Boolean testValidPDF(File file, String extension) {
    if (!extension.trim().equalsIgnoreCase("PDF")) {
      return false;
    }
    try {
      InputStream in = new FileInputStream(file);
      PdfReader pdfReader = new PdfReader(in);
      return true;
    } catch (Exception e) {
      Logger.info(e, "Attempted to parse invalid pdf file");
      return false;
    }
  }

  public File mergeBoardContent(String doc, String pdfLayer, String pathLayer) {
    String resultName = UUID.randomUUID().toString() + ".pdf";
    String mergedImageName = UUID.randomUUID().toString();
    try {
      BufferedImage bgImage = null;
      BufferedImage pathImage = null;
      if (!pdfLayer.isEmpty()) {
        byte[] bgBytes = Base64.decodeBase64(pdfLayer.getBytes());
        bgImage = ImageIO.read(new ByteArrayInputStream(bgBytes));
      }
      if (!pathLayer.isEmpty()) {
        byte[] pathBytes = Base64.decodeBase64(pathLayer.getBytes());
        pathImage = ImageIO.read(new ByteArrayInputStream(pathBytes));
      }

      int resultHeight = (bgImage != null) ? bgImage.getHeight() : 1036;
      int resultWidth = (bgImage != null) ? bgImage.getWidth() : 796;


      CleanerProperties props = new CleanerProperties();

      props.setTranslateSpecialEntities(true);
      props.setTransResCharsToNCR(true);
      props.setOmitComments(true);

      TagNode tagNode = new HtmlCleaner(props).clean(doc);

      String result;
      result = new PrettyXmlSerializer(props).getAsString(tagNode);

      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(false);
      factory.setValidating(false);
      factory.setFeature("http://xml.org/sax/features/namespaces", false);
      factory.setFeature("http://xml.org/sax/features/validation", false);
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      DocumentBuilder builder = factory.newDocumentBuilder();
      builder.setEntityResolver(FSEntityResolver.instance());

      Document targetDoc = builder.parse(new ByteArrayInputStream(result.getBytes()));


      Java2DRenderer renderer = new Java2DRenderer(targetDoc, resultWidth, resultHeight) {
        @Override
        protected BufferedImage createBufferedImage(final int width, final int height) {
          final BufferedImage image =
              org.xhtmlrenderer.util.ImageUtil.createCompatibleBufferedImage(width, height,
                  BufferedImage.TYPE_INT_ARGB);
          org.xhtmlrenderer.util.ImageUtil.clearImage(image, new Color(255, 255, 255, 0));
          return image;
        }
      };


      renderer.setBufferedImageType(BufferedImage.TYPE_INT_ARGB);
      BufferedImage textImage = renderer.getImage();
      Graphics2D g = null;
      if (bgImage != null) {
        g = bgImage.createGraphics();
      } else {
        g = textImage.createGraphics();
      }
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      if (bgImage != null) {
        g.drawImage(bgImage, 0, 0, null);
      }

      g.drawImage(textImage, 0, 0, null);

      if (pathImage != null) {
        g.drawImage(pathImage, 0, 0, null);
      }

      g.dispose();

      BufferedImage mergedImage =
          new BufferedImage(bgImage.getWidth(), bgImage.getHeight(), BufferedImage.TYPE_INT_RGB);
      mergedImage.createGraphics().drawImage(bgImage, 0, 0, Color.WHITE, null);

      ImageIO.write(mergedImage, "jpg", new FileOutputStream(mergedImageName));

      com.itextpdf.text.Document backgroundPdf =
          new com.itextpdf.text.Document(PageSize.A4, 0, 0, 0, 0);
      PdfWriter.getInstance(backgroundPdf, new FileOutputStream(resultName));
      backgroundPdf.open();
      com.itextpdf.text.Image convertJpg = com.itextpdf.text.Image.getInstance(mergedImageName);
      convertJpg.scaleToFit(1036f, 796f);
      backgroundPdf.add(convertJpg);
      backgroundPdf.close();

      return new File(resultName);
    } catch (Exception e) {
      Logger.error(e, "Exception occurred while merging board content");
      return null;
    } finally {
      try {
        File imageFile = new File(mergedImageName);
        imageFile.delete();
      } catch (Exception e) {
        Logger.error(e, "Exception occurred removing temp file while merging board content");
      }
    }
  }

  public Boolean uploadToS3(File file, String fileName, String contentType) throws Exception {
    int mid = fileName.lastIndexOf(".");
    String ext = "";
    ext = fileName.substring(mid + 1, fileName.length());
    Boolean isValid = true;
    if (contentType.trim().equalsIgnoreCase("application/pdf")) {
      isValid = testValidPDF(file, ext);
    }
    if (!isValid) {
      return isValid;
    } else {
      AWSCredentials awsCredentials =
          new AWSCredentials(config.getAwsAccessKey(), config.getAwsSecretKey());
      S3Service s3Service = new RestS3Service(awsCredentials);
      AccessControlList bucketAcl = s3Service.getBucketAcl(config.getS3DocBucket());
      bucketAcl.grantPermission(GroupGrantee.ALL_USERS, Permission.PERMISSION_READ);
      S3Object fileObject = new S3Object(file);
      fileObject.setKey(fileName);
      fileObject.setContentLength(file.length());
      fileObject.setContentType(contentType);;
      fileObject.setAcl(bucketAcl);
      s3Service.putObject(config.getS3DocBucket(), fileObject);
      return true;
    }
  }

  public File resizeImage(File file, int maxWidth, boolean square) {
    int height = 0;
    int width = 0;
    InputStream fin = null;
    try {
      fin = new FileInputStream(file);
      BufferedImage originalImage = ImageIO.read(fin);
      if (square) {
        double aspect = (originalImage.getWidth() * 1.0) / (originalImage.getHeight() * 1.0);
        if (aspect < 1) {
          aspect = 1.0 / aspect;
        }
        width = (int) Math.round(53 * aspect);
      } else if (originalImage.getWidth() > maxWidth) {
        width = maxWidth;
      } else {
        width = originalImage.getWidth();
      }

      double ratio = (width * 1.0) / (originalImage.getWidth() * 1.0);
      height = (int) Math.round(originalImage.getHeight() * ratio);

      int imageType = BufferedImage.TYPE_INT_RGB;
      BufferedImage scaledBI = new BufferedImage(width, height, imageType);

      Graphics2D g = scaledBI.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
          RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g.drawImage(originalImage, 0, 0, width, height, null);
      g.dispose();

      if (square) {
        Rectangle goal = new Rectangle(50, 50);
        Rectangle clip =
            goal.intersection(new Rectangle(scaledBI.getWidth(), scaledBI.getHeight()));
        BufferedImage clippedImg = scaledBI.getSubimage(clip.x, clip.y, clip.width, clip.height);
        BufferedImage bi2 = new BufferedImage(goal.width, goal.height, imageType);
        Graphics2D big2 = bi2.createGraphics();
        big2.setColor(Color.white);
        big2.fillRect(0, 0, goal.width, goal.height);
        int x = goal.width - (clip.width / 2);
        int y = goal.height - (clip.height / 2);
        big2.drawImage(clippedImg, x, y, null);
        scaledBI = clippedImg;
      }
      String fname = UUID.randomUUID().toString();
      File result = new File(fname + ".jpg");
      ImageIO.write(scaledBI, "JPEG", result);
      return result;
    } catch (Exception e) {
      Logger.error(e, "Exception occurred while resizing image to size: %s", maxWidth + "");
      return null;
    } finally {
      try {
        fin.close();
      } catch (Exception ex) {
        Logger.error(ex, "Exception occurred close fileinputstream during resizing");
      }
    }
  }


  public Map getClassIfAttending(Long classId, Long userId, String mode) {
   // System.err.println("querying for class "+classId+"and user id:"+userId);
    Map classResult = dao.getClassByID(classId);
    if (classResult == null) {
      return null;
    }
    Boolean replay = mode.trim().equalsIgnoreCase("replay");
    Boolean endMarked = (Boolean) classResult.get("is_ended") && !replay;

    Boolean isPublic = (Boolean) classResult.get("is_public");
    Boolean replayPublic = (Boolean) classResult.get("recorded_public");

    Long cid = (Long) classResult.get("id");
    Map attending = dao.getClassUser(userId, cid);
    Boolean attendingClass = (attending!=null);
    
    if ((replay && !replayPublic) || (!replay && (isPublic!=null && !isPublic) && !attendingClass) || (!replay && endMarked)) {
      return null;
    } else {
      return classResult;
    }
  }

  public Long createClass(final String className, final String charge, final Float cost,
      final String when, final String dateField, final String timeField, final String timezone,
      final List<String> who, final String photoUrlLarge, final String photoUrlMedium,
      final String photoUrlSmall, final String photoUrlSquare, final Boolean isPublic,
      final String desc, final String tags, Long teacherID) {
      try {
        Map teacher = dao.getUserByID(teacherID);
        HashMap classMap = new HashMap();
        classMap.put("teacher_username", teacher.get("username"));
        Long startTime = null;
        if (when.trim().equalsIgnoreCase("now")){
          startTime = getUnixTime(new Date().getTime());
        } else {
          SimpleDateFormat dtFormat = new SimpleDateFormat("MM-dd-yyyy hh:mm aa");
          TimeZone tz = TimeZone.getTimeZone(timezone);
          dtFormat.setTimeZone(tz);
          String rawDate = dateField + " " + timeField;
          try {
            Date formatted = dtFormat.parse(rawDate);
            startTime = getUnixTime(formatted.getTime());
          } catch (ParseException e) {
            Logger.error(e, "In create class error parsing start date:"+rawDate+", teacherID:"+teacherID);
            startTime = new Date().getTime();
          }  
        }
        classMap.put("start_time", startTime);
        classMap.put("teacher_id", teacherID);
        classMap.put("timezone", timezone);
        classMap.put("is_ended", false);
        Integer maxMinutes = null;
        Float classCost = (float)0.0;
        Boolean isPaid = false;
        if (charge.trim().equalsIgnoreCase("free")){
          maxMinutes = config.getFreeMaxMinutes();
          
        } else {
          maxMinutes = config.getPaidMaxMinutes();
          classCost = cost;
          isPaid = true;
        }
        classMap.put("is_paid", isPaid);
        classMap.put("cost", classCost);
        classMap.put("max_minutes", maxMinutes);
        classMap.put("description", desc);
        
        classMap.put("photo_url_large", photoUrlLarge);
        classMap.put("photo_url_medium", photoUrlMedium);
        classMap.put("photo_url_small", photoUrlSmall);
        classMap.put("photo_url_square", photoUrlSquare);
        
        classMap.put("is_public", isPublic);
        classMap.put("recorded_public", isPublic);
        
        classMap.put("class_name", className);
        classMap.put("created_at", getUnixTime(new Date().getTime()));
        classMap.put("updated_at", getUnixTime(new Date().getTime()));
        Long classId = dao.insertObj("Class", classMap);
        
        String [] tagList = tags.split(",");
        for (int i = 0; i< tagList.length; ++i){
          addClassTag(classId, tagList[i]);
        }
        String teacherEmail = getUserEmail(teacher);
        HashMap classTeacher = new HashMap<>();
        classTeacher.put("class_id", classId);
        classTeacher.put("user_id",teacherID);
        classTeacher.put("is_teacher",true);
        classTeacher.put("cost", classCost);
        classTeacher.put("user_email", teacherEmail);
        classTeacher.put("paid", true);
        dao.insertObj("ClassUser", classTeacher);
        Mails.inviteTeacher(teacherEmail, classId);
        
        HashSet<String> whoSet = new HashSet();
        for (String wh:who){
          whoSet.add(wh);
        }
        
        inviteToClass(whoSet, teacherEmail, classId);
        
        return classId;
      } catch (Exception e) {
        Logger.error(e, "Exception encountered while creating class:"+className+", from user:"+teacherID);
        return null;
      }
  }
  
  public String processCard(Integer amount, Long cardnumber, String cvc, String expMonth, String expYear){
    Stripe.apiKey = config.getStripeSecretKey();
    Map<String, Object> chargeMap = new HashMap<String, Object>();
    chargeMap.put("amount", amount);
    chargeMap.put("currency", "usd");
    Map<String, Object> cardMap = new HashMap<String, Object>();
    cardMap.put("number", cardnumber+"");
    cardMap.put("exp_month", expMonth);
    cardMap.put("exp_year", expYear);
    chargeMap.put("card", cardMap);
    try {
        Charge charge = Charge.create(chargeMap);
        return "success_"+charge.getAmount();
    } catch (StripeException e) {
        return e.getMessage();
    } catch (Exception e) {
      Logger.error(e, "Exception encountered while processing credit card");
      return null;
    }
  }
  public Boolean inviteToClass(Set<String> whoSet, String teacherEmail, Long classId){
    try {
      Map classMap = dao.getClassByID(classId);
      Float classCost = (Float)classMap.get("cost");
      for (String w:whoSet){
        if (w.isEmpty()|| w.trim().equalsIgnoreCase(teacherEmail)){
          continue;
        }
        //System.err.println("loading user for email:"+w);
        Map user = null;
        try {
           user = dao.getUserByEmail(w);
        } catch (Exception e) {
              
        }
        Boolean paid = false;
        if (classCost<=0) {
          paid = true;
        }
        if (user!=null){
          Long uid = (Long)user.get("id");
          HashMap classUser = new HashMap<>();
          classUser.put("class_id", classId);
          classUser.put("user_id",uid);
          classUser.put("is_teacher",false);
          classUser.put("user_email", w);
          classUser.put("cost", classCost);
          classUser.put("paid", paid);
          dao.insertObj("ClassUser", classUser);
          Mails.inviteRegisteredUser(w, classId);
        } else {
          HashMap pendingClassUser = new HashMap<>();
          pendingClassUser.put("class_id", classId);
          pendingClassUser.put("email", w);
          pendingClassUser.put("cost", classCost);
          pendingClassUser.put("paid", paid);
          dao.insertObj("PendingClassUser", pendingClassUser);
          Mails.inviteUnregistered(w, classId);
        }
      
 }
      return true;
    } catch (Exception e) {
      Logger.error(e, "Exception occurred while adding participants for classID:"+classId);
      return false;
    }
  }
  public Boolean addClassTag(Long classId, String tag) {
    try {
      HashMap tagMap = new HashMap<>();
      String finalTag = WordUtils.capitalize(tag.trim());
      tagMap.put("tag", finalTag);
      Long tagId = dao.insertObj("Tag", tagMap);
      HashMap tagClass = new HashMap<>();
      tagClass.put("tag_id", tagId);
      tagClass.put("class_id", classId);
      dao.insertObj("TagClass", tagClass);
      return true;
    } catch (Exception e) {
      Logger.error(e, "Exception occurred while inserting tag:"+tag);
      return false;
    }
  }
  

  public String getTokSessionId(Long classId) {
    // have to do this in a transaction so the sessionId doesnt get stepped on
    try {
      String sessionId = dao.getClassTokSession(classId, config.getTokKey(), config.getTokSecret());
      return sessionId;
    } catch (Exception e) {
      Logger.error(e, "Exception while getting tokbox session for classID: %s", classId + "");
      return null;
    }
  }

  public String getTokToken(String sessionId, Long userId) {
    try {
      String connectionMetadata = "" + userId;
      OpenTokSDK sdk = new OpenTokSDK(config.getTokKey(), config.getTokSecret());
      return sdk.generate_token(sessionId, RoleConstants.MODERATOR, null, connectionMetadata);
    } catch (Exception e) {
      Logger.error(e, "Exception while getting tokbox token for userID: %s, sessionID: %s", userId
          + "", sessionId + "");
      return null;
    }
  }

  public Long saveUser(HashMap user) throws Exception {
    Long id = dao.insertObj("User", user);
    return id;
  }

  public Boolean downloadArchive(Long classId, String archiveId) {
    String tempFileName = UUID.randomUUID().toString();
    File resultFile = new File(tempFileName);
    try {
      String manifestUrl = config.getTokEndpoint() + "/hl/archive/getmanifest/" + archiveId;

      HttpResponse manifestResponse =
          WS.url(manifestUrl)
              .setHeader("X-TB-PARTNER-AUTH", config.getTokKey() + ":" + config.getTokSecret())
              .setParameter("api_key", config.getTokKey()).timeout("30s").post();

      if (!manifestResponse.success()) {
        Logger.error("Could not get archive manifest for classID: %s, archiveID: %s", classId + "",
            archiveId + "");
        return false;
      }
      HashMap<String, JsonObject> videoMap = new HashMap<>();
      String xmlString = manifestResponse.getString();
      Document doc = XML.getDocument(xmlString);
      Boolean videoFound = false;
      for (Node video : XPath.selectNodes("/manifest/resources/video", doc)) {
        videoFound = true;
        String videoId = XPath.selectText("@id", video);
        String streamName = XPath.selectText("@name", video);
        Boolean isTeacher = false;
        if (streamName.trim().equalsIgnoreCase("Instructor")) {
          isTeacher = true;
        }
        String videoLength = XPath.selectText("@length", video);
        JsonObject videoData = new JsonObject();
        videoData.addProperty("length", videoLength);
        videoData.addProperty("isTeacher", isTeacher);
        videoMap.put(videoId, videoData);
      }

      if (!videoFound) {
        Logger
            .error("No videos exist for classID: %s, archiveID: %s", classId + "", archiveId + "");
        return false;
      }

      for (Node event : XPath.selectNodes("/manifest/timeline/event", doc)) {
        String videoId = XPath.selectText("@id", event);
        String videoOffset = XPath.selectText("@offset", event);
        JsonObject videoData = (JsonObject) videoMap.get(videoId);
        if (videoData != null) {
          videoData.addProperty("offset", videoOffset);
        }
      }

      for (String vid : videoMap.keySet()) {
        String downloadUrl = config.getTokEndpoint() + "/hl/archive/url/" + archiveId + "/" + vid;
        HttpResponse downloadResponse =
            WS.url(downloadUrl)
                .setHeader("X-TB-PARTNER-AUTH", config.getTokKey() + ":" + config.getTokSecret())
                .timeout("30s").get();

        String tokDownloadUrl = downloadResponse.getString();
        HttpResponse tokDownloadResponse = WS.url(tokDownloadUrl).timeout("30s").get();

        if (!tokDownloadResponse.success()) {
          Logger.error("Could not get archive download url for classID: %s, archiveID: %s", classId
              + "", archiveId + "");
          return false;
        }


        OutputStream out = new FileOutputStream(resultFile);

        InputStream in = tokDownloadResponse.getStream();
        byte buf[] = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
          out.write(buf, 0, len);
        }
        out.close();
        in.close();
        Boolean uploadStatus = uploadToS3(resultFile, vid + ".flv", "video/x-flv");

        if (!uploadStatus) {
          return false;
        }

        JsonObject data = videoMap.get(vid);
        data.addProperty("url", config.getS3DocUrl() + "/" + vid + ".flv");
      }
      // persist archive Data
      for (String videoId : videoMap.keySet()) {
        JsonObject data = (JsonObject) videoMap.get(videoId);
        HashMap archiveMap = new HashMap<>();
        archiveMap.put("archive_id", archiveId);
        archiveMap.put("class_id", classId);
        archiveMap.put("video_id", videoId);
        archiveMap.put("is_teacher", data.get("isTeacher").getAsBoolean());
        archiveMap.put("url", data.get("url").getAsString());
        archiveMap.put("length", data.get("length").getAsLong());
        archiveMap.put("offset", data.get("offset").getAsLong());
        dao.insertObj("ClassArchive", archiveMap);
      }
      return true;
    } catch (Exception e) {
      Logger.error(e, "Exception occurred while dowloading archives for classID: %s", classId + "");
      return false;
    } finally {
      try {
        resultFile.delete();
      } catch (Exception ex) {
        Logger.error(ex,
            "Exception occurred while removing a temp file while downloading archive classID: %s",
            classId + "");
      }
    }
  }

  public Boolean addClassMaterial(String link, Long classId, String name) {
    try {
      HashMap materialMap = new HashMap();
      materialMap.put("class_id", classId);
      materialMap.put("link", link);
      materialMap.put("material_name", name);
      dao.insertObj("Material", materialMap);
      return true;
    } catch (Exception e) {
      Logger.error(e, "Exception occurred while adding class material classID: %s", classId + "");
      return false;
    }
  }

  public Long insertFacebookUser(com.restfb.types.User fbUser) {
    try {
      HashMap userMap = new HashMap();
      userMap.put("fname", fbUser.getFirstName());
      userMap.put("lname", fbUser.getLastName());
      userMap.put("canonnical_email", fbUser.getEmail());
      userMap.put("is_facebook", true);
      userMap.put("facebook_id", fbUser.getId());
      userMap.put("photo_url_large", "https://graph.facebook.com/" + fbUser.getId()
          + "/picture?type=large");
      userMap.put("photo_url_medium", "https://graph.facebook.com/" + fbUser.getId()
          + "/picture?type=normal");
      userMap.put("photo_url_small", "https://graph.facebook.com/" + fbUser.getId()
          + "/picture?type=small");
      userMap.put("photo_url_square", "https://graph.facebook.com/" + fbUser.getId()
          + "/picture?type=square");

      userMap.put("username", fbUser.getUsername());
      userMap.put("location", fbUser.getLocation().getName());
      Long now = new Date().getTime();
      userMap.put("created_at", now);
      userMap.put("updated_at", now);
      userMap.put("last_signed_on", now);
      userMap.put("is_deleted", false);
      userMap.put("linkedin_verified", false);
      userMap.put("is_teacher", false);
      Long userId = getDao().insertObj("User", userMap);

      List<Education> educationList = fbUser.getEducation();
      if (educationList != null) {
        int i = 0;
        for (Education e : educationList) {
          HashMap edMap = new HashMap<>();
          edMap.put("credential_order", i);
          edMap.put("credential_type", DBHelper.CREDENTIAL_TYPE.EDUCATION.ordinal());
          String school = e.getSchool() != null ? e.getSchool().getName() : "Unknown School";
          String year = e.getYear() != null ? e.getYear().getName() : "";
          String degree = e.getDegree() != null ? e.getDegree().getName() : "";
          edMap.put("school", school);
          edMap.put("year_attained", year);
          edMap.put("degree", degree);
          edMap.put("user_id", userId);
          List<NamedFacebookType> conc = e.getConcentration();
          if (conc != null && conc.size() > 0) {
            String concentration = "";
            int j = 0;
            for (NamedFacebookType c : conc) {
              if (j > 0) {
                concentration += ", ";
              }
              concentration += c.getName();
              ++j;
            }
            edMap.put("concentration", concentration);
          }
          getDao().insertObj("Credential", edMap);
          ++i;
        }
      }
      return userId;
    } catch (Exception e) {
      Logger.error(e, "Exception occurred while adding facebook user: %s", fbUser.getEmail() + "");
      return null;
    }
  }

  public void transferPendingClassUser(String email, Long userId) {
    List<Map> pendingList = dao.getAllPendingUsersForEmail(email);
    for (Map p:pendingList){
      try {
        Long classId = (Long)p.get("class_id");
        Float cost = (Float)p.get("cost");
        Boolean paid = false;
        if (cost <=0){
          paid = true;
        }
        HashMap cu = new HashMap<>();
        cu.put("class_id", classId);
        cu.put("is_teacher", false);
        cu.put("user_email", email);
        cu.put("user_id", userId);
        cu.put("cost", cost);
        cu.put("paid", paid);
        dao.insertObj("ClassUser", cu);
      } catch (Exception e) {
        Logger.error(e, "Exception encountered while transferring pendingclassUser for:"+email);
      }
    }
    dao.deletePendingClassUser(email);
  }

  public Long registerUser(Long userId, String userEmail, String userName, String password,
      String photoUrlLarge, String photoUrlMedium, String photoUrlSmall, String photoUrlSquare,
      String firstName, String lastName) {
    try {
      HashMap userMap = new HashMap();
      if (userId != null) {
        userMap.put("id", userId);
      }
      if (firstName != null && !firstName.isEmpty() && userMap.get("fname")==null) {
        userMap.put("fname", firstName);
      }
      if (lastName != null && !lastName.isEmpty() && userMap.get("lname")==null) {
        userMap.put("lname", lastName);
      }

      userMap.put("user_email", userEmail);
      if (photoUrlLarge != null && photoUrlLarge.length() > 0) {
        userMap.put("photo_url_large", photoUrlLarge);
      }
      if (photoUrlMedium != null && photoUrlMedium.length() > 0) {
        userMap.put("photo_url_medium", photoUrlMedium);
      }
      if (photoUrlSmall != null && photoUrlSmall.length() > 0) {
        userMap.put("photo_url_small", photoUrlSmall);
      }
      if (photoUrlSquare != null && photoUrlSquare.length() > 0) {
        userMap.put("photo_url_square", photoUrlSquare);
      }
      String pwHash = BCrypt.hashpw(password, BCrypt.gensalt());
      userMap.put("password", pwHash);
      userMap.put("username", userName);
      Long now = new Date().getTime();
      userMap.put("created_at", now);
      userMap.put("updated_at", now);
      userMap.put("last_signed_on", now);
      userMap.put("is_deleted", false);
      userMap.put("linkedin_verified", false);
      userMap.put("is_teacher", false);
      Long savedId = getDao().insertObj("User", userMap);
      
      transferPendingClassUser(userEmail, savedId);
      
      return savedId;
    } catch (Exception e) {
      Logger.error(e, "Exception occurred while adding registering: %s", userEmail);
      return null;
    }
  }

  public Boolean updatePaidClass(Long userId, Long classId) {
    try {
      Map classUser = dao.getClassUser(userId, classId);
      classUser.put("paid",true);
      dao.insertObj("ClassUser",classUser);
      return true;
    } catch (Exception e) {
      Logger.error(e, "Exception occurred while updating paid status for user:"+userId+" and class:"+classId);
      return false;
    }
  }
  
  public String getDegree(Long userId) {
    try {
      Map credential = dao.getHighestEducation(userId);
      if (credential == null) {
        return "Education Credentials Not Specified";
      } else {
        String degree = (String)credential.get("degree");
        String school = (String)credential.get("school");
        if (degree != null && !degree.isEmpty()){
          return degree + ", " + school;
        } else {
          return school;
        }
      }
    } catch (Exception e) {
      Logger.error(e, "Exception occurred while getting degree for user:"+userId);
      return "Education Credentials Not Specified";
    }
  }
  
  public Long setInstructorInfo(String firstName, String lastName, Long userId, String location,
      String degree, String school) {
    try {
      HashMap userMap = new HashMap();
      if (userId != null) {
        userMap.put("id", userId);
      }
      if (firstName != null && !firstName.isEmpty()) {
        userMap.put("fname", firstName);
      }
      if (lastName != null && !lastName.isEmpty()) {
        userMap.put("lname", lastName);
      }
      if (school != null && !school.isEmpty()) {
        getDao().deleteUserCredentials(userId);
        HashMap edMap = new HashMap<>();
        edMap.put("credential_order", 0);
        edMap.put("school", school);
        if (degree != null && !degree.isEmpty()) {
          edMap.put("degree", degree);
        }
        edMap.put("user_id", userId);
        getDao().insertObj("Credential", edMap);
      }
      Long savedId = getDao().insertObj("User", userMap);
      return savedId;
    } catch (Exception e) {
      Logger.error(e, "Exception occurred while adding instructor info: %s", userId + "");
      return null;
    }
  }

  public String getUserEmail(Map user){
    String email = (String)user.get("canonnical_email");
    if (email==null || email.isEmpty()){
      email = (String)user.get("user_email");
    }
    return email;
  }

  public String getOrbitHost() {
    // TODO replace with sharding or zookeeper in prod
    return config.getOrbitHost();
  }

  public String getOrbitPort() {
    // TODO replace with sharding or zookeeper in prod
    return config.getOrbitPort();
  }

  public Long getUnixTime(Long javaTime) {
    return javaTime / 1000;
  }

  public Long getJavaTime(Long unixTime) {
    return unixTime * 1000;
  }
}
