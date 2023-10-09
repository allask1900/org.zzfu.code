package com.zzfu.excavator.common;

import com.zzfu.excavator.entity.Entity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class BuildEntityTool {
    private static String packageOutPath = "com.zzfu.excavator.entity";//指定实体生成所在包的路径
    private static String authorName = "zzfu";//作者名字

    public void build(String tablename) {
        Connection con=null;
        PreparedStatement rsmt = null;
        try {

            Properties prop = PropertiesLoaderUtils.loadAllProperties("application.properties");
            con = DriverManager.getConnection(prop.getProperty("spring.datasource.url"),
                    prop.getProperty("spring.datasource.username"),prop.getProperty("spring.datasource.password"));
            String sql = "select  column_name,data_type,column_comment from information_schema.columns where table_schema=? and table_name=?";
            rsmt = con.prepareStatement(sql);
            rsmt.setString(1,con.getCatalog());
            rsmt.setString(2,tablename);
            ResultSet rs = rsmt.executeQuery();
            List<ColumnInfo> columnInfos = new ArrayList<>();
            while (rs.next()){
                columnInfos.add(new ColumnInfo(rs.getString(1),rs.getString(2),rs.getString(3)));
            }
            String content = parse(tablename,columnInfos);
            try {
                File directory = new File("");
                String outputPath = directory.getAbsolutePath() + "/src/main/java/" + this.packageOutPath.replace(".", "/") + "/" + camelCase(tablename) + ".java";
                System.out.println(outputPath);
                FileWriter fw = new FileWriter(outputPath);
                PrintWriter pw = new PrintWriter(fw);
                pw.println(content);
                pw.flush();
                pw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(con!=null) {
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private String parse(String tablename,List<ColumnInfo> columnInfos) {
        StringBuffer sb = new StringBuffer();
        sb.append("package " + this.packageOutPath + ";\r\n");
        sb.append("\r\n");
        sb.append("import java.util.Date;\r\n\r\n");
        sb.append("/**\r\n");
        sb.append("* " + tablename + " 实体类\r\n");
        sb.append("* " + new Date() + " " + this.authorName + "\r\n");
        sb.append("*/ \r\n");        //实体部分
        sb.append("public class " + camelCase(tablename) + "{\r\n");
        onlyPublicAttrs(sb,columnInfos);
        //processAllAttrs(sb,columnInfos);//属性
        //processAllMethod(sb,columnInfos);//get set方法
        sb.append("}");
        return sb.toString();
    }

    private void onlyPublicAttrs(StringBuffer sb,List<ColumnInfo> columnInfos) {
        for (ColumnInfo ci:columnInfos) {
            if(StringUtils.isNotEmpty(ci.comment)) {
                sb.append("\t/**\r\n");
                sb.append("\t* " + ci.comment + " \r\n");
                sb.append("\t*/ \r\n");
            }
            sb.append("\tpublic " + sqlType2JavaType(ci.type) + " " + ci.name + ";\r\n");
        }
        sb.append("\r\n");
    }

    private void processAllAttrs(StringBuffer sb,List<ColumnInfo> columnInfos) {
        for (ColumnInfo ci:columnInfos) {
            if(StringUtils.isNotEmpty(ci.comment)) {
                sb.append("\t/**\r\n");
                sb.append("\t* " + ci.comment + " \r\n");
                sb.append("\t*/ \r\n");
            }
            sb.append("\tprivate " + sqlType2JavaType(ci.type) + " " + ci.name + ";\r\n");
        }
        sb.append("\r\n");
    }

    private void processAllMethod(StringBuffer sb,List<ColumnInfo> columnInfos) {
        for (ColumnInfo ci:columnInfos) {
            sb.append("\tpublic void set" + camelCase(ci.name) + "(" + sqlType2JavaType(ci.type) + " " +ci.name + "){\r\n");
            sb.append("\t\tthis." + ci.name + "=" + ci.name + ";\r\n");
            sb.append("\t}\r\n");
            sb.append("\tpublic " + sqlType2JavaType(ci.type) + " get" + camelCase(ci.name) + "(){\r\n");
            sb.append("\t\treturn " + ci.name + ";\r\n");
            sb.append("\t}\r\n");
        }
    }
    public static String camelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder result = new StringBuilder();
        boolean capitalizeNextChar = false;
        for (int i = 0; i < input.length(); i++) {
            char currentChar = input.charAt(i);
            if (currentChar == ' ' || currentChar == '_' || currentChar == '-') {
                capitalizeNextChar = true;
            } else {
                if (capitalizeNextChar||i==0) {
                    result.append(Character.toUpperCase(currentChar));
                    capitalizeNextChar = false;
                } else {
                    result.append(Character.toLowerCase(currentChar));
                }
            }
        }
        return result.toString();
    }

    private String sqlType2JavaType(String sqlType) {
        if (sqlType.equalsIgnoreCase("bit")) {
            return "boolean";
        } else if (sqlType.equalsIgnoreCase("tinyint")) {
            return "byte";
        } else if (sqlType.equalsIgnoreCase("smallint")) {
            return "short";
        } else if (sqlType.equalsIgnoreCase("int")) {
            return "int";
        } else if (sqlType.equalsIgnoreCase("bigint")) {
            return "long";
        } else if (sqlType.equalsIgnoreCase("float")) {
            return "float";
        } else if (sqlType.equalsIgnoreCase("decimal") || sqlType.equalsIgnoreCase("numeric")
                || sqlType.equalsIgnoreCase("real") || sqlType.equalsIgnoreCase("money")
                || sqlType.equalsIgnoreCase("smallmoney")) {
            return "double";
        } else if (sqlType.equalsIgnoreCase("varchar") || sqlType.equalsIgnoreCase("char")
                || sqlType.equalsIgnoreCase("nvarchar") || sqlType.equalsIgnoreCase("nchar")
                || sqlType.equalsIgnoreCase("text")) {
            return "String";
        } else if (sqlType.equalsIgnoreCase("datetime")||sqlType.equalsIgnoreCase("date")) {
            return "Date";
        } else if (sqlType.equalsIgnoreCase("image")) {
            return "Blod";
        }

        return null;
    }

    public static class ColumnInfo {
        public String name;
        public String type;
        public String comment;

        public ColumnInfo(String name, String type, String comment) {
            this.name = name;
            this.type = type;
            this.comment = comment;
        }
    }

    public static Class<? extends Entity> getClass(String tablename) throws ClassNotFoundException {
        Class cls= BuildEntityTool.class.getClassLoader().loadClass(packageOutPath+"."+camelCase(tablename));
        return cls;
    }
    public static void main(String[] args) {
        BuildEntityTool tool=new BuildEntityTool();
        tool.build("stock_basic");
    }
}
