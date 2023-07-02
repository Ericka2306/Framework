Java Version : 8 ou +
Tomcat Version : 10.0.4

Pré-requis:
    . Mettre toutes les classes dans le meme package
    . Les fonctions doivent retourner un objet ModelView et doivent etre annoter par @Url(lien="...") pour pouvoir etre accessible
    . Avec l'objet ModelView:
        - Utiliser la fonction setView("exemple.jsp") pour se rediriger vers la page exemple.jsp
        - Utiliser la fonction addItem("CLE", OBJECT) pour renvoyer l'objet 'OBJECT' , La recuperation de l'objet 'OBJECT' se fait par la fonction request.getAttribute("CLE") dans votre page jsp
    . Ajouter ce bout de code dans votre web.xml (modifier <param-value> avec le package de vos classes) :
        <servlet>   
            <servlet-name>FrontServlet</servlet-name>
            <servlet-class>etu1965.framework.servlet.FrontServlet</servlet-class>
            <init-param>
            <param-name>package</param-name>
            <param-value> ... </param-value>
            <description>package_modele</description>
            </init-param>
        </servlet>
        <servlet-mapping>
            <servlet-name>FrontServlet</servlet-name>
            <url-pattern>/</url-pattern>
        </servlet-mapping>
    . Compiler vos classes avec l'option -parameters

Remarque :            
    . Si il y a des insertion à travers des formulaires le nom des champs dans le formulaire devrait correspondre aux noms des attributs de votre classe
        Exemple :
            Employer.java
                class Employer{
                    int id;
                    String nom;

                    ...
                }
            AjoutEmployer.jsp 
                ...
                Id : <input ... name="id">
		        Nom : <input ... name="nom">
                ...



    . Si votre fonction a besoin d'arguments , le nom des arguments devrait etre le meme que le nom des variables de l'url 
        Exemple :
            Employer.java
                class Employer{
                    ...
                    public ModelView save(int id , String nom){
                        ....
                    }
                }
            Page.jsp
                <a href="....?id=...&nom=...">