/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package etu1965.framework.modele;

import etu1965.framework.annotation.Url;

/**
 *
 * @author ITU
 */
public class Departement {
    int id;
    String nom;
    String addresse;

    @Url(lien="dept_all")
    public String findAll(){
        return "Find All Departement";
    }
    
    @Url(lien="dept_update")
    public String update(){
        return "Update Departement";
    }
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getAddresse() {
        return addresse;
    }

    public void setAddresse(String addresse) {
        this.addresse = addresse;
    }
    
    
}
