/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package etu1965.framework.servlet;



import etu1965.framework.Mapping;
import etu1965.framework.annotation.Url;
import jakarta.servlet.ServletConfig;
import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;

/**
 *
 * @author ITU
 */
public class FrontServlet extends HttpServlet { 
    HashMap<String,Mapping> mappingUrls;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        HashMap<String,Mapping> mappingUrls = FrontServlet.getAllMapping();
        this.setMappingUrls(mappingUrls);
    }
    
    
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        PrintWriter out = response.getWriter();
        out.println("Servlet : Front Servlet");
        out.println("");
        out.println("Context Path :"+request.getContextPath());
        out.println("");
        out.println("URL :"+request.getRequestURL());
        out.println("");
        out.println("Parametre :");
        Enumeration<String> liste = request.getParameterNames();
        while(liste.hasMoreElements()){
            String element = liste.nextElement();
            String[] elementValues = request.getParameterValues(element);
            for(int i=0 ; i<elementValues.length ; i++){
                out.println(element+" "+(i+1)+" : "+elementValues[i]);
            }
        }
        out.println("");

        out.println("Mapping Urls :");
        HashMap<String,Mapping> mappingUrls = this.getMappingUrls();
        Set keys = mappingUrls.keySet();
        Iterator itr = keys.iterator();
        while(itr.hasNext()){
            String key = (String) itr.next();
            out.print("Key : "+key+" , ");
            out.println("Value : Class: "+mappingUrls.get(key).getClassName()+", Method: "+mappingUrls.get(key).getMethod());
        }
        
        out.println("");
        itr = keys.iterator();
        while(itr.hasNext()){
            String key = (String) itr.next();
            if(key.equals(request.getHttpServletMapping().getMatchValue())){
                try {
                    Class<?> classMapping = Class.forName("etu2043.framework.modele."+mappingUrls.get(key).getClassName());
                    Object objet = classMapping.newInstance();
                    Method method = objet.getClass().getMethod(mappingUrls.get(key).getMethod());
                    out.println("Action : "+String.valueOf(method.invoke(objet)));
                } catch (Exception ex) {
                    out.println(ex.getMessage());
                }
            }
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold> 
    
    public HashMap<String, Mapping> getMappingUrls() {
        return mappingUrls;
    }

    public void setMappingUrls(HashMap<String, Mapping> mappingUrls) {
        this.mappingUrls = mappingUrls;
    }
    
    public static HashMap<String,Mapping> getAllMapping(){
        HashMap<String , Mapping> mappingUrls = new HashMap<String,Mapping>();
        Set<Method> method = new Reflections("etu2043.framework.modele",new MethodAnnotationsScanner()).getMethodsAnnotatedWith(Url.class);
        Iterator<Method> itr = method.iterator();
        while(itr.hasNext()){
            Method m = itr.next();
            
            Mapping tempMapping = new Mapping();
            tempMapping.setClassName(m.getDeclaringClass().getSimpleName());
            tempMapping.setMethod(m.getName());
            
            Url url = m.getAnnotation(Url.class);
            String cle = url.lien();
            
            mappingUrls.put(cle, tempMapping);
        }
        return mappingUrls;
    }
}
