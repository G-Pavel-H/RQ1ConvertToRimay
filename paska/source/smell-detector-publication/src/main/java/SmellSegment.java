public class SmellSegment {
    private final String noActorCondMsg = "Actor is missing in the condition.";
    private final String noVerbCondMsg = "Verb is missing in the condition.";
    private final String noActorSRMsg = "Actor is missing in the SR.";
    private final String noModalSRMsg = "Modal verb is missing in the SR.";
    private final String noVerbSRMsg = "Verb is missing in the SR.";
    private final String noNounScopeMsg = "Noun is missing in the Scope.";
    private final String pVoiceCondMsg = "Passive voice in the condition.";
    private final String pVoiceSRMsg = "Passive voice in the system response.";
    private final String ambVerbCondMsg = "Not precise verb in the condition.";
    private final String ambVerbSRMsg = "Not precise verb in the system response.";





    private String reqID;
    private boolean noActorCond;
    private boolean noVerbCond;
    private boolean noActorSR;
    private boolean noModalSR;
    private boolean noVerbSR;
    private boolean noNounScope;
    private boolean pVoiceCond;
    private boolean pVoiceSR;
    private boolean ambVerbCond;
    private boolean ambVerbSR;


    public SmellSegment(String reqId)
    {
        this.reqID = reqId;
        this.noNounScope = false;
        this.noActorCond = false;
        this.noVerbCond = false;
        this.noActorSR = false;
        this.noModalSR = false;
        this.noVerbSR = false;

        this.pVoiceCond = false;
        this.pVoiceSR = false;
        this.ambVerbCond = false;
        this.ambVerbSR = false;

    }
    public String getNoActorCondMsg() {
        return noActorCondMsg;
    }
    public String getNoVerbCondMsg() {
        return noVerbCondMsg;
    }
    public String getNoActorSRMsg(){return noActorSRMsg;}
    public String getNoModalSRMsg() {
        return noModalSRMsg;
    }
    public String getNoVerbSRMsg() {
        return noVerbSRMsg;
    }
    public String getNoNounScopeMsg() {
        return noNounScopeMsg;
    }
    public String getpVoiceCondMsg() {
        return pVoiceCondMsg;
    }
    public String getpVoiceSRMsg() {
        return pVoiceSRMsg;
    }
    public String getAmbVerbCondMsg() {
        return ambVerbCondMsg;
    }
    public String getAmbVerbSRMsg() {
        return ambVerbSRMsg;
    }

    public void setNoActorCond(boolean noActorCond) {
        this.noActorCond = noActorCond;
    }
    public void setNoVerbCond(boolean noVerbCond) {
        this.noVerbCond = noVerbCond;
    }
    public void setNoActorSR(boolean noActorSR) { this.noActorSR = noActorSR;}
    public void setNoModalSR(boolean noModalSR) {
        this.noModalSR = noModalSR;
    }
    public void setNoVerbSR(boolean noVerbSR) {
        this.noVerbSR = noVerbSR;
    }
    public void setNoNounScope(boolean noNounScope) {
        this.noNounScope = noNounScope;
    }

    public void setpVoiceCond(boolean pVoiceCond) {
        this.pVoiceCond = pVoiceCond;
    }

    public void setpVoiceSR(boolean pVoiceSR) {
        this.pVoiceSR = pVoiceSR;
    }
    public void setAmbVerbCond(boolean ambVerbCond) {
        this.ambVerbCond = ambVerbCond;
    }
    public void setAmbVerbSR(boolean ambVerbSR) {
        this.ambVerbSR = ambVerbSR;
    }

    public boolean getNoActorCond(){
        return this.noActorCond;
    }
    public boolean getNoVerbCond(){
        return this.noVerbCond;
    }
    public boolean getNoActorSR() {
        return  this.noActorSR;
    }
    public boolean getNoModalSR(){
        return this.noModalSR;
    }
    public boolean getNoVerbSR(){
        return this.noVerbSR;
    }
    public boolean getNoNounScope(){
        return this.noNounScope;
    }
    public boolean getPVoiceCond(){
        return this.pVoiceCond;
    }
    public boolean getPVoiceSR(){
        return this.pVoiceSR;
    }
    public boolean getAmbVerbCond(){
        return this.ambVerbCond;
    }
    public boolean getAmbVerbSR(){
        return this.ambVerbSR;
            }

}
