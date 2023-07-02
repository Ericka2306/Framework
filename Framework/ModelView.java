package etu1965.framework;

import java.util.ArrayList;
import java.util.HashMap;

public class ModelView{
	String view; // vue andehanana
	HashMap<String,Object> data; // fanasina donnee 
	HashMap<String,Object> auth; // fanasina session
	boolean isJson; // fandefasans Json ra ModelView no apesaina
	boolean invalidateSession=false; // hi-supprimer-na ny session rehetra
	ArrayList<String> removeSession; // liste-na session tina supprimer-na

	public boolean isJson() {
		return isJson;
	}
	public void setJson(boolean isJson) {
		this.isJson = isJson;
	}
	public ModelView()
	{
		this.setData(new HashMap<String,Object>());
		this.setAuth(new HashMap<String,Object>());
		this.setRemoveSession(new ArrayList<String>());
	}
	public String getView(){
		return this.view;
	}

	public void setView(String view){
		this.view = view;
	}

	public HashMap<String, Object> getData() {
		return data;
	}

	public void setData(HashMap<String, Object> data) {
		this.data = data;
	}

	// Fanapina donnee ao amle data
	public void addItem(String key , Object value){
		this.data.put(key, value);
	}
	public HashMap<String, Object> getAuth() {
		return auth;
	}
	public void setAuth(HashMap<String, Object> auth) {
		this.auth = auth;
	}
	// Fanapina session
	public void addAuth(String key , Object value){
		this.auth.put(key,value);
	}
	public boolean isInvalidateSession() {
		return invalidateSession;
	}
	public void setInvalidateSession(boolean invalidateSession) {
		this.invalidateSession = invalidateSession;
	}
	public ArrayList<String> getRemoveSession() {
		return removeSession;
	}
	public void setRemoveSession(ArrayList<String> removeSession) {
		this.removeSession = removeSession;
	}
	// fanapina session tina ho supprimer-na
	public void addRemoveSession(String session){
		this.removeSession.add(session);
	}

}