package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.opentok.api.OpenTokSDK;
import com.opentok.exception.OpenTokException;

public class JDBCDao extends JdbcDaoSupport {

  private TransactionTemplate transactionTemplate = null;
  private DBHelper dbHelper = null;

  public void setTransactionManager(PlatformTransactionManager transactionManager) {
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    getConnection();
  }

  public void setDbHelper(DBHelper helper) {
    this.dbHelper = helper;
    this.dbHelper.setSchemaInfo(getConnection());
  }

  public Map getUserByID(Long id) {
    try {
      String sql = "SELECT * FROM User WHERE id = ?";
      return getJdbcTemplate().queryForMap(sql, new Object[] {id});
    } catch (DataAccessException e) {
      return null;
    }
  }

  public Map getMaxClassId() {
    try {
      String sql =
          "SELECT count(distinct ca.class_id) as max FROM ClassArchive ca, Class c where c.is_public=true and ca.class_id=c.id";
      return getJdbcTemplate().queryForMap(sql);
    } catch (DataAccessException e) {
      return null;
    }
  }

  public List getTagsForClass(Long classId) {
    try {
      String sql =
          "SELECT t.tag FROM Class c, Tag t, TagClass tc where tc.tag_id = t.id and tc.class_id = c.id and c.id = ?";
      return getJdbcTemplate().queryForList(sql, new Object[] {classId});
    } catch (DataAccessException e) {
      e.printStackTrace();

      return null;
    }
  }

  public List getRecentClasses(Integer pageSize, Integer pageNumber) {
    try {
      String sql =
          "SELECT DISTINCT c.id as classId, u.fname as firstName, u.lname as lastName, c.start_time as startTime, c.photo_url_large as classPhotoUrl, u.photo_url_square as profilePic, c.description,  c.class_name as className from ClassArchive arch, Class c, User u where arch.class_id=c.id AND c.teacher_id = u.id and c.is_public = true ORDER BY c.updated_at DESC LIMIT ? offset ?";
      return getJdbcTemplate().queryForList(sql, new Object[] {pageSize, pageNumber * pageSize});
    } catch (DataAccessException e) {
      return null;
    }
  }

  public List getScheduledClasses(Long userId) {
    try {
      String sql =
          "SELECT DISTINCT c.id as classId,c.start_time as start, c.start_time+80*60 as end, c.class_name as title from ClassUser ca,  Class c, User u WHERE ca.class_id = c.id and ca.user_id = u.id and u.id = ?  ORDER BY c.updated_at";
          return getJdbcTemplate().queryForList(sql, new Object[] {userId});
    } catch (DataAccessException e) {
      e.printStackTrace();
      return null;
    }
  }

  public List getTaughtClasses(Integer pageSize, Integer pageNumber, Long userId) {
    try {
      String sql =
          "SELECT DISTINCT c.id as classId, u.fname as firstName, u.lname as lastName, c.start_time as startTime, c.photo_url_large as classPhotoUrl, u.photo_url_square as profilePic, c.description as description,  c.class_name as className from ClassArchive arch, Class c, User u where arch.class_id=c.id AND c.teacher_id = u.id AND teacher_id = ? and c.is_public = true ORDER BY c.updated_at DESC LIMIT ? offset ?";
      return getJdbcTemplate().queryForList(sql,
          new Object[] {userId, pageSize, pageNumber * pageSize});
    } catch (DataAccessException e) {
      return null;
    }
  }

  public List getAttendedClasses(Integer pageSize, Integer pageNumber, Long userId) {
    try {
      String sql =
          "SELECT DISTINCT c.id as classId, u.fname as firstName, u.lname as lastName, c.start_time as startTime, c.photo_url_large as classPhotoUrl, u.photo_url_square as profilePic, c.description as description,  c.class_name as className from ClassArchive arch, Class c, User u where arch.class_id=c.id AND c.teacher_id = u.id AND teacher_id <> ? and c.is_public = true ORDER BY c.updated_at DESC LIMIT ? offset ?";
      return getJdbcTemplate().queryForList(sql,
          new Object[] {userId, pageSize, pageNumber * pageSize});
    } catch (DataAccessException e) {
      return null;
    }
  }

  public List getAllUsersIdsWithUsername(String username) {
    try {
      String sql = "SELECT id FROM User WHERE username = ?";
      return getJdbcTemplate().queryForList(sql, new Object[] {username});
    } catch (DataAccessException e) {
      return null;
    }
  }

  public List getAllPendingUsersForClass(Long classId) {
    try {
      String sql = "SELECT * FROM PendingClassUser WHERE class_id = ?";
      return getJdbcTemplate().queryForList(sql, new Object[] {classId});
    } catch (DataAccessException e) {
      return null;
    }
  }

  public List getAllPendingUsersForEmail(String email) {
    try {
      String sql = "SELECT * FROM PendingClassUser WHERE email = ?";
      return getJdbcTemplate().queryForList(sql, new Object[] {email});
    } catch (DataAccessException e) {
      return null;
    }
  }

  public List getAllUsersForClass(Long classId) {
    try {
      String sql = "SELECT * FROM ClassUser WHERE class_id = ?";
      return getJdbcTemplate().queryForList(sql, new Object[] {classId});
    } catch (DataAccessException e) {
      return null;
    }
  }

  public Map getUserByUsername(String username) {
    try {
      String sql = "SELECT * FROM User WHERE username = ?";
      return getJdbcTemplate().queryForMap(sql, new Object[] {username});
    } catch (DataAccessException e) {
      return null;
    }
  }

  public Map getUserByFacebookID(Long facebookID) {
    try {
      String sql = "SELECT * FROM User WHERE facebook_id = ?";
      return getJdbcTemplate().queryForMap(sql, new Object[] {facebookID});
    } catch (DataAccessException e) {
      return null;
    }
  }

  public Map getUserByEmail(String email) {
    try {
      String sql = "SELECT * FROM User WHERE user_email = ? OR canonnical_email = ?";
      return getJdbcTemplate().queryForMap(sql, new Object[] {email, email});
    } catch (DataAccessException e) {
      e.printStackTrace();
      return null;
    }
  }

  public Map getUserByRecoveryId(String recoveryId) {
    try {
      String sql = "SELECT * FROM User WHERE recovery_id = ?";
      return getJdbcTemplate().queryForMap(sql, new Object[] {recoveryId});
    } catch (DataAccessException e) {
      return null;
    }
  }

  public void setUserRecoveryID(String recoveryID, Long userID) {
    String sql = "UPDATE User SET recovery_id = ? WHERE id = ?";
    getJdbcTemplate().update(sql, new Object[] {recoveryID, userID});
  }

  public Map getClassByID(Long id) {
    try {
      String sql = "SELECT * FROM Class WHERE id = ?";
      return getJdbcTemplate().queryForMap(sql, new Object[] {id});
    } catch (DataAccessException e) {
      return null;
    }
  }

  public List getClassByUserID(Long userid) {
    try {
      String sql =
          "SELECT Distinct c.id, c.class_name, c.start_time FROM Class c, ClassUser cu, User u WHERE u.id = cu.user_id and c.id= cu.class_id and cu.user_id = ? ORDER BY c.start_time desc";
      return getJdbcTemplate().queryForList(sql, new Object[] {userid});
    } catch (DataAccessException e) {
      e.printStackTrace();
      return null;
    }
  }

  public List getMaterialForClass(Long id) {
    try {
      String sql = "SELECT link, material_name FROM Material WHERE class_id = ?";
      return getJdbcTemplate().queryForList(sql, new Object[] {id});
    } catch (DataAccessException e) {
      return null;
    }
  }

  public List getCredentialForUser(Long userid) {
    try {
      String sql = "SELECT * from Credential where user_id = ?";
      return getJdbcTemplate().queryForList(sql, new Object[] {userid});
    } catch (DataAccessException e) {
      return null;
    }
  }

  public void deleteUserCredentials(Long userId) {
    String sql = "DELETE FROM Credential WHERE user_id = ?";
    getJdbcTemplate().update(sql, new Object[] {userId});
  }

  public void deleteClassTags(Long classId) {
    String sql = "DELETE FROM TagClass WHERE class_id = ?";
    getJdbcTemplate().update(sql, new Object[] {classId});
  }

  public void deletePendingClassUser(String email) {
    String sql = "DELETE FROM PendingClassUser WHERE email = ?";
    getJdbcTemplate().update(sql, new Object[] {email});
  }

  public List getClassArchiveByID(Long id, String streamingUrl, String s3Bucket) {
    try {
      String sql =
          "SELECT *, '" + streamingUrl + "' as streaming_url, '" + s3Bucket
              + "' as s3_bucket FROM ClassArchive WHERE class_id = ?";
      return getJdbcTemplate().queryForList(sql, new Object[] {id});
    } catch (DataAccessException e) {
      return null;
    }
  }

  public Map getClassUser(Long userId, Long classId) {
    try {
      String sql = "SELECT * FROM ClassUser WHERE user_id = ? AND class_id = ?";
      return getJdbcTemplate().queryForMap(sql, new Object[] {userId, classId});
    } catch (DataAccessException e) {
      return null;
    }
  }

  public void setClassEnded(Long classId) {
    String sql = "UPDATE Class SET is_ended = ? WHERE id = ?";
    getJdbcTemplate().update(sql, new Object[] {true, classId});
  }

  public void setClassTokSession(String sessionId, Long classId) {
    String sql = "UPDATE Class SET tok_session = ? WHERE id = ?";
    getJdbcTemplate().update(sql, new Object[] {sessionId, classId});
  }

  public void setClassArchiveId(String archiveId, Long classId) {
    String sql = "UPDATE Class SET archive_id = ? WHERE id = ?";
    getJdbcTemplate().update(sql, new Object[] {archiveId, classId});
  }

  public void setClassVideoOffset(Long videoOffset, Long classId) {
    String sql = "UPDATE Class SET video_offset = ? WHERE id = ?";
    getJdbcTemplate().update(sql, new Object[] {videoOffset, classId});
  }

  public Map getHighestEducation(Long userId) {
    try {
      String sql = "SELECT * FROM Credential WHERE user_id = ? ORDER BY degree_rank DESC LIMIT 1";
      return getJdbcTemplate().queryForMap(sql, new Object[] {userId});
    } catch (DataAccessException e) {
      return null;
    }
  }

  public Long updateObj(String table, HashMap params, HashMap conditions) throws Exception {
    Map queryData = dbHelper.getUpdateFromMap(table, params, conditions);
    final String sql = (String) queryData.get(DBHelper.SQL);
    final Object[] data = (Object[]) queryData.get(DBHelper.DATA);
    KeyHolder keyHolder = new GeneratedKeyHolder();
    getJdbcTemplate().update(new PreparedStatementCreator() {
      public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        for (int i = 0; i < data.length; i++) {
          int index = i + 1;
          ps.setObject(index, data[i]);
        }
        return ps;
      }
    }, keyHolder);
    return keyHolder.getKey().longValue();
  }

  public Long insertObj(String table, Map params) throws Exception {
    Map queryData = dbHelper.getInsertFromMap(table, params);
    final String sql = (String) queryData.get(DBHelper.SQL);
    // System.err.println("SQL:"+sql);
    final Object[] data = (Object[]) queryData.get(DBHelper.DATA);
    KeyHolder keyHolder = new GeneratedKeyHolder();
    getJdbcTemplate().update(new PreparedStatementCreator() {
      public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        for (int i = 0; i < data.length; i++) {
          int index = i + 1;
          ps.setObject(index, data[i]);
        }
        return ps;
      }
    }, keyHolder);
    Long id = (Long) keyHolder.getKeyList().get(0).get("GENERATED_KEY");
    return id;
  }

  public Long insertClass(final HashMap classMap, final Long teacherID, final List<Long> studentList) {
    return transactionTemplate.execute(new TransactionCallback<Long>() {
      public Long doInTransaction(TransactionStatus transactionStatus) {
        try {
          Long classId = insertObj("Class", classMap);
          HashMap teacherMap = new HashMap();
          teacherMap.put("class_id", classId);
          teacherMap.put("user_id", teacherID);
          teacherMap.put("is_teacher", true);
          insertObj("ClassUser", teacherMap);
          for (Long s : studentList) {
            HashMap studentMap = new HashMap();
            studentMap.put("class_id", classId);
            studentMap.put("user_id", s);
            studentMap.put("is_teacher", false);
            insertObj("ClassUser", studentMap);
          }
          return classId;
        } catch (Exception e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
          transactionStatus.setRollbackOnly();
          return null;
        }
      }
    });
  }

  public String getClassTokSession(final Long classId, final Integer tokKey, final String tokSecret) {
    return transactionTemplate.execute(new TransactionCallback<String>() {
      public String doInTransaction(TransactionStatus transactionStatus) {
        Map classMap = getClassByID(classId);
        String sessionId = (String) classMap.get("tok_session");
        if (sessionId != null) {
          return sessionId;
        } else {
          try {
            OpenTokSDK sdk = new OpenTokSDK(tokKey, tokSecret);
            sessionId = sdk.create_session().getSessionId();
            setClassTokSession(sessionId, classId);
            return sessionId;
          } catch (Exception e) {
            e.printStackTrace();
            transactionStatus.setRollbackOnly();
            return null;
          }
        }
      }
    });
  }

}
