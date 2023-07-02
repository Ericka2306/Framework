package etu1965.framework.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import etu1965.framework.FileUpload;
import etu1965.framework.Mapping;
import etu1965.framework.ModelView;
import etu1965.framework.annotation.Scope;
import etu1965.framework.annotation.Session;
import etu1965.framework.annotation.Url;
import etu1965.framework.annotation.Auth;
import etu1965.framework.annotation.RestApi;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpSession;
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
import java.net.http.HttpResponse;
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
    HashMap<String,Object> singletons;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String pkg = this.getInitParameter("package");

        // Maka ny methode rehetre misy annotation URL 
        HashMap<String,Mapping> mappingUrl =  this.allMappingUrls(pkg);
        this.setMappingUrls(mappingUrl);

        // Maka ny classe ho atao singleton rehetra (misy annotation scope(value="Singleton"))
        HashMap<String,Object> singleton = this.allSingletons(pkg);
        this.setSingletons(singleton);
    }
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        // Maka ny url andehanana (ilaina afantarana anle methode hiasa)
        String servletName = request.getServletPath().substring(1);

        HashMap<String,Mapping> mappingUrls = this.getMappingUrls(); 
        try {
            // Maka ny anarana classe mifanaraka amle url andehanana
            String className = mappingUrls.get(servletName).getClassName();
            // Objet appelant anle methode
            Object objet;

            // Jerena ra singleton le classe
            if(singletons.containsKey(className))
            {
                // Maka anle singleton anatinle HashMap 
                objet = singletons.get(className);
                // Raha mbola tsisy de instancier-na de apetraka antin le HashMap
                if(objet == null)
                {
                    Class<?> classMapping = Class.forName(this.getInitParameter("package")+"."+mappingUrls.get(servletName).getClassName());
                    objet = classMapping.getDeclaredConstructor().newInstance();
                    singletons.replace(className, objet);
                }
                // Atao valeur par defaut dooly leh attribut satria mety ampifangaro données
                this.resetObject(objet);
            }else{
                // Raha tsy singleton de instancier-na fotsiny de apesaina
                Class<?> classMapping = Class.forName(this.getInitParameter("package")+"."+mappingUrls.get(servletName).getClassName());
                objet = classMapping.getDeclaredConstructor().newInstance();
            }
            // Manisy ny valeur rehetra anle objet ra misy(ohatra hoe avy am formulaire)
            this.setObject(request,response,objet,servletName);
            // ato no mi-executer anle methode sy m-dispatch 
            this.dispatchModelView(request , response , objet , servletName);

        } catch (Exception ex) {
            out.println(ex.getMessage());
        }
    }

    // Mi-set anle objet appelant
    public void setObject(HttpServletRequest request , HttpServletResponse response , Object objet , String mappingUrlkey)
            throws Exception{

        Method method = this.getMethodByUrl(objet,mappingUrlkey);
        Field[] attributs = objet.getClass().getDeclaredFields();
        String[] setters = new String[attributs.length];
        for(int i=0 ; i<attributs.length ; i++){
            setters[i] = "set"+attributs[i].getName().substring(0,1).toUpperCase()+attributs[i].getName().substring(1);
        }

        for(int i=0 ; i<attributs.length ; i++){
            // setter anle objet
            Method set = objet.getClass().getDeclaredMethod(setters[i], attributs[i].getType());
            // mi-check hoe misy attribut session ve leh objet , de mila anle session ve le user
            if(attributs[i].getName().equals("session") && this.need_session(method)){
                // Raha mila le user de setter-na amle session rehetra leh attribut session anle objet appelant
                HashMap<String , Object> hashMapSessions = new HashMap<String,Object>();
                HttpSession session = request.getSession();
                Enumeration<String> attributeNames = session.getAttributeNames();
                while (attributeNames.hasMoreElements()) {
                    String attributeName = attributeNames.nextElement();
                    Object attributeValue = session.getAttribute(attributeName);
                    hashMapSessions.put(attributeName, attributeValue);
                }
                set.invoke(objet, hashMapSessions);
            }else{
                // Raha misy type FileUpload leh attribut de manambotra objet FileUpload lou de iny no setter-na amle attribut
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
                    // Akotrzay alaina fotsiny leh valeur ana input na leh misy ? eny am url ,de caster amle type-nle attribut, de setter-na
                    String[] parameter = request.getParameterValues(attributs[i].getName());
                    if(parameter!=null){
                        set.invoke(objet,FrontServlet.castStringToType(parameter,attributs[i].getType()));
                    }
                }
            }
            
        }

    }

    // Maka anle methode ho ampesaina
    public Method getMethodByUrl(Object objet , String mappingUrlkey)
            throws Exception{
        // Method rehetra ao amle classe
        Method[] all_methods = objet.getClass().getDeclaredMethods();
        for(int i=0 ; i<all_methods.length ; i++){
            // Annotation rehetra amle method[i]
            Annotation[] annotations = all_methods[i].getAnnotations();
            for (int j = 0; j < annotations.length; j++) {
                if(annotations[j].annotationType()==Url.class)
                {
                    Url url=(Url)annotations[j];
                    // Comparer-na ra mitovy le lien eo amle annotation sy leh lien n-taper-na tam navigateur
                    if(url.lien().compareTo(mappingUrlkey)==0 && all_methods[i].getName().compareTo(mappingUrls.get(mappingUrlkey).getMethod())==0){
                        return all_methods[i];
                    }
                }
            }
        }
        throw new Exception("Method not found");
    }

    // Maka ny argument anle method ra misy
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
        HttpSession session = request.getSession();
        String authKey_1 = this.getInitParameter("connected");
        String authKey_2 = this.getInitParameter("profil");

        // Maka ny methode ho apesaina
        Method method = this.getMethodByUrl(objet,mappingUrlkey);
        // Maka ny valeur-ny arguments
        Object[] parameters = this.getMethodParametersValues(request,response,method);
        // Jerena ra Rest-api leh method de tsy mireturn ModelView (tsy m-dispatch any am page fa mandefa JSON fotsiny) 
        if(this.is_rest_api(method)){
            Object temp ;
            if(parameters.length==0){
                temp = method.invoke(objet);
            }else{
                temp = method.invoke(objet,parameters);
            }  
            this.sendJson(temp,response);
        }else{
            ModelView mv = new ModelView();

            // Jerena ra mila authentification leh methode , na mila profil manokana , na izy roa

            // Raha mila leh izy ka nefa tsisy anaty session de midispatch any am Login
            if(this.need_auth(method) && session.getAttribute(authKey_1)==null ){
                this.dispatchToLogin(request,response);
            }else if(!this.need_profil(method).equals("") && session.getAttribute(authKey_2)==null){
                this.dispatchToLogin(request,response);
            }else if(!this.need_profil(method).equals("") && session.getAttribute(authKey_2)!=null && !session.getAttribute(authKey_2).equals(need_profil(method))){
                this.dispatchToLogin(request,response);
            }else{
                // Raha tsy mila leh izy na mila fa misy anaty session de tonga de alaina ny Modelview retourner-nle fonction
                mv = this.getModelView(method, parameters , objet);
            }
            
            // Ra ohatra ka mila supprimer ny session rehetra (ohatra hoe mi-deco)
            if(mv.isInvalidateSession()){
                session.invalidate();
            }
            // Raha misy session manokana ho supprimer-na
            for(int i=0 ; i<mv.getRemoveSession().size() ; i++){
                session.removeAttribute(mv.getRemoveSession().get(i));
            }

            // Jerena sode rest-api ko leh izy (fa eto mireturn modelview leh fonction)
            if(mv.isJson())
            {
                // Raha rest-api de ny données rehetra ao amle modelview no alefa am Json
                this.sendJson(mv.getData(), response);
            }else{
                // Raha tsy rest-api de alefa any amle page ny donnee rehetra (request.setattribute(...))
                Set<String> mvKeys = mv.getData().keySet();
                for(String mvKey : mvKeys){
                    request.setAttribute(mvKey , mv.getData().get(mvKey));
                }
                // Raha misy session alefanle user de apdirina am session anle servlet dooly
                if(mv.getAuth().get(authKey_1)!=null)
                {
                    session.setAttribute(authKey_1,mv.getAuth().get(authKey_1));
                    if(mv.getAuth().get(authKey_2)!=null)
                    {
                        session.setAttribute(authKey_2,mv.getAuth().get(authKey_2));
                    }
                }
                // mi-dispatch makany amle vue nataon ny user
                RequestDispatcher dispat = request.getRequestDispatcher(mv.getView());
                dispat.forward(request,response);
            }

            
        }
    }
    // Mandefa donnee ho Json
    public void sendJson(Object data , HttpServletResponse response) throws Exception
    {
        // Conversion du HashMap en JSON
        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(data);
        // Configuration de la réponse HTTP
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        // Envoi du JSON dans le corps de la réponse
        response.getWriter().write(json);
    }
    public void dispatchToLogin(HttpServletRequest request , HttpServletResponse response) throws Exception
    {
        RequestDispatcher dispat = request.getRequestDispatcher("login.jsp");
        dispat.forward(request,response);
    }
    // Mi-invoke anle method (azahona anle ModelView)
    public ModelView getModelView(Method method , Object[] parameters , Object objet) throws Exception
    {
        ModelView mv = new ModelView();
        if(parameters.length==0){
            mv = (ModelView)method.invoke(objet);
        }else if(parameters.length > 0){
            mv = (ModelView)method.invoke(objet,parameters);
        }  
        return mv;
    }
    // micheck hoe mila authentification ve le method
    public boolean need_auth(Method method)
    {
        Annotation[] annotations = method.getAnnotations();
        for (int j = 0; j < annotations.length; j++) {
            if(annotations[j].annotationType()==Auth.class)
            {
                return true;
            }
        }
        return false;
    }
    // micheck hoe mila profil manokana ve le method
    public String need_profil(Method method)
    {
        Annotation[] annotations = method.getAnnotations();
        for (int j = 0; j < annotations.length; j++) {
            if(annotations[j].annotationType()==Auth.class)
            {
                Auth auth=(Auth)annotations[j];
                if(!auth.profil().equals(""))
                {
                    return auth.profil();
                }
            }
        }
        return "";
    }
    // micheck hoe mila anle session ve leh user
    public boolean need_session(Method method)
    {
        Annotation[] annotations = method.getAnnotations();
        for (int j = 0; j < annotations.length; j++) {
            if(annotations[j].annotationType()==Session.class)
            {
                return true;
            }
        }
        return false;
    }
    // micheck hoe rest_api ve leh method
    public boolean is_rest_api(Method method)
    {
        Annotation[] annotations = method.getAnnotations();
        for (int j = 0; j < annotations.length; j++) {
            if(annotations[j].annotationType()==RestApi.class)
            {
                return true;
            }
        }
        return false;
    }

    // Maka ny classe rehetra misy anle url
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
    // Maka ny classe rehetra singleton
    public HashMap<String,Object> allSingletons(String pckg)
    { 
        HashMap<String,Object> singletons = new HashMap<String,Object>();

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
                    if(myClass.isAnnotationPresent(Scope.class))
                    {
                        Scope scope = (Scope)myClass.getAnnotation(Scope.class);
                        if(!scope.value().equals("")){
                            Object objet = myClass.getDeclaredConstructor().newInstance();
                            singletons.put(myClass.getSimpleName(), null);
                        }  
                    }
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }
        return singletons;
    }
    // mi-reinitialiser objet
    public void resetObject(Object obj) {
        Class<?> clazz = obj.getClass();
        try {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                Class<?> fieldType = field.getType();
                if (fieldType.isPrimitive()) {
                    this.setPrimitiveDefault(obj, field);
                } else {
                    field.set(obj, null);
                } 
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    // Mamerina ny attribut ho valeur par defaut
    private void setPrimitiveDefault(Object obj, Field field) throws IllegalAccessException {
        Class<?> fieldType = field.getType();
        if (fieldType == boolean.class) {
            field.setBoolean(obj, false);
        } else if (fieldType == byte.class) {
            field.setByte(obj, (byte) 0);
        } else if (fieldType == short.class) {
            field.setShort(obj, (short) 0);
        } else if (fieldType == int.class) {
            field.setInt(obj, 0);
        } else if (fieldType == long.class) {
            field.setLong(obj, 0L);
        } else if (fieldType == float.class) {
            field.setFloat(obj, 0.0f);
        } else if (fieldType == double.class) {
            field.setDouble(obj, 0.0);
        } else if (fieldType == char.class) {
            field.setChar(obj, '\u0000');
        }
    }
    // Mi-caster string ho type ze omena
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

    // mamadika fichier uploader-na ho lasa bbyte[]
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

    public HashMap<String, Object> getSingletons() {
        return singletons;
    }

    public void setSingletons(HashMap<String, Object> singletons) {
        this.singletons = singletons;
    }
}