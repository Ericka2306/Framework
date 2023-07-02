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
public class Employer {
    int id;
    String nom;

    @Url(lien="emp_all")
    public String findAll(){
        return "Find All Employer";
    }
    
    @Url(lien="add_emp")
    public String addEmp(){
        return "Add Employer";
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
}
