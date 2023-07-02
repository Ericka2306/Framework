package etu1965.framework.servlet;

import etu1965.framework.FileUpload;
import etu1965.framework.Mapping;
import etu1965.framework.ModelView;
import etu1965.framework.annotation.Url;
import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.http.Part;
import java.util.Collection;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.lang.annotation.Annotation;
import java.sql.Date;
import jakarta.servlet.annotation.MultipartConfig;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;


@MultipartConfig
public class FrontServlet extends HttpServlet { 

    HashMap<String,Mapping> mappingUrls;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String pkg = this.getInitParameter("package");
        HashMap<String,Mapping> mappingUrl =  this.allMappingUrls(pkg);
        this.setMappingUrls(mappingUrl);
    }
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        String servletName = request.getServletPath().substring(1);

        HashMap<String,Mapping> mappingUrls = this.getMappingUrls(); 
        Set<String> mappingUrlsKeys = mappingUrls.keySet();

        for(String key : mappingUrlsKeys){
            if(key.equals(servletName)){
                try {
                    Class<?> classMapping = Class.forName(this.getInitParameter("package")+"."+mappingUrls.get(key).getClassName());
                    Object objet = classMapping.getDeclaredConstructor().newInstance();
                    
                    this.setObject(request,response,objet);
                    this.dispatchModelView(request , response , objet , key);

                } catch (Exception ex) {
                    out.println(ex.getMessage());
                }
            }
        }
    }

    public void setObject(HttpServletRequest request , HttpServletResponse response , Object objet )
            throws Exception{

        Field[] attributs = objet.getClass().getDeclaredFields();
        String[] setters = new String[attributs.length];
        for(int i=0 ; i<attributs.length ; i++){
            setters[i] = "set"+attributs[i].getName().substring(0,1).toUpperCase()+attributs[i].getName().substring(1);
        }

        for(int i=0 ; i<attributs.length ; i++){
            Method set = objet.getClass().getDeclaredMethod(setters[i], attributs[i].getType());
            if(attributs[i].getType() == (new FileUpload()).getClass() && request.getContentType() != null && request.getContentType().toLowerCase().startsWith("multipart/form-data")){
                Part filePart = request.getPart(attributs[i].getName());
                if (filePart != null) {
                    String fileName = filePart.getSubmittedFileName();
                    byte[] fileBytes = convertToByteArray(filePart);
                    FileUpload fileUpload = new FileUpload();
                    fileUpload.setName(fileName);
                    fileUpload.setBytes(fileBytes);
                    set.invoke(objet,fileUpload);
                }
            }else{
                String[] parameter = request.getParameterValues(attributs[i].getName());
                if(parameter!=null){
                    set.invoke(objet,FrontServlet.castStringToType(parameter,attributs[i].getType()));
                }
            }
        }

    }

    public Method getMethodByUrl(Object objet , String mappingUrlkey)
            throws Exception{
        Method[] all_methods = objet.getClass().getDeclaredMethods();
        for(int i=0 ; i<all_methods.length ; i++){
            Annotation[] annotations = all_methods[i].getAnnotations();
            for (int j = 0; j < annotations.length; j++) {
                if(annotations[j].annotationType()==Url.class)
                {
                    Url url=(Url)annotations[j];
                    if(url.lien().compareTo(mappingUrlkey)==0 && all_methods[i].getName().compareTo(mappingUrls.get(mappingUrlkey).getMethod())==0){
                        return all_methods[i];
                    }
                }
            }
        }
        throw new Exception("Method not found");
    }

    public Object[] getMethodParametersValues(HttpServletRequest request , HttpServletResponse response , Method method){
        Parameter[] parameters = method.getParameters();
        Object[] parametersValue = new Object[parameters.length];
        for(int i=0 ; i<parameters.length ; i++){
            String[] urlParam = request.getParameterValues(parameters[i].getName());
            parametersValue[i] = FrontServlet.castStringToType(urlParam,parameters[i].getType());
        }
        return parametersValue;
    }
    public void dispatchModelView(HttpServletRequest request , HttpServletResponse response , Object objet , String mappingUrlkey)
            throws Exception{
        Method method = this.getMethodByUrl(objet,mappingUrlkey);
        Object[] parameters = this.getMethodParametersValues(request,response,method);
        
        ModelView mv = new ModelView();
        if(parameters.length==0){
            mv = (ModelView)method.invoke(objet);
        }else if(parameters.length > 0){
            mv = (ModelView)method.invoke(objet,parameters);
        }
        
        Set<String> mvKeys = mv.getData().keySet();
        for(String mvKey : mvKeys){
            request.setAttribute(mvKey , mv.getData().get(mvKey));
        }

        RequestDispatcher dispat = request.getRequestDispatcher(mv.getView());
        dispat.forward(request,response);
    }

    public HashMap<String, Mapping> allMappingUrls(String pckg){
        HashMap<String, Mapping> mappingUrl = new HashMap<String, Mapping>();

        ServletContext context = getServletContext();
        String path = "/WEB-INF/classes/"+pckg;

        Set<String> classNames = context.getResourcePaths(path);
        for (String className : classNames) {
            if (className.endsWith(".class")) {
                String fullClassName = className.substring(0, className.length() - 6);
                int taille = fullClassName.split("/").length;
                fullClassName = fullClassName.split("/")[taille-2]+"."+fullClassName.split("/")[taille-1];
                try {
                    Class<?> myClass = Class.forName(fullClassName);

                    Method[] methods = myClass.getDeclaredMethods();
                    for (int i = 0; i < methods.length; i++) {
                        Annotation[] annotations = methods[i].getAnnotations();
                        for (int j = 0; j < annotations.length; j++) {
                            if(annotations[j].annotationType()==Url.class)
                            {
                                Url url=(Url)annotations[j];
                                Mapping map=new Mapping(myClass.getSimpleName(),methods[i].getName());
                                mappingUrl.put(url.lien(),map);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }

        return mappingUrl;
    }

    public static <T> T castStringToType(String[] value, Class<T> type) {
        if(value==null){
            return null;
        }
        if(!type.isArray()){
            if (type == String.class) {
                return (T) value[0];
            } else if (type == Integer.class || type == int.class) {
                return (T) Integer.valueOf(value[0]);
            } else if (type == Double.class || type == double.class) {
                return (T) Double.valueOf(value[0]);
            } else if (type == Float.class || type == float.class) {
                return (T) Float.valueOf(value[0]);
            } else if (type == Boolean.class || type == boolean.class) {
                return (T) Boolean.valueOf(value[0]);
            } else if (type == Date.class) {
                return (T) Date.valueOf(value[0]);
            }else if(type == FileUpload.class){
                FileUpload fichier = new FileUpload();
                fichier.setName(value[0]);
                return (T) fichier;
            } else {
                throw new IllegalArgumentException("Unsupported type: " + type.getName());
            }
        }else{
            if (type == String[].class) {
                return (T) value;
            } else if (type == Integer[].class) {
                Integer[] tab = new Integer[value.length];
                for (int i = 0; i < value.length; i++) {
                    tab[i] = Integer.valueOf(value[i]);
                }
                return (T) tab;
            } else if (type == Double[].class) {
                Double[] tab = new Double[value.length];
                for (int i = 0; i < value.length; i++) {
                    tab[i] = Double.valueOf(value[i]);
                }
                return (T) tab;
            } else if (type == Float[].class) {
                Float[] tab = new Float[value.length];
                for (int i = 0; i < value.length; i++) {
                    tab[i] = Float.valueOf(value[i]);
                }
                return (T) tab;
            } else if(type == int[].class){
                int[] tab = new int[value.length];
                for (int i = 0; i < value.length; i++) {
                    tab[i] = Integer.valueOf(value[i]);
                }
                return (T) tab;
            }else if( type == double[].class){
                double[] tab = new double[value.length];
                for (int i = 0; i < value.length; i++) {
                    tab[i] = Double.valueOf(value[i]);
                }
                return (T) tab;
            }else if(type == float[].class){
                float[] tab = new float[value.length];
                for (int i = 0; i < value.length; i++) {
                    tab[i] = Float.valueOf(value[i]);
                }
                return (T) tab;
            }else {
                throw new IllegalArgumentException("Unsupported type: " + type.getName());
            }
        }
    }

    private byte[] convertToByteArray(Part filePart) throws IOException {
        InputStream inputStream = filePart.getInputStream();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        return outputStream.toByteArray();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    public HashMap<String, Mapping> getMappingUrls() {
        return mappingUrls;
    }

    public void setMappingUrls(HashMap<String, Mapping> mappingUrls) {
        this.mappingUrls = mappingUrls;
    }
}
