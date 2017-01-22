package dao;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.ejb.criteria.predicate.IsEmptyPredicate;

import com.google.common.primitives.Primitives;

public class DBHelper {
  public HashMap tableFieldMap = new HashMap<>();

  public static final String WHERE = "where_param";
  public static final String DATA = "data";
  public static final String SQL = "sql";

  public static enum CREDENTIAL_TYPE {
    EDUCATION, KNOWLEDGE
  }
  public void setSchemaInfo(Connection conn) {
    try {
      DatabaseMetaData md = conn.getMetaData();
      ResultSet ts = md.getTables(null, null, "%", null);
      while (ts.next()) {
        String table = ts.getString(3);
        if (tableFieldMap.get(table) == null) {
          tableFieldMap.put(table, new ArrayList());
        }
      }
      ResultSet cs = md.getColumns(null, null, "%", null);
      while (cs.next()) {
        String column = cs.getString("COLUMN_NAME");
        String table = cs.getString("TABLE_NAME");
        List colList = (List) tableFieldMap.get(table);
        if (colList != null) {
          colList.add(column);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public Map getUpdateFromMap(String table, HashMap untrusted, HashMap conditions) throws Exception {
    String fields = "";
    ArrayList<Object> pl = new ArrayList<>();
    int pos = 1;
    Set entries = untrusted.keySet();

    List<String> fieldList = (List) tableFieldMap.get(table);
    for (Object k : entries) {
      String key = (String) k;
      if (!fieldList.contains(key)) {
        continue;
      }
      fields += key + " = ?";
      pl.add(untrusted.get(key));

      if (pos < entries.size()) {
        fields += ", ";
      }
      ++pos;
    }
    // no valid fields matched
    if (pos <= 1) {
      return null;
    }
    String sql = "UPDATE " + table + " SET " + fields;
    if (conditions != null) {
      String cond = (String) conditions.get(DBHelper.WHERE);
      if (!cond.isEmpty()) {
        sql += " WHERE " + cond;
      }
      Object[] whereList = (Object[]) conditions.get(DBHelper.DATA);
      if (whereList != null) {
        for (int i = 0; i < whereList.length; i++) {
          pl.add(whereList[i]);
        }
      }
    }
    HashMap result = new HashMap<>();
    result.put(DBHelper.SQL, sql);
    result.put(DBHelper.DATA, pl.toArray());
    return result;
  }

  public Map getInsertFromMap(String table, Map untrusted) throws Exception {
    String fields = "";
    String paramFields = "";
    String duplicateFields ="";
    
    ArrayList<Object> paramList = new ArrayList<>();
    ArrayList<Object> duplicateParamList = new ArrayList<> ();
    int pos = 1;
    Set entries = untrusted.keySet();

    List<String> fieldList = (List) tableFieldMap.get(table);
    for (Object k : entries) {
      String key = (String) k;
      if (!fieldList.contains(key)) {
        continue;
      }
      if (fields.length() < 1) {
        fields += "(";
        paramFields += "(";
      }
      if (pos > 1) {
        fields += ",";
        paramFields += ",";
      }
      fields += k;
      paramFields += "?";
      paramList.add(untrusted.get(key));
      
      if (!key.trim().equalsIgnoreCase("id")) {
        duplicateFields += ",";
        duplicateFields += (key+"= ?");
        duplicateParamList.add(untrusted.get(key));
      }

      ++pos;
    }
    fields += ")";
    paramFields += ")";
    // no valid fields matched
    if (pos <= 1) {
      return null;
    }
    String sql = "INSERT INTO " + table + " " + fields + " VALUES " + paramFields + " ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id) " + duplicateFields;
    paramList.addAll(duplicateParamList);
    HashMap result = new HashMap<>();
    result.put(DBHelper.SQL, sql);
    result.put(DBHelper.DATA, paramList.toArray());
    return result;
  }
  
  public List getFieldNames(String table) {
    return (List)tableFieldMap.get(table);
  }

  public int nthOccurrence(String str, char c, int n) {
    int pos = str.indexOf(c, 0);
    while (n-- > 0 && pos != -1)
      pos = str.indexOf(c, pos + 1);
    return pos;
  }
  
}
