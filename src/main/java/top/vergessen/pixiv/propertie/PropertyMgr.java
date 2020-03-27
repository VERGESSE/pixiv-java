package top.vergessen.pixiv.propertie;

import java.io.IOException;
import java.util.Properties;

// 配置文件解析器
public class PropertyMgr {

    private static final PropertyMgr propertyMgr = new PropertyMgr();
    private static Properties props = new Properties();

    static {
        try {
            props.load(PropertyMgr.class.getClassLoader().getResourceAsStream("config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private PropertyMgr(){}

    public static PropertyMgr instance(){
        return propertyMgr;
    }

    public static Integer getInt(String key){
        return Integer.parseInt(PropertyMgr.getString(key));
    }

    public static String getString(String key){
        return (String)PropertyMgr.get(key);
    }

    public static Object get(String key){
        if(props == null) return null;
        return props.get(key);
    }
}
