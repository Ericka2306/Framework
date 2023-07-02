package modele;

import etu1965.framework.ModelView;
import etu1965.framework.annotation.Scope;
import etu1965.framework.annotation.Url;

/**
 *
 * @author hp
 */
@Scope(value="Singleton")
public class Login {
    String userName;
    String password;
    
    @Url(lien="sign_up")
    public ModelView seConnecter(){
        ModelView mv = new ModelView();
        // Reto manampy session izy
        mv.addAuth("is_connected", true);
        mv.addAuth("profil", this.getUserName());
        mv.setView("index.jsp");
        return mv;
    }
    @Url(lien="log_out")
    public ModelView seDeconnecter(){
        ModelView mv = new ModelView();
        // miteny hoe supprimer-na dooly ny session ao
        mv.setInvalidateSession(true);
        mv.setView("index.jsp");
        return mv;
    }
    @Url(lien="delete_profil")
    public ModelView deleteProfil(String profil){
        ModelView mv = new ModelView();
        // Atao anaty RemoveSession ze session tina supprimer-na
        mv.addRemoveSession("profil");
        mv.setView("index.jsp");
        return mv;
    }
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
    
}
