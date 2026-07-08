import edu.stanford.nlp.ling.TaggedWord;

import java.util.ArrayList;

public class Condition {
    private boolean subConj;
    private boolean noun;
    private boolean verb;
    private boolean pVoice;
    private boolean ambVerb;
    private boolean notMatched;
    private boolean verbSymbol;
    private String textVerb;
    private ArrayList<TaggedWord> conditionTagged;

    public Condition(boolean sc, boolean n, boolean v){
        this.subConj = sc;
        this.noun = n;
        this.verb = v;
        this.pVoice=false;
        this.ambVerb=false;
        this.verbSymbol=false;
        this.notMatched=false;
        this.conditionTagged= new ArrayList<>();
    }

    public void setSubConj(boolean sc){
        this.subConj = sc;
    }
    public Boolean getSubConj(){
        return this.subConj;
    }

    public void setNoun(boolean n){
        this.noun = n;
    }
    public boolean getNoun(){
        return this.noun;
    }

    public void setVerb(boolean v){
        this.verb = v;
    }

    public boolean getVerb() {
        return verb;
    }

    public void setpVoice(boolean pVoice) {
        this.pVoice = pVoice;
    }

    public boolean getpVoice() {
        return pVoice;
    }

    public void setAmbVerb(boolean ambVerb) {
        this.ambVerb = ambVerb;
    }

    public boolean getAmbVerb() {
        return ambVerb;
    }

    public void setNotMatched(boolean notMatched) {
        this.notMatched = notMatched;
    }

    public boolean getNotMatched() {
        return notMatched;
    }

    public boolean isVerbSymbol() {
        return verbSymbol;
    }

    public void setVerbSymbol(boolean verbSymbol) {
        this.verbSymbol = verbSymbol;
    }

    public boolean getVerbSymbol(){return this.verbSymbol;}
    public void setConditionTagged(ArrayList<TaggedWord> conditionTagged) {
        this.conditionTagged = conditionTagged;
    }

    public ArrayList<TaggedWord> getConditionTagged() {
        return conditionTagged;
    }

    public String getTextVerb() {
        return textVerb;
    }

    public void setTextVerb(String textVerb) {
        this.textVerb = textVerb;
    }
}
