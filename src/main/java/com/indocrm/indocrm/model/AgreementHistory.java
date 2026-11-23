package com.indocrm.indocrm.model;
import java.time.Instant;
public class AgreementHistory {
 private String action, actor, actorRole, remarks, status; private Instant at; private int version;
 public String getAction(){return action;} public void setAction(String v){action=v;} public String getActor(){return actor;} public void setActor(String v){actor=v;} public String getActorRole(){return actorRole;} public void setActorRole(String v){actorRole=v;} public String getRemarks(){return remarks;} public void setRemarks(String v){remarks=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public Instant getAt(){return at;} public void setAt(Instant v){at=v;} public int getVersion(){return version;} public void setVersion(int v){version=v;}
}
