import edu.stanford.nlp.ling.TaggedWord;

import java.util.ArrayList;

public class Sresp {
    private boolean actor;
    private boolean modalVerb;
    private boolean verb;
    private boolean pVoice;
    private boolean ambVerb;
    private boolean notMatched;
    private ArrayList<TaggedWord> sRespTagged = new ArrayList<>();

    public Sresp(boolean actor, boolean modalVerb, boolean verb){
        this.actor = actor;
        this.modalVerb = actor;
        this.verb =  verb;
        this.pVoice =false;
        this.ambVerb=false;
        this.notMatched=false;
    }

    public void setActor(boolean actor) {
        this.actor = actor;
    }

    public boolean getActor() {
        return actor;
    }

    public void setModalVerb(boolean modalVerb) {
        this.modalVerb = modalVerb;
    }

    public boolean getModalVerb() {
        return modalVerb;
    }

    public void setVerb(boolean verb) {
        this.verb = verb;
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

    public void setsRespTagged(ArrayList<TaggedWord> sRespTagged) {
        this.sRespTagged = sRespTagged;
    }

    public ArrayList<TaggedWord> getsRespTagged() {
        return sRespTagged;
    }
}
